package net.asianovel.reader.help.source

import android.os.Handler
import android.os.Looper
import net.asianovel.reader.constant.AppConst
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.BaseSource
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.data.entities.RssSource
import net.asianovel.reader.utils.EncoderUtils
import net.asianovel.reader.utils.NetworkUtils
import net.asianovel.reader.utils.splitNotBlank
import net.asianovel.reader.utils.toastOnUi
import splitties.init.appCtx

object SourceHelp {

    private val handler = Handler(Looper.getMainLooper())
    private val list18Plus by lazy {
        try {
            return@lazy String(appCtx.assets.open("18PlusList.txt").readBytes())
                .splitNotBlank("\n")
        } catch (e: Exception) {
            return@lazy arrayOf<String>()
        }
    }

    fun getSource(key: String?): BaseSource? {
        key ?: return null
        return appDb.bookSourceDao.getBookSource(key)
            ?: appDb.rssSourceDao.getByKey(key)
    }

    fun insertRssSource(vararg rssSources: RssSource) {
        rssSources.forEach { rssSource ->
            if (is18Plus(rssSource.sourceUrl)) {
                handler.post {
                    appCtx.toastOnUi("${rssSource.sourceName}是18+网址,禁止导入.")
                }
            } else {
                appDb.rssSourceDao.insert(rssSource)
            }
        }
    }

    fun insertBookSource(vararg bookSources: BookSource) {
        bookSources.forEach { bookSource ->
            if (is18Plus(bookSource.bookSourceUrl)) {
                handler.post {
                    appCtx.toastOnUi("${bookSource.bookSourceName}是18+网址,禁止导入.")
                }
            } else {
                appDb.bookSourceDao.insert(bookSource)
            }
        }
    }

    private fun is18Plus(url: String?): Boolean {
        url ?: return false
        val baseUrl = NetworkUtils.getBaseUrl(url)
        baseUrl ?: return false
        if (AppConst.isPlayChannel) return false
        kotlin.runCatching {
            val host = baseUrl.split("//", ".")
            val base64Url = EncoderUtils.base64Encode("${host[host.lastIndex - 1]}.${host.last()}")
            list18Plus.forEach {
                if (base64Url == it) {
                    return true
                }
            }
        }
        return false
    }

}