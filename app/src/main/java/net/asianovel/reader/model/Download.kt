package net.asianovel.reader.model

import android.content.Context
import net.asianovel.reader.constant.IntentAction
import net.asianovel.reader.service.DownloadService
import net.asianovel.reader.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}