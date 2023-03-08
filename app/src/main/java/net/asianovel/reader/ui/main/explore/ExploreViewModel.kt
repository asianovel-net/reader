package net.asianovel.reader.ui.main.explore

import android.app.Application
import net.asianovel.reader.base.BaseViewModel
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.help.config.SourceConfig

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSource) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.insert(bookSource)
        }
    }

    fun deleteSource(source: BookSource) {
        execute {
            appDb.bookSourceDao.delete(source)
            SourceConfig.removeSource(source.bookSourceUrl)
        }
    }

}