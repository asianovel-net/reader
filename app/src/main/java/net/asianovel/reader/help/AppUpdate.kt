package net.asianovel.reader.help

import net.asianovel.reader.help.coroutine.Coroutine
import kotlinx.coroutines.CoroutineScope

object AppUpdate {

    val gitHubUpdate by lazy {
        kotlin.runCatching {
            Class.forName("net.asianovel.reader.help.AppUpdateGitHub")
                .kotlin.objectInstance as AppUpdateInterface
        }.getOrNull()
    }

    data class UpdateInfo(
        val tagName: String,
        val updateLog: String,
        val downloadUrl: String,
        val fileName: String
    )

    interface AppUpdateInterface {

        fun check(scope: CoroutineScope): Coroutine<UpdateInfo>

    }

}