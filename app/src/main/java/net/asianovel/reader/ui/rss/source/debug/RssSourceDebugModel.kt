package net.asianovel.reader.ui.rss.source.debug

import android.app.Application
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.RssSource
import net.asianovel.reader.model.Debug

class RssSourceDebugModel(application: Application) : BaseViewModel(application),
    Debug.Callback {
    var rssSource: RssSource? = null
    private var callback: ((Int, String) -> Unit)? = null
    var listSrc: String? = null
    var contentSrc: String? = null

    fun initData(sourceUrl: String?, finally: () -> Unit) {
        sourceUrl?.let {
            execute {
                rssSource = appDb.rssSourceDao.getByKey(sourceUrl)
            }.onFinally {
                finally()
            }
        }
    }

    fun observe(callback: (Int, String) -> Unit) {
        this.callback = callback
    }

    fun startDebug(source: RssSource) {
        execute {
            Debug.callback = this@RssSourceDebugModel
            Debug.startDebug(this, source)
        }
    }

    override fun printLog(state: Int, msg: String) {
        when (state) {
            10 -> listSrc = msg
            20 -> contentSrc = msg
            else -> callback?.invoke(state, msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Debug.cancelDebug(true)
    }

}
