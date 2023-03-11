package net.asianovel.reader.model.analyzeRule

import android.R
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode


class ContentExtractor(var doc: Document? = null) {

    var infoMap = HashMap<Element, CountInfo>()


    class CountInfo {
        var textCount = 0
        var linkTextCount = 0
        var tagCount = 0
        var linkTagCount = 0
        var density = 0.0
        var densitySum = 0.0
        var score = 0.0
        var pCount = 0
        var leafList = ArrayList<Int>()
    }

    fun clean() {
        doc!!.select("script,noscript,style,iframe,br").remove()
    }

    fun computeInfo(node: Node): CountInfo {
        return if (node is Element) {
            val tag = node as Element
            val countInfo = CountInfo()
            for (childNode in tag.childNodes()) {
                val childCountInfo = computeInfo(childNode)
                countInfo.textCount += childCountInfo.textCount
                countInfo.linkTextCount += childCountInfo.linkTextCount
                countInfo.tagCount += childCountInfo.tagCount
                countInfo.linkTagCount += childCountInfo.linkTagCount
                countInfo.leafList.addAll(childCountInfo.leafList)
                countInfo.densitySum += childCountInfo.density
                countInfo.pCount += childCountInfo.pCount
            }
            countInfo.tagCount++
            val tagName = tag.tagName()
            if (tagName == "a") {
                countInfo.linkTextCount = countInfo.textCount
                countInfo.linkTagCount++
            } else if (tagName == "p") {
                countInfo.pCount++
            }
            val pureLen = countInfo.textCount - countInfo.linkTextCount
            val len = countInfo.tagCount - countInfo.linkTagCount
            if (pureLen == 0 || len == 0) {
                countInfo.density = 0.0
            } else {
                countInfo.density = (pureLen + 0.0) / len
            }
            infoMap[tag] = countInfo
            countInfo
        } else if (node is TextNode) {
            val tn = node as TextNode
            val countInfo = CountInfo()
            val text = tn.text()
            val len = text.length
            countInfo.textCount = len
            countInfo.leafList.add(len)
            countInfo
        } else {
            CountInfo()
        }
    }

    fun computeScore(tag: Element?): Double {
        val countInfo = infoMap[tag]
        val `var` = Math.sqrt(computeVar(countInfo!!.leafList) + 1)
        return Math.log(`var`) * countInfo!!.densitySum * Math.log((countInfo.textCount - countInfo.linkTextCount + 1).toDouble()) * Math.log10(
            (countInfo.pCount + 2).toDouble()
        )
    }

    fun computeVar(data: ArrayList<Int>): Double {
        if (data.size === 0) {
            return 0.0
        }
        if (data.size === 1) {
            return (data[0] / 2).toDouble()
        }
        var sum = 0.0
        for (i in data) {
            sum += i.toDouble()
        }
        val ave: Double = sum / data.size
        sum = 0.0
        for (i in data) {
            sum += (i - ave) * (i - ave)
        }
        sum = sum / data.size
        return sum
    }

    @Throws(Exception::class)
    fun getContentElement(): Element? {
        clean()
        computeInfo(doc!!.body())
        var maxScore = 0.0
        var content: Element? = null
        for (entry in infoMap.entries) {
            val tag: Element = entry.key
            if (tag.tagName().equals("a") || tag == doc!!.body()) {
                continue;
            }
            val score = computeScore(tag)
            if (score > maxScore) {
                maxScore = score;
                content = tag;
            }
        }
        if (content == null) {
            throw Exception("extraction failed")
        }
        return content
    }
}