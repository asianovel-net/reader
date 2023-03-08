package net.asianovel.reader.ui.book.read.config

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.HttpTTS
import net.asianovel.reader.exception.NoStackTraceException
import net.asianovel.reader.help.DefaultData
import net.asianovel.reader.help.http.newCallResponseBody
import net.asianovel.reader.help.http.okHttpClient
import net.asianovel.reader.help.http.text
import net.asianovel.reader.utils.isJsonArray
import net.asianovel.reader.utils.isJsonObject
import net.asianovel.reader.utils.readText
import net.asianovel.reader.utils.toastOnUi

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importOnLine(url: String) {
        execute {
            okHttpClient.newCallResponseBody {
                url(url)
            }.text("utf-8").let { json ->
                import(json)
            }
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败")
        }
    }

    fun importLocal(uri: Uri) {
        execute {
            import(uri.readText(context))
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败\n${it.localizedMessage}")
        }
    }

    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                HttpTTS.fromJsonArray(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(it)
                }
            }
            else -> {
                throw NoStackTraceException("格式不对")
            }
        }
    }

}