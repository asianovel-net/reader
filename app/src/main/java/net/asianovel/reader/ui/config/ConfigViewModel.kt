package net.asianovel.reader.ui.config

import android.app.Application
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.help.AppWebDav
import net.asianovel.reader.help.book.BookHelp
import net.asianovel.reader.utils.FileUtils
import net.asianovel.reader.utils.toastOnUi

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }


}