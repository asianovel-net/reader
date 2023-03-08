package net.asianovel.reader.ui.rss.article

import android.content.Context
import androidx.viewbinding.ViewBinding
import net.asianovel.reader.base.adapter.RecyclerAdapter
import net.asianovel.reader.data.entities.RssArticle


abstract class BaseRssArticlesAdapter<VB : ViewBinding>(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssArticle, VB>(context) {

    interface CallBack {
        val isGridLayout: Boolean
        fun readRss(rssArticle: RssArticle)
    }
}