package net.asianovel.reader.ui.book.explore

import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import net.asianovel.reader.R
import net.asianovel.reader.base.AppContextWrapper
import net.asianovel.reader.base.VMBaseActivity
import net.asianovel.reader.data.entities.Book
import net.asianovel.reader.data.entities.SearchBook
import net.asianovel.reader.databinding.ActivityExploreShowBinding
import net.asianovel.reader.databinding.ViewLoadMoreBinding
import net.asianovel.reader.help.DefaultData
import net.asianovel.reader.help.book.BookHelp
import net.asianovel.reader.help.config.AppConfig
import net.asianovel.reader.model.translation.Translation
import net.asianovel.reader.ui.book.info.BookInfoActivity
import net.asianovel.reader.ui.widget.recycler.LoadMoreView
import net.asianovel.reader.ui.widget.recycler.VerticalDivider
import net.asianovel.reader.utils.startActivity
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx

class ExploreShowActivity : VMBaseActivity<ActivityExploreShowBinding, ExploreShowViewModel>(),
    ExploreShowAdapter.CallBack {
    override val binding by viewBinding(ActivityExploreShowBinding::inflate)
    override val viewModel by viewModels<ExploreShowViewModel>()

    private val adapter by lazy { ExploreShowAdapter(this, this) }
    private val loadMoreView by lazy { LoadMoreView(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.title = intent.getStringExtra("exploreName")
        val toLanguage   = Translation.getToLanguage()
        val fromLanguage = BookHelp.detectLang(binding.titleBar.title.toString())
        if (AppConfig.translateMode != "off" && !toLanguage.equals(fromLanguage,true)) {
            binding.titleBar.title = DefaultData.zhEnTerms.get(binding.titleBar.title) ?: binding.titleBar.title
        }
        initRecyclerView()
        viewModel.booksData.observe(this) { upData(it) }
        viewModel.initData(intent)
        viewModel.errorLiveData.observe(this) {
            loadMoreView.error(it)
        }
        viewModel.upAdapterLiveData.observe(this) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, it)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        adapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.startLoad()
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                loadMoreView.hasMore()
                scrollToBottom()
            }
        }
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun scrollToBottom() {
        adapter.let {
            if (loadMoreView.hasMore && !loadMoreView.isLoading) {
                loadMoreView.startLoad()
                viewModel.explore()
            }
        }
    }

    private fun upData(books: List<SearchBook>) {
        Translation.getTranslateSearchBook(books).let {
            loadMoreView.stopLoad()
            if (it.isEmpty() && adapter.isEmpty()) {
                loadMoreView.noMore(getString(R.string.empty))
            } else if (it.isEmpty()) {
                loadMoreView.noMore()
            } else if (adapter.getItems().contains(it.first()) && adapter.getItems()
                    .contains(it.last())
            ) {
                loadMoreView.noMore()
            } else {
                adapter.addItems(it)
            }
        }

    }

    override fun isInBookshelf(name: String, author: String): Boolean {
        return if (author.isNotBlank()) {
            viewModel.bookshelf.contains("$name-$author")
        } else {
            viewModel.bookshelf.any { it.startsWith("$name-") }
        }
    }

    override fun showBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
            putExtra("bookUrl", book.bookUrl)
        }
    }
}
