/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.betterlyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

object TTMLParser {
    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val words: List<ParsedWord>,
        val isBackground: Boolean = false,
        val agent: String? = null,
        val providerRomanizedText: String? = null,
        val providerRomanizedWords: List<String>? = null,
        val providerRomanizedLanguage: String? = null,
        val providerTranslationText: String? = null,
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val isBackground: Boolean = false,
    )

    private data class TimingContext(
        val tickRate: Double,
        val frameRate: Double,
    )

    private data class ParsedTransliteration(
        val text: String,
        val words: List<String>,
        val language: String?,
    )

    private val whitespaceRegex = Regex("\\s+")

    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            val timingContext = readTimingContext(doc.documentElement)
            val transliterations = parseTransliterations(doc.documentElement)
            val translations = parseTranslations(doc.documentElement)

            val divElements = doc.getElementsByTagName("*")

            for (divIdx in 0 until divElements.length) {
                val divElement = divElements.item(divIdx) as? Element ?: continue
                if (!divElement.tagName.endsWith("div", ignoreCase = true)) continue

                val pElements = divElement.getElementsByTagName("*")

                for (pIdx in 0 until pElements.length) {
                    val pElement = pElements.item(pIdx) as? Element ?: continue
                    if (!pElement.tagName.endsWith("p", ignoreCase = true)) continue

                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    val dur = pElement.getAttribute("dur")
                    if (begin.isNullOrEmpty()) continue

                    val startTime = parseTime(begin, timingContext)
                    val endTime =
                        when {
                            end.isNotEmpty() -> parseTime(end, timingContext)
                            dur.isNotEmpty() -> startTime + parseTime(dur, timingContext)
                            else -> startTime + 5.0
                        }

                    val agent =
                        pElement.getAttribute("ttm:agent").takeIf { it.isNotEmpty() }
                            ?: pElement.attributes?.let { attrs ->
                                (0 until attrs.length)
                                    .map { attrs.item(it) }
                                    .firstOrNull {
                                        it.nodeName.endsWith(
                                            "agent",
                                            ignoreCase = true
                                        )
                                    }
                                    ?.nodeValue
                                    ?.takeIf { it.isNotEmpty() }
                            }
                    val lineKey = readAttributeBySuffix(pElement, "key")
                    val transliteration = lineKey?.let(transliterations::get)
                    val translation = lineKey?.let(translations::get)

                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()

                    parseSpanElements(pElement, words, lineText, startTime, endTime, false, timingContext)

                    if (lineText.isEmpty()) {
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                        }
                    }

                    if (lineText.isNotEmpty()) {
                        lines.add(
                            ParsedLine(
                                text = lineText.toString().trim(),
                                startTime = startTime,
                                endTime = endTime,
                                words = words,
                                isBackground = false,
                                agent = agent,
                                providerRomanizedText = transliteration?.text,
                                providerRomanizedWords = transliteration?.words,
                                providerRomanizedLanguage = transliteration?.language,
                                providerTranslationText = translation,
                            ),
                        )
                    }
                }
            }

            if (lines.isEmpty()) {
                val pElements = doc.getElementsByTagName("*")

                for (i in 0 until pElements.length) {
                    val pElement = pElements.item(i) as? Element ?: continue
                    if (!pElement.tagName.endsWith("p", ignoreCase = true)) continue

                    val begin = pElement.getAttribute("begin")
                    val end = pElement.getAttribute("end")
                    val dur = pElement.getAttribute("dur")
                    if (begin.isNullOrEmpty()) continue

                    val startTime = parseTime(begin, timingContext)
                    val endTime =
                        when {
                            end.isNotEmpty() -> parseTime(end, timingContext)
                            dur.isNotEmpty() -> startTime + parseTime(dur, timingContext)
                            else -> startTime + 5.0
                        }

                    val agent =
                        pElement.getAttribute("ttm:agent").takeIf { it.isNotEmpty() }
                            ?: pElement.attributes?.let { attrs ->
                                (0 until attrs.length)
                                    .map { attrs.item(it) }
                                    .firstOrNull {
                                        it.nodeName.endsWith(
                                            "agent",
                                            ignoreCase = true
                                        )
                                    }
                                    ?.nodeValue
                                    ?.takeIf { it.isNotEmpty() }
                            }
                    val lineKey = readAttributeBySuffix(pElement, "key")
                    val transliteration = lineKey?.let(transliterations::get)
                    val translation = lineKey?.let(translations::get)

                    val words = mutableListOf<ParsedWord>()
                    val lineText = StringBuilder()

                    parseSpanElements(pElement, words, lineText, startTime, endTime, false, timingContext)

                    if (lineText.isEmpty()) {
                        val directText = getDirectTextContent(pElement).trim()
                        if (directText.isNotEmpty()) {
                            lineText.append(directText)
                        }
                    }

                    if (lineText.isNotEmpty()) {
                        lines.add(
                            ParsedLine(
                                text = lineText.toString().trim(),
                                startTime = startTime,
                                endTime = endTime,
                                words = words,
                                isBackground = false,
                                agent = agent,
                                providerRomanizedText = transliteration?.text,
                                providerRomanizedWords = transliteration?.words,
                                providerRomanizedLanguage = transliteration?.language,
                                providerTranslationText = translation,
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        return lines.sortedBy { it.startTime }
    }

    private fun parseTransliterations(root: Element): Map<String, ParsedTransliteration> {
        val latinTransliterations = linkedMapOf<String, ParsedTransliteration>()
        val fallbackTransliterations = linkedMapOf<String, ParsedTransliteration>()
        val elements = root.getElementsByTagName("*")

        for (i in 0 until elements.length) {
            val transliterationElement = elements.item(i) as? Element ?: continue
            if (!transliterationElement.tagName.endsWith(
                    "transliteration",
                    ignoreCase = true
                )
            ) continue

            val language = readAttributeBySuffix(transliterationElement, "lang")
            val target =
                if (language?.contains("Latn", ignoreCase = true) == true) {
                    latinTransliterations
                } else {
                    fallbackTransliterations
                }

            val textElements = transliterationElement.getElementsByTagName("*")
            for (textIndex in 0 until textElements.length) {
                val textElement = textElements.item(textIndex) as? Element ?: continue
                if (!textElement.tagName.endsWith("text", ignoreCase = true)) continue

                val lineKey = readAttributeBySuffix(textElement, "for") ?: continue
                val parsed = parseTransliterationLine(textElement, language)
                if (parsed.text.isNotBlank() && lineKey !in target) {
                    target[lineKey] = parsed
                }
            }
        }

        return fallbackTransliterations.apply { putAll(latinTransliterations) }
    }

    private fun parseTranslations(root: Element): Map<String, String> {
        val translations = linkedMapOf<String, String>()
        val elements = root.getElementsByTagName("*")

        for (i in 0 until elements.length) {
            val translationElement = elements.item(i) as? Element ?: continue
            if (!translationElement.tagName.endsWith("translation", ignoreCase = true)) continue

            val textElements = translationElement.getElementsByTagName("*")
            for (textIndex in 0 until textElements.length) {
                val textElement = textElements.item(textIndex) as? Element ?: continue
                if (!textElement.tagName.endsWith("text", ignoreCase = true)) continue

                val lineKey = readAttributeBySuffix(textElement, "for") ?: continue
                val translatedText =
                    normalizeProvidedRomanization(textElement.textContent.orEmpty())
                        ?: continue
                translations[lineKey] = translatedText
            }
        }

        return translations
    }

    private fun parseTransliterationLine(
        element: Element,
        language: String?,
    ): ParsedTransliteration {
        val lineText = StringBuilder()
        val words = mutableListOf<String>()
        parseTransliterationNodes(element, lineText, words)

        return ParsedTransliteration(
            text = normalizeProvidedRomanization(lineText.toString()).orEmpty(),
            words = words.mapNotNull(::normalizeProvidedRomanization),
            language = language,
        )
    }

    private fun parseTransliterationNodes(
        element: Element,
        lineText: StringBuilder,
        words: MutableList<String>,
    ) {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            when (node.nodeType) {
                Node.ELEMENT_NODE -> {
                    val childElement = node as Element
                    if (childElement.tagName.endsWith("span", ignoreCase = true)) {
                        val rawText = childElement.textContent.orEmpty()
                        lineText.append(rawText)
                        normalizeProvidedRomanization(rawText)?.let(words::add)
                    } else {
                        parseTransliterationNodes(childElement, lineText, words)
                    }
                }

                Node.TEXT_NODE -> {
                    lineText.append(node.textContent.orEmpty())
                }
            }
        }
    }

    private fun normalizeProvidedRomanization(text: String): String? =
        text
            .replace(whitespaceRegex, " ")
            .trim()
            .takeIf { it.isNotEmpty() }

    private fun parseSpanElements(
        element: Element,
        words: MutableList<ParsedWord>,
        lineText: StringBuilder,
        lineStartTime: Double,
        lineEndTime: Double,
        isBackground: Boolean,
        timingContext: TimingContext,
    ) {
        val childNodes = element.childNodes

        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)

            when (node.nodeType) {
                Node.ELEMENT_NODE -> {
                    val childElement = node as Element
                    if (childElement.tagName.endsWith("span", ignoreCase = true)) {
                        val role =
                            childElement.getAttribute("role").takeIf { it.isNotEmpty() }
                                ?: childElement.getAttribute("ttm:role")
                        val isBgSpan = role == "x-bg" || isBackground

                        val wordBegin = childElement.getAttribute("begin")
                        val wordEnd = childElement.getAttribute("end")
                        val wordDur = childElement.getAttribute("dur")
                        val hasExplicitTiming =
                            wordBegin.isNotEmpty() && (wordEnd.isNotEmpty() || wordDur.isNotEmpty())

                        val nestedSpans = childElement.getElementsByTagName("*")
                        if (nestedSpans.length > 0 && hasDirectSpanChildren(childElement) && !hasExplicitTiming) {
                            parseSpanElements(childElement, words, lineText, lineStartTime, lineEndTime, isBgSpan, timingContext)
                        } else {
                            val wordText =
                                if (nestedSpans.length > 0) {
                                    childElement.textContent.orEmpty()
                                } else {
                                    getDirectTextContent(childElement)
                                }
                            if (wordText.isNotEmpty()) {
                                lineText.append(wordText)

                                if (hasExplicitTiming) {
                                    val rawWordStart = parseTime(wordBegin, timingContext)
                                    val rawWordEnd =
                                        if (wordEnd.isNotEmpty()) {
                                            parseTime(wordEnd, timingContext)
                                        } else {
                                            rawWordStart + parseTime(wordDur, timingContext)
                                        }
                                    val wordStartTime =
                                        normalizeChildTime(
                                            raw = rawWordStart,
                                            lineStartTime = lineStartTime,
                                            lineEndTime = lineEndTime,
                                            fallback = lineStartTime,
                                        )
                                    val wordEndTime =
                                        normalizeChildTime(
                                            raw = rawWordEnd,
                                            lineStartTime = lineStartTime,
                                            lineEndTime = lineEndTime,
                                            fallback = lineEndTime,
                                        ).coerceAtLeast(wordStartTime)
                                    val trimmedText = wordText.trim()
                                    if (trimmedText.isNotEmpty()) {
                                        words.add(
                                            ParsedWord(
                                                text = trimmedText,
                                                startTime = wordStartTime,
                                                endTime = wordEndTime,
                                                isBackground = isBgSpan,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Node.TEXT_NODE -> {
                    val text = node.textContent
                    if (text.isNotBlank()) {
                        lineText.append(text)
                    } else if (text.isNotEmpty() && !text.contains('\n')) {
                        if (words.isNotEmpty() && !words.last().text.endsWith(" ")) {
                            lineText.append(" ")
                            val lastWord = words.last()
                            words[words.lastIndex] = lastWord.copy(text = lastWord.text + " ")
                        }
                    }
                }
            }
        }
    }

    private fun hasDirectSpanChildren(element: Element): Boolean {
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val childElement = node as Element
                if (childElement.tagName.endsWith("span", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun getDirectTextContent(element: Element): String {
        val textBuilder = StringBuilder()
        val childNodes = element.childNodes

        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.TEXT_NODE) {
                textBuilder.append(node.textContent)
            }
        }

        return textBuilder.toString()
    }

    private fun normalizeChildTime(
        raw: Double?,
        lineStartTime: Double,
        lineEndTime: Double,
        fallback: Double,
    ): Double {
        if (raw == null || raw.isNaN() || raw.isInfinite()) return fallback
        val lineDuration = (lineEndTime - lineStartTime).coerceAtLeast(0.0)
        val isProbablyRelative =
            raw < (lineStartTime - 0.25) && raw <= (lineDuration + 1.0)
        val adjusted = if (isProbablyRelative) lineStartTime + raw else raw
        return adjusted.coerceIn(lineStartTime.coerceAtLeast(0.0), lineEndTime.coerceAtLeast(lineStartTime))
    }

    private fun readTimingContext(root: Element): TimingContext {
        fun getAttrBySuffix(suffix: String): String? {
            val attrs = root.attributes ?: return null
            for (i in 0 until attrs.length) {
                val node = attrs.item(i) ?: continue
                if (node.nodeName.endsWith(suffix, ignoreCase = true)) {
                    val v = node.nodeValue?.trim()
                    if (!v.isNullOrEmpty()) return v
                }
            }
            return null
        }

        val baseFrameRate = getAttrBySuffix("frameRate")?.toDoubleOrNull() ?: 30.0
        val frameRateMultiplierRaw = getAttrBySuffix("frameRateMultiplier")
        val frameRateMultiplier =
            frameRateMultiplierRaw
                ?.split(Regex("\\s+"))
                ?.mapNotNull { it.toDoubleOrNull() }
                ?.takeIf { it.size == 2 && it[1] != 0.0 }
                ?.let { it[0] / it[1] }
                ?: 1.0
        val frameRate = (baseFrameRate * frameRateMultiplier).coerceAtLeast(1.0)

        val tickRate =
            getAttrBySuffix("tickRate")?.toDoubleOrNull()
                ?: (frameRate * 1.0).coerceAtLeast(1.0)

        return TimingContext(
            tickRate = tickRate,
            frameRate = frameRate,
        )
    }

    private fun readAttributeBySuffix(
        element: Element,
        suffix: String,
    ): String? {
        val directValue =
            element
                .getAttribute(suffix)
                .takeIf { it.isNotBlank() }
        if (directValue != null) return directValue

        val attrs = element.attributes ?: return null
        for (i in 0 until attrs.length) {
            val node = attrs.item(i) ?: continue
            val name = node.nodeName ?: continue
            if (name.equals(suffix, ignoreCase = true) || name.endsWith(
                    ":$suffix",
                    ignoreCase = true
                )
            ) {
                val value = node.nodeValue?.trim()
                if (!value.isNullOrEmpty()) return value
            }
        }
        return null
    }

    private fun parseTime(
        timeStr: String,
        timingContext: TimingContext,
    ): Double {
        return try {
            val raw = timeStr.trim()
            if (raw.isEmpty()) return 0.0

            val offsetRegex = Regex("""^([0-9]+(?:\.[0-9]+)?)(h|ms|m|s|f|t)$""", RegexOption.IGNORE_CASE)
            offsetRegex.matchEntire(raw)?.let { m ->
                val value = m.groupValues[1].toDoubleOrNull() ?: return 0.0
                return when (m.groupValues[2].lowercase()) {
                    "h" -> value * 3600.0
                    "m" -> value * 60.0
                    "s" -> value
                    "ms" -> value / 1000.0
                    "f" -> value / timingContext.frameRate
                    "t" -> value / timingContext.tickRate
                    else -> value
                }
            }

            val cleanClock =
                raw
                    .replace(';', ':')
                    .trimEnd { it.isLetter() }

            if (cleanClock.contains(":")) {
                val parts = cleanClock.split(":")
                return when (parts.size) {
                    2 -> {
                        val minutes = parts[0].toDoubleOrNull() ?: 0.0
                        val seconds = parts[1].toDoubleOrNull() ?: 0.0
                        minutes * 60.0 + seconds
                    }

                    3 -> {
                        val hours = parts[0].toDoubleOrNull() ?: 0.0
                        val minutes = parts[1].toDoubleOrNull() ?: 0.0
                        val seconds = parts[2].toDoubleOrNull() ?: 0.0
                        hours * 3600.0 + minutes * 60.0 + seconds
                    }

                    4 -> {
                        val hours = parts[0].toDoubleOrNull() ?: 0.0
                        val minutes = parts[1].toDoubleOrNull() ?: 0.0
                        val seconds = parts[2].toDoubleOrNull() ?: 0.0
                        val frames = parts[3].toDoubleOrNull() ?: 0.0
                        hours * 3600.0 + minutes * 60.0 + seconds + (frames / timingContext.frameRate)
                    }

                    else -> {
                        cleanClock.toDoubleOrNull() ?: 0.0
                    }
                }
            }

            raw.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
