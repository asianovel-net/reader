package net.asianovel.reader.ui.book.explore

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.constant.AppLog
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.data.entities.SearchBook
import net.asianovel.reader.model.webBook.WebBook
import net.asianovel.reader.utils.printOnDebug
import net.asianovel.reader.utils.stackTraceStr
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest


@OptIn(ExperimentalCoroutinesApi::class)
class ExploreShowViewModel(application: Application) : BaseViewModel(application) {
    val bookshelf = hashSetOf<String>()
    val upAdapterLiveData = MutableLiveData<String>()
    val booksData = MutableLiveData<List<SearchBook>>()
    val errorLiveData = MutableLiveData<String>()
    private var bookSource: BookSource? = null
    private var exploreUrl: String? = null
    private var page = 1

    init {
        execute {
            appDb.bookDao.flowAll().mapLatest { books ->
                books.map { "${it.name}-${it.author}" }
            }.collect {
                bookshelf.clear()
                bookshelf.addAll(it)
                upAdapterLiveData.postValue("isInBookshelf")
            }
        }.onError {
            AppLog.put("加载书架数据失败", it)
        }
    }

    fun initData(intent: Intent) {
        execute {
            val sourceUrl = intent.getStringExtra("sourceUrl")
            exploreUrl = intent.getStringExtra("exploreUrl")
            if (bookSource == null && sourceUrl != null) {
                bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
            }
            explore()
        }
    }

    fun explore() {
        val source = bookSource
        val url = exploreUrl
        if (source != null && url != null) {
            WebBook.exploreBook(viewModelScope, source, url, page)
                .timeout(100000L)
                .onSuccess(IO) { searchBooks ->
                    booksData.postValue(searchBooks)
                    appDb.searchBookDao.insert(*searchBooks.toTypedArray())
                    page++
                }.onError {
                    it.printOnDebug()
                    errorLiveData.postValue(it.stackTraceStr)
                }
        }
    }

}