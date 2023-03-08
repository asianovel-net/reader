package net.asianovel.reader.ui.book.audio

import android.app.Application
import android.content.Intent
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.constant.EventBus
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.Book
import net.asianovel.reader.data.entities.BookChapter
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.model.AudioPlay
import net.asianovel.reader.model.webBook.WebBook
import net.asianovel.reader.utils.postEvent
import net.asianovel.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {

    fun initData(intent: Intent) = AudioPlay.apply {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl")
            if (bookUrl != null && bookUrl != book?.bookUrl) {
                stop(context)
                inBookshelf = intent.getBooleanExtra("inBookshelf", true)
                book = appDb.bookDao.getBook(bookUrl)
                book?.let { book ->
                    titleData.postValue(book.name)
                    coverData.postValue(book.getDisplayCover())
                    durChapter = appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)
                    upDurChapter(book)
                    bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                    if (durChapter == null) {
                        if (book.tocUrl.isEmpty()) {
                            loadBookInfo(book)
                        } else {
                            loadChapterList(book)
                        }
                    }
                }
            }
            saveRead()
        }
    }

    private fun loadBookInfo(book: Book) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getBookInfo(this, it, book)
                    .onSuccess {
                        loadChapterList(book)
                    }
            }
        }
    }

    private fun loadChapterList(book: Book) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getChapterList(this, it, book)
                    .onSuccess(Dispatchers.IO) { cList ->
                        appDb.bookChapterDao.insert(*cList.toTypedArray())
                        AudioPlay.upDurChapter(book)
                    }.onError {
                        context.toastOnUi(R.string.error_load_toc)
                    }
            }
        }
    }

    fun upSource() {
        execute {
            AudioPlay.book?.let { book ->
                AudioPlay.bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            }
        }
    }

    fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        execute {
            AudioPlay.book?.migrateTo(book, toc)
            appDb.bookDao.insert(book)
            AudioPlay.book = book
            AudioPlay.bookSource = source
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            AudioPlay.upDurChapter(book)
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                appDb.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}