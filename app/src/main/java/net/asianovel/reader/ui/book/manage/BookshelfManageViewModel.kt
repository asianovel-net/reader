package net.asianovel.reader.ui.book.manage

import android.app.Application
import androidx.lifecycle.MutableLiveData
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.constant.BookType
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.Book
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.help.book.isLocal
import net.asianovel.reader.help.book.removeType
import net.asianovel.reader.help.coroutine.Coroutine
import net.asianovel.reader.model.webBook.WebBook
import net.asianovel.reader.utils.toastOnUi


class BookshelfManageViewModel(application: Application) : BaseViewModel(application) {
    var groupId: Long = -1L
    var groupName: String? = null
    val batchChangeSourceState = MutableLiveData<Boolean>()
    val batchChangeSourceProcessLiveData = MutableLiveData<String>()
    var batchChangeSourceCoroutine: Coroutine<Unit>? = null

    fun upCanUpdate(books: List<Book>, canUpdate: Boolean) {
        execute {
            val array = Array(books.size) {
                books[it].copy(canUpdate = canUpdate)
            }
            appDb.bookDao.update(*array)
        }
    }

    fun updateBook(vararg book: Book) {
        execute {
            appDb.bookDao.update(*book)
        }
    }

    fun deleteBook(vararg book: Book) {
        execute {
            appDb.bookDao.delete(*book)
        }
    }

    fun changeSource(books: List<Book>, source: BookSource) {
        batchChangeSourceCoroutine?.cancel()
        batchChangeSourceCoroutine = execute {
            books.forEachIndexed { index, book ->
                batchChangeSourceProcessLiveData.postValue("${index + 1}/${books.size}")
                if (book.isLocal) return@forEachIndexed
                if (book.origin == source.bookSourceUrl) return@forEachIndexed
                WebBook.preciseSearchAwait(this, source, book.name, book.author)
                    .onFailure {
                        context.toastOnUi("获取书籍出错\n${it.localizedMessage}")
                    }.getOrNull()?.let { newBook ->
                        WebBook.getChapterListAwait(source, newBook)
                            .onFailure {
                                context.toastOnUi("获取目录出错\n${it.localizedMessage}")
                            }.getOrNull()?.let { toc ->
                                book.migrateTo(newBook, toc)
                                book.removeType(BookType.updateError)
                                appDb.bookDao.insert(newBook)
                                appDb.bookChapterDao.insert(*toc.toTypedArray())
                            }
                    }
            }
        }.onStart {
            batchChangeSourceState.postValue(true)
        }.onFinally {
            batchChangeSourceState.postValue(false)
        }
    }

}