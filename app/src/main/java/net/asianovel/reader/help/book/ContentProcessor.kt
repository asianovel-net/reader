package net.asianovel.reader.help.book

import com.github.liuyueyi.quick.transfer.ChineseUtils
import net.asianovel.reader.constant.AppLog
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.Book
import net.asianovel.reader.data.entities.BookChapter
import net.asianovel.reader.data.entities.ReplaceRule
import net.asianovel.reader.exception.RegexTimeoutException
import net.asianovel.reader.help.config.AppConfig
import net.asianovel.reader.help.config.ReadBookConfig
import net.asianovel.reader.utils.replace
import net.asianovel.reader.utils.stackTraceStr
import net.asianovel.reader.utils.toastOnUi
import kotlinx.coroutines.CancellationException
import splitties.init.appCtx
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

class ContentProcessor private constructor(
    private val bookName: String,
    private val bookOrigin: String
) {

    companion object {
        private val processors = hashMapOf<String, WeakReference<ContentProcessor>>()

        fun get(bookName: String, bookOrigin: String): ContentProcessor {
            val processorWr = processors[bookName + bookOrigin]
            var processor: ContentProcessor? = processorWr?.get()
            if (processor == null) {
                processor = ContentProcessor(bookName, bookOrigin)
                processors[bookName + bookOrigin] = WeakReference(processor)
            }
            return processor
        }

        fun upReplaceRules() {
            processors.forEach {
                it.value.get()?.upReplaceRules()
            }
        }

    }

    private val titleReplaceRules = CopyOnWriteArrayList<ReplaceRule>()
    private val contentReplaceRules = CopyOnWriteArrayList<ReplaceRule>()

    init {
        upReplaceRules()
    }

    fun upReplaceRules() {
        titleReplaceRules.run {
            clear()
            addAll(appDb.replaceRuleDao.findEnabledByTitleScope(bookName, bookOrigin))
        }
        contentReplaceRules.run {
            clear()
            addAll(appDb.replaceRuleDao.findEnabledByContentScope(bookName, bookOrigin))
        }
    }

    fun getTitleReplaceRules(): List<ReplaceRule> {
        return titleReplaceRules
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getContentReplaceRules(): List<ReplaceRule> {
        return contentReplaceRules
    }

    suspend fun getContent(
        book: Book,
        chapter: BookChapter,
        content: String,
        includeTitle: Boolean = true,
        useReplace: Boolean = true,
        chineseConvert: Boolean = true,
        reSegment: Boolean = true
    ): BookContent {
        var mContent = content
        var sameTitleRemoved = false
        if (content != "null") {
            //去除重复标题
            if (BookHelp.removeSameTitle(book, chapter)) try {
                val name = Pattern.quote(book.name)
                var title = Pattern.quote(chapter.title)
                var matcher = Pattern.compile("^(\\s|\\p{P}|${name})*${title}(\\s)*")
                    .matcher(mContent)
                if (matcher.find()) {
                    mContent = mContent.substring(matcher.end())
                    sameTitleRemoved = true
                } else if (useReplace) {
                    title = Pattern.quote(
                        chapter.getDisplayTitle(
                            contentReplaceRules,
                            chineseConvert = false
                        )
                    )
                    matcher = Pattern.compile("^(\\s|\\p{P}|${name})*${title}(\\s)*")
                        .matcher(mContent)
                    if (matcher.find()) {
                        mContent = mContent.substring(matcher.end())
                        sameTitleRemoved = true
                    }
                }
            } catch (e: Exception) {
                AppLog.put("去除重复标题出错\n${e.localizedMessage}", e)
            }
            if (reSegment && book.getReSegment()) {
                //重新分段
                mContent = ContentHelp.reSegment(mContent, chapter.title)
            }
            if (chineseConvert) {
                //简繁转换
                try {
                    when (AppConfig.chineseConverterType) {
                        1 -> mContent = ChineseUtils.t2s(mContent)
                        2 -> mContent = ChineseUtils.s2t(mContent)
                    }
                } catch (e: Exception) {
                    appCtx.toastOnUi("简繁转换出错")
                }
            }
            if (useReplace && book.getUseReplaceRule()) {
                //替换
                mContent = replaceContent(mContent)
            }
        }
        if (includeTitle) {
            //重新添加标题
            mContent = chapter.getDisplayTitle(
                getTitleReplaceRules(),
                useReplace = useReplace && book.getUseReplaceRule()
            ) + "\n" + mContent
        }
        val contents = arrayListOf<String>()
        mContent.split("\n").forEach { str ->
            val paragraph = str.trim {
                it.code <= 0x20 || it == '　'
            }
            if (paragraph.isNotEmpty()) {
                if (contents.isEmpty() && includeTitle) {
                    contents.add(paragraph)
                } else {
                    contents.add("${ReadBookConfig.paragraphIndent}$paragraph")
                }
            }
        }
        return BookContent(sameTitleRemoved, contents)
    }

    private suspend fun replaceContent(content: String): String {
        var mContent = content
        mContent = mContent.lines().joinToString("\n") { it.trim() }
        getContentReplaceRules().forEach { item ->
            if (item.pattern.isNotEmpty()) {
                try {
                    mContent = if (item.isRegex) {
                        mContent.replace(
                            item.pattern.toRegex(),
                            item.replacement,
                            item.timeoutMillisecond
                        )
                    } else {
                        mContent.replace(item.pattern, item.replacement)
                    }
                } catch (e: RegexTimeoutException) {
                    item.isEnabled = false
                    appDb.replaceRuleDao.update(item)
                    return item.name + e.stackTraceStr
                } catch (e: CancellationException) {
                    return mContent
                } catch (e: Exception) {
                    AppLog.put("替换净化: 规则 ${item.name}替换出错\n替换内容\n${mContent}", e)
                    appCtx.toastOnUi("替换净化: 规则 ${item.name}替换出错")
                }
            }
        }
        return mContent
    }

}
