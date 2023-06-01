package net.asianovel.reader.ui.book.info

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import net.asianovel.reader.R
import net.asianovel.reader.base.VMBaseActivity
import net.asianovel.reader.constant.EventBus
import net.asianovel.reader.constant.Theme
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.Book
import net.asianovel.reader.data.entities.BookChapter
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.databinding.ActivityBookInfoBinding
import net.asianovel.reader.databinding.DialogEditTextBinding
import net.asianovel.reader.help.book.isAudio
import net.asianovel.reader.help.book.isLocal
import net.asianovel.reader.help.book.isLocalTxt
import net.asianovel.reader.help.config.AppConfig
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.theme.backgroundColor
import net.asianovel.reader.lib.theme.bottomBackground
import net.asianovel.reader.lib.theme.getPrimaryTextColor
import net.asianovel.reader.model.BookCover
import net.asianovel.reader.model.remote.RemoteBookWebDav
import net.asianovel.reader.ui.about.AppLogDialog
import net.asianovel.reader.ui.association.ImportOnLineBookFileDialog
import net.asianovel.reader.ui.book.audio.AudioPlayActivity
import net.asianovel.reader.ui.book.changecover.ChangeCoverDialog
import net.asianovel.reader.ui.book.changesource.ChangeBookSourceDialog
import net.asianovel.reader.ui.book.group.GroupSelectDialog
import net.asianovel.reader.ui.book.info.edit.BookInfoEditActivity
import net.asianovel.reader.ui.book.read.ReadBookActivity
import net.asianovel.reader.ui.book.search.SearchActivity
import net.asianovel.reader.ui.book.source.edit.BookSourceEditActivity
import net.asianovel.reader.ui.book.toc.TocActivityResult
import net.asianovel.reader.ui.login.SourceLoginActivity
import net.asianovel.reader.ui.widget.dialog.PhotoDialog
import net.asianovel.reader.ui.widget.dialog.WaitDialog
import net.asianovel.reader.utils.*
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.asianovel.reader.model.translation.Translation


class BookInfoActivity :
    VMBaseActivity<ActivityBookInfoBinding, BookInfoViewModel>(toolBarTheme = Theme.Dark),
    GroupSelectDialog.CallBack,
    ChangeBookSourceDialog.CallBack,
    ChangeCoverDialog.CallBack {

    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            viewModel.bookData.value?.let { book ->
                launch {
                    withContext(IO) {
                        book.durChapterIndex = it.first
                        book.durChapterPos = it.second
                        appDb.bookDao.update(book)
                    }
                    viewModel.chapterListData.value?.let { chapterList ->
                        binding.tvToc.text =
                            getString(R.string.toc_s, chapterList[book.durChapterIndex].title)
                    }
                    startReadActivity(book)
                }
            }
        } ?: let {
            if (!viewModel.inBookshelf) {
                viewModel.delBook()
            }
        }
    }
    private val readBookResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.upBook(intent)
        if (it.resultCode == RESULT_OK) {
            viewModel.inBookshelf = true
            upTvBookshelf()
        }
    }
    private val infoEditResult = registerForActivityResult(
        StartActivityContract(BookInfoEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.upEditBook()
        }
    }
    private var tocChanged = false

    override val binding by viewBinding(ActivityBookInfoBinding::inflate)
    override val viewModel by viewModels<BookInfoViewModel>()

    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setBackgroundResource(R.color.transparent)
        binding.arcView.setBgColor(backgroundColor)
        binding.llInfo.setBackgroundColor(backgroundColor)
        binding.scrollView.setBackgroundColor(backgroundColor)
        binding.flAction.setBackgroundColor(bottomBackground)
        binding.tvShelf.setTextColor(getPrimaryTextColor(ColorUtils.isColorLight(bottomBackground)))
        binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
        viewModel.bookData.observe(this) { showBook(it) }
        viewModel.chapterListData.observe(this) { upLoading(false, it) }
        viewModel.initData(intent)
        initViewEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_can_update)?.isChecked =
            viewModel.bookData.value?.canUpdate ?: true
        menu.findItem(R.id.menu_split_long_chapter)?.isChecked =
            viewModel.bookData.value?.getSplitLongChapter() ?: true
        menu.findItem(R.id.menu_login)?.isVisible =
            !viewModel.bookSource?.loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_set_source_variable)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_set_book_variable)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_can_update)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_split_long_chapter)?.isVisible =
            viewModel.bookData.value?.isLocalTxt ?: false
        menu.findItem(R.id.menu_upload)?.isVisible =
            viewModel.bookData.value?.isLocal ?: false
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit -> {
                if (viewModel.inBookshelf) {
                    viewModel.bookData.value?.let {
                        infoEditResult.launch {
                            putExtra("bookUrl", it.bookUrl)
                        }
                    }
                } else {
                    toastOnUi(R.string.after_add_bookshelf)
                }
            }
            R.id.menu_share_it -> {
                viewModel.bookData.value?.let {
                    val bookJson = GSON.toJson(it)
                    val shareStr = "${it.bookUrl}#$bookJson"
                    shareWithQr(shareStr, it.name)
                }
            }
            R.id.menu_refresh -> {
                upLoading(true)
                viewModel.bookData.value?.let {
                    viewModel.refreshBook(it)
                }
            }
            R.id.menu_login -> viewModel.bookSource?.let {
                startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", it.bookSourceUrl)
                }
            }
            R.id.menu_top -> viewModel.topBook()
            R.id.menu_set_source_variable -> setSourceVariable()
            R.id.menu_set_book_variable -> setBookVariable()
            R.id.menu_copy_book_url -> viewModel.bookData.value?.bookUrl?.let {
                sendToClip(it)
            } ?: toastOnUi(R.string.no_book)
            R.id.menu_copy_toc_url -> viewModel.bookData.value?.tocUrl?.let {
                sendToClip(it)
            } ?: toastOnUi(R.string.no_book)
            R.id.menu_can_update -> {
                if (viewModel.inBookshelf) {
                    viewModel.bookData.value?.let {
                        it.canUpdate = !it.canUpdate
                        viewModel.saveBook(it)
                    }
                } else {
                    toastOnUi(R.string.after_add_bookshelf)
                }
            }
            R.id.menu_clear_cache -> viewModel.clearCache()
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
            R.id.menu_split_long_chapter -> {
                upLoading(true)
                tocChanged = true
                viewModel.bookData.value?.let {
                    it.setSplitLongChapter(!item.isChecked)
                    viewModel.loadBookInfo(it, false)
                }
                item.isChecked = !item.isChecked
                if (!item.isChecked) longToastOnUi(R.string.need_more_time_load_content)
            }

            R.id.menu_upload -> {
                launch {
                    viewModel.bookData.value?.let {
                        val waitDialog = WaitDialog(this@BookInfoActivity)
                        waitDialog.setText("上传中.....")
                        waitDialog.show()
                        try {
                            RemoteBookWebDav.upload(it)
                            //更新书籍最后更新时间,使之比远程书籍的时间新
                            it.lastCheckTime = System.currentTimeMillis()
                            viewModel.saveBook(it)
                        } catch (e: Exception) {
                            toastOnUi(e.localizedMessage)
                        } finally {
                            waitDialog.dismiss()
                        }
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun showBook(book: Book) = binding.run {
        Translation.getTranslateBook(book).let {translateBook ->
            showCover(book)
            tvName.text = translateBook.name
            tvAuthor.text = getString(R.string.author_show, book.getRealAuthor())
            tvOrigin.text = getString(R.string.origin_show, book.originName)
            tvLasted.text = getString(R.string.lasted_show, translateBook.latestChapterTitle)
            tvIntro.text = translateBook.getDisplayIntro()
            upTvBookshelf()
            val kinds = translateBook.getKindList()
            if (kinds.isEmpty()) {
                lbKind.gone()
            } else {
                lbKind.visible()
                lbKind.setLabels(kinds)
            }
            upGroup(book.group)
        }

    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book.getDisplayCover(), book.name, book.author, false, book.origin)
        if(!AppConfig.isEInkMode) {
            BookCover.loadBlur(this, book.getDisplayCover())
                .into(binding.bgBook)
        }
    }

    private fun upLoading(isLoading: Boolean, chapterList: List<BookChapter>? = null) {
        when {
            isLoading -> {
                binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
            }
            chapterList.isNullOrEmpty() -> {
                binding.tvToc.text =
                    if (viewModel.isImportBookOnLine) getString(R.string.click_read_button_load) else getString(
                        R.string.toc_s,
                        getString(R.string.error_load_toc)
                    )
            }
            else -> {
                viewModel.bookData.value?.let {
                    if (it.durChapterIndex < chapterList.size) {
                        binding.tvToc.text =
                            getString(R.string.toc_s, chapterList[it.durChapterIndex].getTranslateTitle())
                    } else {
                        binding.tvToc.text = getString(R.string.toc_s, chapterList.last().getTranslateTitle())
                    }
                    binding.tvLasted.text =
                        getString(R.string.lasted_show, chapterList.last().getTranslateTitle())
                }
            }
        }
    }

    private fun upTvBookshelf() {
        if (viewModel.inBookshelf) {
            binding.tvShelf.text = getString(R.string.remove_from_bookshelf)
        } else {
            binding.tvShelf.text = getString(R.string.add_to_bookshelf)
        }
    }

    private fun upGroup(groupId: Long) {
        viewModel.loadGroup(groupId) {
            if (it.isNullOrEmpty()) {
                binding.tvGroup.text = getString(R.string.group_s, getString(R.string.no_group))
            } else {
                binding.tvGroup.text = getString(R.string.group_s, it)
            }
        }
    }

    private fun initViewEvent() = binding.run {
        ivCover.setOnClickListener {
            viewModel.bookData.value?.let {
                showDialogFragment(
                    ChangeCoverDialog(it.name, it.author)
                )
            } ?: toastOnUi("Book is null")
        }
        ivCover.setOnLongClickListener {
            viewModel.bookData.value?.getDisplayCover()?.let { path ->
                showDialogFragment(PhotoDialog(path))
            }
            true
        }
        tvRead.setOnClickListener {
            viewModel.bookData.value?.let { book ->
                if (viewModel.isImportBookOnLine) {
                    showDialogFragment<ImportOnLineBookFileDialog> {
                        putString("bookUrl", book.bookUrl)
                    }
                } else {
                    readBook(book)
                }
            } ?: toastOnUi("Book is null")
        }
        tvShelf.setOnClickListener {
            if (viewModel.inBookshelf) {
                deleteBook()
            } else {
                viewModel.addToBookshelf {
                    upTvBookshelf()
                }
            }
        }
        tvOrigin.setOnClickListener {
            viewModel.bookData.value?.let { book ->
                if (book.isLocal) return@let
                startActivity<BookSourceEditActivity> {
                    putExtra("sourceUrl", book.origin)
                }
            } ?: toastOnUi("Book is null")
        }
        tvChangeSource.setOnClickListener {
            viewModel.bookData.value?.let { book ->
                showDialogFragment(ChangeBookSourceDialog(book.name, book.author))
            } ?: toastOnUi("Book is null")
        }
        tvTocView.setOnClickListener {
            if (!viewModel.inBookshelf) {
                viewModel.saveBook(viewModel.bookData.value) {
                    viewModel.saveChapterList {
                        openChapterList()
                    }
                }
            } else {
                openChapterList()
            }
        }
        tvChangeGroup.setOnClickListener {
            viewModel.bookData.value?.let {
                showDialogFragment(
                    GroupSelectDialog(it.group)
                )
            } ?: toastOnUi("Book is null")
        }
        tvAuthor.setOnClickListener {
            startActivity<SearchActivity> {
                putExtra("key", viewModel.bookData.value?.author)
            }
        }
        tvName.setOnClickListener {
            startActivity<SearchActivity> {
                putExtra("key", viewModel.bookData.value?.name)
            }
        }
    }

    private fun setSourceVariable() {
        launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("书源不存在")
                return@launch
            }
            val variable = withContext(IO) { source.getVariable() }
            alert(R.string.set_source_variable) {
                setMessage(source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取"))
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "source variable"
                    editView.setText(variable)
                }
                customView { alertBinding.root }
                okButton {
                    viewModel.bookSource?.setVariable(alertBinding.editView.text?.toString())
                }
                cancelButton()
                neutralButton(R.string.delete) {
                    viewModel.bookSource?.setVariable(null)
                }
            }
        }
    }

    private fun setBookVariable() {
        launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("书源不存在")
                return@launch
            }
            val variable = withContext(IO) { viewModel.bookData.value?.getVariable("custom") }
            alert(R.string.set_source_variable) {
                setMessage(source.getDisplayVariableComment("""书籍变量可在js中通过book.getVariable("custom")获取"""))
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "book variable"
                    editView.setText(variable)
                }
                customView { alertBinding.root }
                okButton {
                    viewModel.bookData.value?.let { book ->
                        book.putVariable("custom", alertBinding.editView.text?.toString())
                        viewModel.saveBook(book)
                    }
                }
                cancelButton()
                neutralButton(R.string.delete) {
                    viewModel.bookData.value?.let { book ->
                        book.putVariable("custom", null)
                        viewModel.saveBook(book)
                    }
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun deleteBook() {
        viewModel.bookData.value?.let {
            if (it.isLocal) {
                alert(
                    titleResource = R.string.sure,
                    messageResource = R.string.sure_del
                ) {
                    val checkBox = CheckBox(this@BookInfoActivity).apply {
                        setText(R.string.delete_book_file)
                    }
                    val view = LinearLayout(this@BookInfoActivity).apply {
                        setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
                        addView(checkBox)
                    }
                    customView { view }
                    positiveButton(R.string.yes) {
                        viewModel.delBook(checkBox.isChecked) {
                            finish()
                        }
                    }
                    negativeButton(R.string.no)
                }
            } else {
                viewModel.delBook {
                    upTvBookshelf()
                }
            }
        }
    }

    private fun openChapterList() {
        if (viewModel.chapterListData.value.isNullOrEmpty()) {
            toastOnUi(R.string.chapter_list_empty)
            return
        }
        viewModel.bookData.value?.let {
            tocActivityResult.launch(it.bookUrl)
        }
    }

    private fun readBook(book: Book) {
        if (!viewModel.inBookshelf) {
            viewModel.saveBook(book) {
                viewModel.saveChapterList {
                    startReadActivity(book)
                }
            }
        } else {
            viewModel.saveBook(book) {
                startReadActivity(book)
            }
        }
    }

    private fun startReadActivity(book: Book) {
        when {
            book.isAudio -> readBookResult.launch(
                Intent(this, AudioPlayActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
            )
            else -> readBookResult.launch(
                Intent(this, ReadBookActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
                    .putExtra("tocChanged", tocChanged)
            )
        }
        tocChanged = false
    }

    override val oldBook: Book?
        get() = viewModel.bookData.value

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        viewModel.changeTo(source, book, toc)
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.bookData.value?.let { book ->
            book.customCoverUrl = coverUrl
            showCover(book)
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            }
        }
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        upGroup(groupId)
        viewModel.bookData.value?.let { book ->
            book.group = groupId
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            } else if (groupId > 0) {
                viewModel.saveBook(book)
                viewModel.inBookshelf = true
                upTvBookshelf()
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.FILE_SOURCE_DOWNLOAD_DONE) {
            viewModel.changeToLocalBook(it)
        }
    }
}