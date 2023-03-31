package net.asianovel.reader.ui.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.constant.AppConst
import net.asianovel.reader.data.appDb
import net.asianovel.reader.exception.NoStackTraceException
import net.asianovel.reader.help.CacheManager
import net.asianovel.reader.help.IntentData
import net.asianovel.reader.help.http.newCallResponseBody
import net.asianovel.reader.help.http.okHttpClient
import net.asianovel.reader.model.analyzeRule.AnalyzeUrl
import net.asianovel.reader.utils.*
import java.io.File
import java.util.*

class WebViewModel(application: Application) : BaseViewModel(application) {
    var baseUrl: String = ""
    var html: String? = null
    val headerMap: HashMap<String, String> = hashMapOf()
    var sourceVerificationEnable: Boolean = false
    var sourceOrigin: String = ""

    fun initData(
        intent: Intent,
        success: () -> Unit
    ) {
        execute {
            val url = intent.getStringExtra("url")
                ?: throw NoStackTraceException("url不能为空")
            sourceOrigin = intent.getStringExtra("sourceOrigin") ?: ""
            sourceVerificationEnable = intent.getBooleanExtra("sourceVerificationEnable", false)
            val headerMapF = IntentData.get<Map<String, String>>(url)
            val analyzeUrl = AnalyzeUrl(url, headerMapF = headerMapF)
            baseUrl = analyzeUrl.url
            headerMap.putAll(analyzeUrl.headerMap)
            if (analyzeUrl.isPost()) {
                html = analyzeUrl.getStrResponseAwait(useWebView = false).body
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
    }

    fun saveImage(webPic: String?, path: String) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { biteArray ->
                if (path.isContentScheme()) {
                    val uri = Uri.parse(path)
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)
                            ?.writeBytes(context, biteArray)
                    }
                } else {
                    val file = FileUtils.createFileIfNotExist(File(path), fileName)
                    file.writeBytes(biteArray)
                }
            } ?: throw Throwable("NULL")
        }.onError {
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            @Suppress("BlockingMethodInNonBlockingContext")
            okHttpClient.newCallResponseBody {
                url(data)
            }.bytes()
        } else {
            Base64.decode(data.split(",").toTypedArray()[1], Base64.DEFAULT)
        }
    }

    fun saveVerificationResult(html: String?, success: () -> Unit) {
        execute {
            if (sourceVerificationEnable) {
                val key = "${sourceOrigin}_verificationResult"
                CacheManager.putMemory(key, html ?: "")
            }
        }.onSuccess {
            success.invoke()
        }
    }

}