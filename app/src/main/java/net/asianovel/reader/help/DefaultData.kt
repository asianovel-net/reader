package net.asianovel.reader.help

import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.*
import net.asianovel.reader.help.config.ReadBookConfig
import net.asianovel.reader.help.config.ThemeConfig
import net.asianovel.reader.model.BookCover
import net.asianovel.reader.utils.GSON
import net.asianovel.reader.utils.fromJsonArray
import net.asianovel.reader.utils.fromJsonObject
import splitties.init.appCtx
import java.io.File

object DefaultData {

    val httpTTS: List<HttpTTS> by lazy {
        val json =
            String(
                appCtx.assets.open("defaultData${File.separator}httpTTS.json")
                    .readBytes()
            )
        HttpTTS.fromJsonArray(json).getOrElse {
            emptyList()
        }
    }

    val zhEnTerms: Map<String,String> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}zhEnTerms.json")
                .readBytes()
        )
        GSON.fromJsonObject<Map<String,String>>(json).getOrNull()
            ?: emptyMap<String,String>()
    }

    val readConfigs: List<ReadBookConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ReadBookConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ReadBookConfig.Config>(json).getOrNull()
            ?: emptyList()
    }

    val txtTocRules: List<TxtTocRule> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}txtTocRule.json")
                .readBytes()
        )
        GSON.fromJsonArray<TxtTocRule>(json).getOrNull() ?: emptyList()
    }

    val themeConfigs: List<ThemeConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ThemeConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ThemeConfig.Config>(json).getOrNull() ?: emptyList()
    }

    val rssSources: List<RssSource> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}rssSources.json")
                .readBytes()
        )
        RssSource.fromJsonArray(json).getOrDefault(emptyList())
    }

    val bookSources: List<BookSource> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}bookSources.json")
                .readBytes()
        )
        BookSource.fromJsonArray(json).getOrDefault(emptyList())
    }

    val coverRule: BookCover.CoverRule by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}coverRule.json")
                .readBytes()
        )
        GSON.fromJsonObject<BookCover.CoverRule>(json).getOrThrow()!!
    }

    val keyboardAssists: List<KeyboardAssist> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}keyboardAssists.json")
                .readBytes()
        )
        GSON.fromJsonArray<KeyboardAssist>(json).getOrNull()!!
    }

    fun importDefaultHttpTTS() {
        appDb.httpTTSDao.deleteDefault()
        appDb.httpTTSDao.insert(*httpTTS.toTypedArray())
    }

    fun importDefaultTocRules() {
        appDb.txtTocRuleDao.deleteDefault()
        appDb.txtTocRuleDao.insert(*txtTocRules.toTypedArray())
    }

    fun importDefaultRssSources() {
        appDb.rssSourceDao.insert(*rssSources.toTypedArray())
    }

    fun importDefaultBookSources() {
        appDb.bookSourceDao.insert(*bookSources.toTypedArray())
    }
}