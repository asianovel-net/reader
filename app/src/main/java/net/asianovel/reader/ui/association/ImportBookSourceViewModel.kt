package net.asianovel.reader.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.constant.AppConst
import net.asianovel.reader.constant.AppPattern
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.exception.NoStackTraceException
import net.asianovel.reader.help.book.ContentProcessor
import net.asianovel.reader.help.config.AppConfig
import net.asianovel.reader.help.http.newCallResponseBody
import net.asianovel.reader.help.http.okHttpClient
import net.asianovel.reader.help.source.SourceHelp
import net.asianovel.reader.utils.*


class ImportBookSourceViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<BookSource>()
    val checkSources = arrayListOf<BookSource?>()
    val selectStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val group = groupName?.trim()
            val keepName = AppConfig.importKeepName
            val selectSource = arrayListOf<BookSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    if (keepName) {
                        checkSources[index]?.let {
                            source.bookSourceName = it.bookSourceName
                            source.bookSourceGroup = it.bookSourceGroup
                            source.customOrder = it.customOrder
                        }
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            source.bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            source.bookSourceGroup = groups.joinToString(",")
                        } else {
                            source.bookSourceGroup = group
                        }
                    }
                    selectSource.add(source)
                }
            }
            SourceHelp.insertBookSource(*selectSource.toTypedArray())
            ContentProcessor.upReplaceRules()
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            val mText = text.trim()
            when {
                mText.isJsonObject() -> {
                    kotlin.runCatching {
                        val json = JsonPath.parse(mText)
                        json.read<List<String>>("$.sourceUrls")
                    }.onSuccess {
                        it.forEach {
                            importSourceUrl(it)
                        }
                    }.onFailure {
                        BookSource.fromJson(mText).getOrThrow().let {
                            allSources.add(it)
                        }
                    }
                }
                mText.isJsonArray() -> BookSource.fromJsonArray(mText).getOrThrow().let { items ->
                    allSources.addAll(items)
                }
                mText.isAbsUrl() -> {
                    importSourceUrl(mText)
                }
                mText.isUri() -> {
                    val uri = Uri.parse(mText)
                    uri.inputStream(context).getOrThrow().let {
                        allSources.addAll(BookSource.fromJsonArray(it).getOrThrow())
                    }
                }
                else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
            }
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.byteStream().let {
            allSources.addAll(BookSource.fromJsonArray(it).getOrThrow())
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.bookSourceDao.getBookSource(it.bookSourceUrl)
                checkSources.add(source)
                selectStatus.add(source == null || source.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}