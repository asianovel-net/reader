package net.asianovel.reader.model.translation

import androidx.collection.LruCache
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import net.asianovel.reader.base.AppContextWrapper
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.Book
import net.asianovel.reader.data.entities.BookChapter
import net.asianovel.reader.data.entities.SearchBook
import net.asianovel.reader.exception.NoStackTraceException
import net.asianovel.reader.help.CacheManager
import net.asianovel.reader.help.book.BookHelp
import net.asianovel.reader.help.config.AppConfig
import net.asianovel.reader.help.http.*
import net.asianovel.reader.model.translation.openai.OpenAIConstants
import net.asianovel.reader.model.translation.openai.OpenAIRequest
import net.asianovel.reader.model.translation.openai.OpenAIResponse
import net.asianovel.reader.utils.*
import splitties.init.appCtx
import java.net.URLEncoder


object Translation {

    private const val prefix = "TRANSLATION"

    private const val separator = "$"

    private const val googleWebRequestLineBreak = "%0A"

    private const val googleWebResponseLineBreak = "\\n"

    private val translateLruCache = object : LruCache<String, String>(1024 * 1024 * 5) {
        override fun sizeOf(key: String, value: String): Int {
            return value.memorySize()
        }
    }

    const val translatePrefix = "$prefix$separator"

    suspend fun  translateSearchBook(fromLanguage: String, searchBookList: ArrayList<SearchBook>) {
        if (enableTranslate(fromLanguage)) {
            val textList = ArrayList<String>()
            val filterSearchList = searchBookList.filter {
                CacheManager.get(getTranslationKey(it.bookUrl,"info")) == null
            }.toList()
            if (filterSearchList.isEmpty()) return
            filterSearchList.forEach {searchBook ->
                textList.add(searchBook.name)
                searchBook.intro?.let {
                    textList.add(it)
                }
                searchBook.kind?.let {
                    textList.add(it)
                }
                searchBook.latestChapterTitle?.let {
                    textList.add(it)
                }
            }
            translate(fromLanguage,textList)
            filterSearchList.forEach {
                val searchBookMap = mapOf(
                    Pair("name", getTranslatedString(it.name)),
                    Pair("intro", getTranslatedString(it.intro)),
                    Pair("kind", getTranslatedString(it.kind)),
                    Pair("latestChapterTitle", getTranslatedString(it.latestChapterTitle))
                )
                CacheManager.put(getTranslationKey(it.bookUrl,"info"),GSON.toJson(searchBookMap))
            }
        }
    }

    suspend fun translateBook(fromLanguage: String,book: Book) {
        if (enableTranslate(fromLanguage)) {
            if(CacheManager.get(getTranslationKey(book.bookUrl,"info"))!=null) {
                return
            }
            val textList = ArrayList<String>()
            textList.add(book.name)
            book.intro?.let {
                textList.add(it)
            }
            book.kind?.let {
                textList.add(it)
            }
            book.latestChapterTitle?.let {
                textList.add(it)
            }
            translate(fromLanguage,textList)
            val bookMap = mapOf(
                Pair("name", getTranslatedString(book.name)),
                Pair("intro", getTranslatedString(book.intro)),
                Pair("kind", getTranslatedString(book.kind)),
                Pair("latestChapterTitle", getTranslatedString(book.latestChapterTitle))
            )
            CacheManager.put(getTranslationKey(book.bookUrl,"info"),GSON.toJson(bookMap))
        }
    }

    suspend fun translateContent(fromLanguage: String,
                         book: Book,
                         bookChapter: BookChapter,
                         content:String): String {
        if (enableTranslate(fromLanguage)) {
            BookHelp.getContent(book,bookChapter)?.let {
                return it
            }
            translate(fromLanguage,content.lines().plus(bookChapter.title))
            val resBuilder = StringBuilder()
            content.lines().forEach {
                resBuilder.append(getTranslatedString(it))
                resBuilder.appendLine()
            }
            val translateTitle = getTranslatedString(bookChapter.title)
            CacheManager.put(getTranslationKey(book.bookUrl,bookChapter.title),translateTitle)
            BookHelp.saveText(book,bookChapter,resBuilder.toString())
            return resBuilder.toString()
        }
        return content
    }



    fun getTranslateSearchBook(searchBookList: List<SearchBook>):List<SearchBook>{
        searchBookList.forEach { seachBook ->
            appDb.bookSourceDao.getBookSource(seachBook.origin)?.let {bookSource ->
                if (enableTranslate(bookSource.bookSourceLang)) {
                    getTranslationKey(seachBook.bookUrl,"info").let {it ->
                        CacheManager.get(it)?.let {it->
                            GSON.fromJsonObject<Map<String, String>>(it).getOrNull()
                        }?.let { it ->
                            it["name"]?.let { it -> seachBook.name = it }
                            it["intro"]?.let { it -> seachBook.intro = it }
                            it["kind"]?.let { it -> seachBook.kind = it }
                            it["latestChapterTitle"]?.let { it -> seachBook.latestChapterTitle = it }
                        }
                    }
                }
            }
        }
        return searchBookList
    }

    fun getTranslateChapterList(bookChapterList: List<BookChapter>): List<BookChapter> {
        if (bookChapterList.isEmpty() ) return bookChapterList
        appDb.bookDao.getBook(bookChapterList.get(0).bookUrl)?.let {book ->
            appDb.bookSourceDao.getBookSource(book.origin)?.let {
                if (enableTranslate(it.bookSourceLang)) {
                    bookChapterList.forEachIndexed { index, bookChapter ->
                        val oldBookChapterTitle = bookChapter.title
                        bookChapter.title = "chapter "+(index+1)
                        CacheManager.get(getTranslationKey(book.bookUrl,oldBookChapterTitle))?.let {
                            bookChapter.title = it
                        }
                    }
                }
            }
        }
        return bookChapterList

    }

    fun getTranslateBook(book: Book):Book{
        appDb.bookSourceDao.getBookSource(book.origin)?.let {bookSource ->
            if (enableTranslate(bookSource.bookSourceLang)) {
                getTranslationKey(book.bookUrl, "info").let { it ->
                    CacheManager.get(it)?.let { it ->
                        GSON.fromJsonObject<Map<String, String>>(it).getOrNull()
                    }?.let { it ->
                        it["name"]?.let { it -> book.name = it }
                        it["intro"]?.let { it -> book.intro = it }
                        it["kind"]?.let { it -> book.kind = it }
                        it["latestChapterTitle"]?.let { it -> book.latestChapterTitle = it }
                    }
                }
            }
        }
        return book
    }

    private suspend fun  translate(from: String="zh",textList: List<String>) {
        val to = getToLanguage()
        var resList = filterTextList(textList)
        var res = batchTranslate(30,from,to,resList)
        if (!res) {
            resList = filterTextList(resList)
            res = batchTranslate(10,from,to,resList)
            if (!res) {
                resList = filterTextList(resList)
                res = batchTranslate(5,from,to,resList)
                if (!res) {
                    throw NoStackTraceException("translate error !!!")
                }
            }
        }
    }

    private suspend fun batchTranslate(batchSize:Int=10,from:String="zh",to:String="en",textList:List<String>):Boolean{
        textList.chunked(batchSize).forEach {

            val resList:List<String>? =   when (AppConfig.translateMode){
                "chatGpt" -> translateByChatGpt(from,to,it)
                else -> translateByGoogleWeb(from,to,it)
            }
            if (resList?.size == it.size) {
                for (i in resList.indices) {
                    val key = getTranslationKey(it[i])
                    translateLruCache.put(key,resList[i])
                }
            } else {
                return false
            }
        }
        return true
    }

    private fun filterTextList(textList:List<String>): List<String> {
        val resList =  ArrayList<String>()
        textList.forEach {
            resList.addAll(it.replace(googleWebResponseLineBreak,"").lines())
        }
        return resList.filter {it.isNotBlank() && translateLruCache.get(getTranslationKey(it)) == null}.toList()
    }

     fun getTranslationKey(bookUrl:String,key:String): String {
        val md5BookUrl =  MD5Utils.md5Encode16("$bookUrl")
        val md5Key =  MD5Utils.md5Encode16("$key")
        return "$translatePrefix$md5BookUrl$separator$md5Key"
    }

    private fun getTranslationKey(key:String): String {
        val md5Key =  MD5Utils.md5Encode16("$key")
        return "$translatePrefix$md5Key"
    }

    private suspend fun  translateByGoogleWeb(from: String="zh",to: String="en",textList: List<String>): List<String>? {
        val encodeTextList = ArrayList<String>()
        for (text in textList) {
            val encodeText = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
            encodeTextList.add(encodeText)
        }
        val text = encodeTextList.joinToString(googleWebRequestLineBreak)

        val sl = googleWebConvert(from)
        val tl = googleWebConvert(to)

        val url = AppConfig.googleWebTranslateUrl+"/?sl=$sl&tl=$tl&text=$text&op=translate"
        val jsStr = "(function() { return document.querySelectorAll('div[aria-live] >div >div')[0].innerText; })();"
        BackstageWebView(
            url = url,
            javaScript = jsStr,
        ).getStrResponse().let {
            return it.body?.lines()
        }
    }


    private suspend fun translateByChatGpt(from: String="zh",to: String="en",textList: List<String>) :List<String> {
        if (AppConfig.chatGptToken.isNullOrBlank()) throw NoStackTraceException("chatgpt token is empty !!!")
        var resList = ArrayList<String>()
        var commandPrompt ="Translate this ${textList.size} lines from $from to $to preserving its number."
        var contentPrompt = StringBuffer()
        textList.forEachIndexed { index, text ->
            contentPrompt.append("$index. $text").append(" ")
        }
        val headerMap = HashMap<String, String>()
        headerMap[OpenAIConstants.AUTHORIZATION] = AppConfig.chatGptToken
        headerMap["Content-Type"] = "application/json"
        val body = okHttpClient.newCallStrResponse {
                addHeaders(headerMap)
                url("https://api.openai.com/v1/chat/completions")
                postJson(GSON.toJson(buildOpenRequest(commandPrompt,contentPrompt.toString())))
        }.body

        GSON.fromJsonObject<OpenAIResponse>(body).getOrNull()?.let { openAIResponse ->
            openAIResponse.choices?.forEach { choice ->
                choice.message?.let {message ->
                    message.content?.let {
                        it.lines().filter { it.isNotBlank() }.forEachIndexed { index, s ->
                            val res = s.replace("$index.","").trim()
                            resList.add(res)
                        }
                    }
                }
            }
        }
        if (resList.isEmpty()) delay(10000)
        return resList
    }

    private fun buildOpenRequest(commandPrompt:String,contentPrompt:String):OpenAIRequest {
        val messageList = ArrayList<OpenAIRequest.Message>()
        val ai = OpenAIRequest()
        ai.model = OpenAIConstants.MODEL

        var rolePromptMessage = ai.Message(OpenAIConstants.SYSTEM_ROLE,AppConfig.chatGptRolePrompt)
        messageList.add(rolePromptMessage)

        var cmdPromptMessage = ai.Message(OpenAIConstants.USER_ROLE,commandPrompt)
        messageList.add(cmdPromptMessage)

        var contentPromptMessage = ai.Message(OpenAIConstants.USER_ROLE,contentPrompt)
        messageList.add(contentPromptMessage)
        ai.messages = messageList
        ai.frequency_penalty = 0
        ai.presence_penalty = 0
        return ai
    }

    private fun getTranslatedString(text: String?): String {
        val resBuilder = StringBuilder()
        text?.lines()?.forEach {
            val key = getTranslationKey(it)
            resBuilder.append(translateLruCache.get(key) ?: it)
            resBuilder.appendLine()
        }
        return resBuilder.toString()
    }

    private fun googleWebConvert(language: String): String {
        if (language == "zh") {
            return "zh-CN"
        }
        return language
    }

    fun enableTranslate(fromLanguage: String): Boolean {
        val toLanguage = getToLanguage()
        return AppConfig.translateMode != "off" && toLanguage != fromLanguage
    }

    fun getToLanguage(): String{
        val toLanguage = AppContextWrapper.getSetLocale(appCtx).language
        if (!toLanguage.equals("zh")) return "en"
        return "zh"
    }
}