package net.asianovel.reader.help

import androidx.annotation.Keep
import net.asianovel.reader.exception.NoStackTraceException
import net.asianovel.reader.model.analyzeRule.AnalyzeRule
import net.asianovel.reader.model.analyzeRule.AnalyzeUrl
import net.asianovel.reader.utils.ACache
import net.asianovel.reader.utils.GSON
import net.asianovel.reader.utils.fromJsonObject
import splitties.init.appCtx
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
object DirectLinkUpload {

    const val ruleFileName = "directLinkUploadRule.json"

    @Throws(NoStackTraceException::class)
    suspend fun upLoad(fileName: String, file: Any, contentType: String): String {
        val rule = getRule()
        rule ?: throw NoStackTraceException("直链上传规则未配置")
        val url = rule.uploadUrl
        if (url.isBlank()) {
            throw NoStackTraceException("上传url未配置")
        }
        val downloadUrlRule = rule.downloadUrlRule
        if (downloadUrlRule.isBlank()) {
            throw NoStackTraceException("下载地址规则未配置")
        }
        val analyzeUrl = AnalyzeUrl(url)
        val res = analyzeUrl.upload(fileName, file, contentType)
        val analyzeRule = AnalyzeRule().setContent(res.body, res.url)
        val downloadUrl = analyzeRule.getString(downloadUrlRule)
        if (downloadUrl.isBlank()) {
            throw NoStackTraceException("上传失败,${res.body}")
        }
        return downloadUrl
    }

    private val defaultRule: Rule? by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}directLinkUpload.json")
                .readBytes()
        )
        GSON.fromJsonObject<Rule>(json).getOrNull()
    }

    fun getRule(): Rule? {
        return getConfig() ?: defaultRule
    }

    fun getConfig(): Rule? {
        val json = ACache.get(cacheDir = false).getAsString(ruleFileName)
        return GSON.fromJsonObject<Rule>(json).getOrNull()
    }

    fun putConfig(uploadUrl: String, downloadUrlRule: String, summary: String?) {
        val rule = Rule(uploadUrl, downloadUrlRule, summary)
        ACache.get(cacheDir = false).put(ruleFileName, GSON.toJson(rule))
    }

    fun delConfig() {
        ACache.get(cacheDir = false).remove(ruleFileName)
    }

    fun getSummary(): String? {
        return getRule()?.summary
    }

    @Keep
    data class Rule(
        var uploadUrl: String,
        var downloadUrlRule: String,
        var summary: String?
    )

}
