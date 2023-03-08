package net.asianovel.reader.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.constant.AppConst
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.TxtTocRule
import net.asianovel.reader.exception.NoStackTraceException
import net.asianovel.reader.help.http.newCallResponseBody
import net.asianovel.reader.help.http.okHttpClient
import net.asianovel.reader.help.http.text
import net.asianovel.reader.utils.*

class ImportTxtTocRuleViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<TxtTocRule>()
    val checkSources = arrayListOf<TxtTocRule?>()
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
            val selectSource = arrayListOf<TxtTocRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.txtTocRuleDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            it.printOnDebug()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                GSON.fromJsonObject<TxtTocRule>(text).getOrThrow()?.let {
                    allSources.add(it)
                }
            }
            text.isJsonArray() -> GSON.fromJsonArray<TxtTocRule>(text).getOrThrow()
                ?.let { items ->
                    allSources.addAll(items)
                }
            text.isAbsUrl() -> {
                importSourceUrl(text)
            }
            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
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
        }.text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.txtTocRuleDao.get(it.id)
                checkSources.add(source)
                selectStatus.add(source == null || it != source)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}