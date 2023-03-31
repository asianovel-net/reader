package net.asianovel.reader.ui.rss.source.edit

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.viewModels
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import net.asianovel.reader.R
import net.asianovel.reader.base.VMBaseActivity
import net.asianovel.reader.data.entities.RssSource
import net.asianovel.reader.databinding.ActivityRssSourceEditBinding
import net.asianovel.reader.help.config.LocalConfig
import net.asianovel.reader.lib.dialogs.SelectItem
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.ui.document.HandleFileContract
import net.asianovel.reader.ui.login.SourceLoginActivity
import net.asianovel.reader.ui.qrcode.QrCodeResult
import net.asianovel.reader.ui.rss.source.debug.RssSourceDebugActivity
import net.asianovel.reader.ui.widget.dialog.TextDialog
import net.asianovel.reader.ui.widget.dialog.UrlOptionDialog
import net.asianovel.reader.ui.widget.keyboard.KeyboardToolPop
import net.asianovel.reader.ui.widget.text.EditEntity
import net.asianovel.reader.utils.*
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding

class RssSourceEditActivity :
    VMBaseActivity<ActivityRssSourceEditBinding, RssSourceEditViewModel>(false),
    KeyboardToolPop.CallBack {

    override val binding by viewBinding(ActivityRssSourceEditBinding::inflate)
    override val viewModel by viewModels<RssSourceEditViewModel>()
    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, this, binding.root, this)
    }
    private val adapter by lazy { RssSourceEditAdapter() }
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()
    private val selectDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                sendText(uri.toString())
            } else {
                sendText(uri.path.toString())
            }
        }
    }
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it?.let {
            viewModel.importSource(it) { source: RssSource ->
                upSourceView(source)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        initView()
        viewModel.initData(intent) {
            upSourceView()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!LocalConfig.ruleHelpVersionIsLast) {
            showHelp("ruleHelp")
        }
    }

    override fun finish() {
        val source = getRssSource()
        if (!source.equal(viewModel.rssSource)) {
            alert(R.string.exit) {
                setMessage(R.string.exit_no_save)
                positiveButton(R.string.yes)
                negativeButton(R.string.no) {
                    super.finish()
                }
            }
        } else {
            super.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        softKeyboardTool.dismiss()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !viewModel.rssSource.loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_auto_complete)?.isChecked = viewModel.autoComplete
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val source = getRssSource()
                if (checkSource(source)) {
                    viewModel.save(source) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
            R.id.menu_debug_source -> {
                val source = getRssSource()
                if (checkSource(source)) {
                    viewModel.save(source) {
                        startActivity<RssSourceDebugActivity> {
                            putExtra("key", source.sourceUrl)
                        }
                    }
                }
            }
            R.id.menu_login -> getRssSource().let {
                if (checkSource(it)) {
                    viewModel.save(it) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "rssSource")
                            putExtra("key", it.sourceUrl)
                        }
                    }
                }
            }
            R.id.menu_clear_cookie -> viewModel.clearCookie(getRssSource().sourceUrl)
            R.id.menu_auto_complete -> viewModel.autoComplete = !viewModel.autoComplete
            R.id.menu_copy_source -> sendToClip(GSON.toJson(getRssSource()))
            R.id.menu_qr_code_camera -> qrCodeResult.launch()
            R.id.menu_paste_source -> viewModel.pasteSource { upSourceView(it) }
            R.id.menu_share_str -> share(GSON.toJson(getRssSource()))
            R.id.menu_share_qr -> shareWithQr(
                GSON.toJson(getRssSource()),
                getString(R.string.share_rss_source),
                ErrorCorrectionLevel.L
            )
            R.id.menu_help -> showHelp("ruleHelp")
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
    }

    private fun upSourceView(rs: RssSource? = viewModel.rssSource) {
        rs?.let {
            binding.cbIsEnable.isChecked = rs.enabled
            binding.cbSingleUrl.isChecked = rs.singleUrl
            binding.cbIsEnableCookie.isChecked = rs.enabledCookieJar == true
            binding.cbEnableJs.isChecked = rs.enableJs
            binding.cbEnableBaseUrl.isChecked = rs.loadWithBaseUrl
        }
        sourceEntities.clear()
        sourceEntities.apply {
            add(EditEntity("sourceName", rs?.sourceName, R.string.source_name))
            add(EditEntity("sourceUrl", rs?.sourceUrl, R.string.source_url))
            add(EditEntity("sourceIcon", rs?.sourceIcon, R.string.source_icon))
            add(EditEntity("sourceGroup", rs?.sourceGroup, R.string.source_group))
            add(EditEntity("sourceComment", rs?.sourceComment, R.string.comment))
            add(EditEntity("loginUrl", rs?.loginUrl, R.string.login_url))
            add(EditEntity("loginUi", rs?.loginUi, R.string.login_ui))
            add(EditEntity("loginCheckJs", rs?.loginCheckJs, R.string.login_check_js))
            add(EditEntity("coverDecodeJs", rs?.coverDecodeJs, R.string.cover_decode_js))
            add(EditEntity("header", rs?.header, R.string.source_http_header))
            add(EditEntity("variableComment", rs?.variableComment, R.string.variable_comment))
            add(EditEntity("concurrentRate", rs?.concurrentRate, R.string.concurrent_rate))
            add(EditEntity("sortUrl", rs?.sortUrl, R.string.sort_url))
            add(EditEntity("ruleArticles", rs?.ruleArticles, R.string.r_articles))
            add(EditEntity("ruleNextPage", rs?.ruleNextPage, R.string.r_next))
            add(EditEntity("ruleTitle", rs?.ruleTitle, R.string.r_title))
            add(EditEntity("rulePubDate", rs?.rulePubDate, R.string.r_date))
            add(EditEntity("ruleDescription", rs?.ruleDescription, R.string.r_description))
            add(EditEntity("ruleImage", rs?.ruleImage, R.string.r_image))
            add(EditEntity("ruleLink", rs?.ruleLink, R.string.r_link))
            add(EditEntity("ruleContent", rs?.ruleContent, R.string.r_content))
            add(EditEntity("style", rs?.style, R.string.r_style))
            add(EditEntity("injectJs", rs?.injectJs, R.string.r_inject_js))
            add(EditEntity("contentWhitelist", rs?.contentWhitelist, R.string.c_whitelist))
            add(EditEntity("contentBlacklist", rs?.contentBlacklist, R.string.c_blacklist))
        }
        adapter.editEntities = sourceEntities
    }

    private fun getRssSource(): RssSource {
        val source = viewModel.rssSource
        source.enabled = binding.cbIsEnable.isChecked
        source.singleUrl = binding.cbSingleUrl.isChecked
        source.enabledCookieJar = binding.cbIsEnableCookie.isChecked
        source.enableJs = binding.cbEnableJs.isChecked
        source.loadWithBaseUrl = binding.cbEnableBaseUrl.isChecked
        sourceEntities.forEach {
            when (it.key) {
                "sourceName" -> source.sourceName = it.value ?: ""
                "sourceUrl" -> source.sourceUrl = it.value ?: ""
                "sourceIcon" -> source.sourceIcon = it.value ?: ""
                "sourceGroup" -> source.sourceGroup = it.value
                "sourceComment" -> source.sourceComment = it.value
                "loginUrl" -> source.loginUrl = it.value
                "loginUi" -> source.loginUi = it.value
                "loginCheckJs" -> source.loginCheckJs = it.value
                "coverDecodeJs" -> source.coverDecodeJs = it.value
                "header" -> source.header = it.value
                "variableComment" -> source.variableComment = it.value
                "concurrentRate" -> source.concurrentRate = it.value
                "sortUrl" -> source.sortUrl = it.value
                "ruleArticles" -> source.ruleArticles = it.value
                "ruleNextPage" -> source.ruleNextPage =
                    viewModel.ruleComplete(it.value, source.ruleArticles, 2)
                "ruleTitle" -> source.ruleTitle =
                    viewModel.ruleComplete(it.value, source.ruleArticles)
                "rulePubDate" -> source.rulePubDate =
                    viewModel.ruleComplete(it.value, source.ruleArticles)
                "ruleDescription" -> source.ruleDescription =
                    viewModel.ruleComplete(it.value, source.ruleArticles)
                "ruleImage" -> source.ruleImage =
                    viewModel.ruleComplete(it.value, source.ruleArticles, 3)
                "ruleLink" -> source.ruleLink =
                    viewModel.ruleComplete(it.value, source.ruleArticles)
                "ruleContent" -> source.ruleContent =
                    viewModel.ruleComplete(it.value, source.ruleArticles)
                "style" -> source.style = it.value
                "injectJs" -> source.injectJs = it.value
                "contentWhitelist" -> source.contentWhitelist = it.value
                "contentBlacklist" -> source.contentBlacklist = it.value
            }
        }
        return source
    }

    private fun checkSource(source: RssSource): Boolean {
        if (source.sourceName.isBlank() || source.sourceName.isBlank()) {
            toastOnUi("名称或url不能为空")
            return false
        }
        return true
    }

    override fun helpActions(): List<SelectItem<String>> {
        return arrayListOf(
            SelectItem("插入URL参数", "urlOption"),
            SelectItem("订阅源教程", "ruleHelp"),
            SelectItem("js教程", "jsHelp"),
            SelectItem("正则教程", "regexHelp"),
            SelectItem("选择文件", "selectFile"),
        )
    }

    override fun onHelpActionSelect(action: String) {
        when (action) {
            "urlOption" -> UrlOptionDialog(this) {
                sendText(it)
            }.show()
            "ruleHelp" -> showHelp("ruleHelp")
            "jsHelp" -> showHelp("jsHelp")
            "regexHelp" -> showHelp("regexHelp")
            "selectFile" -> selectDoc.launch {
                mode = HandleFileContract.FILE
            }
        }
    }

    override fun sendText(text: String) {
        if (text.isBlank()) return
        val view = window.decorView.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            val edit = view.editableText//获取EditText的文字
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                edit.replace(start, end, text)//光标所在位置插入文字
            }
        }
    }

    private fun showHelp(fileName: String) {
        //显示目录help下的帮助文档
        val mdText = String(assets.open("help/${fileName}.md").readBytes())
        showDialogFragment(TextDialog(getString(R.string.help), mdText, TextDialog.Mode.MD))
    }

}