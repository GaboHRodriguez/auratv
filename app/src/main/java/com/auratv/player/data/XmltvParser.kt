package com.auratv.player.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

object XmltvParser {

    /**
     * Parsea XMLTV (.xml o .xml.gz) de forma streaming y conserva solo los programas
     * dentro de la ventana útil (desde ayer hasta +5 días) para mantener bajo uso de RAM en TV Box.
     */
    fun parse(
        rawStream: InputStream,
        allowedKeys: Set<String>,
        windowStartMs: Long,
        windowEndMs: Long
    ): Map<String, List<EpgProgram>> {
        val buffered = BufferedInputStream(rawStream, 32 * 1024)
        buffered.mark(4)
        val b0 = buffered.read()
        val b1 = buffered.read()
        buffered.reset()
        val stream: InputStream = if (b0 == 0x1f && b1 == 0x8b) {
            GZIPInputStream(buffered, 32 * 1024)
        } else {
            buffered
        }

        val result = HashMap<String, MutableList<EpgProgram>>()
        val channelDisplayMap = HashMap<String, String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, "UTF-8")

        val dateFmtTz = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
        val dateFmtUtc = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        fun parseTimestamp(raw: String?): Long? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            return try {
                if (trimmed.contains(" ") || trimmed.contains("+") || trimmed.contains("-")) {
                    dateFmtTz.parse(trimmed)?.time
                } else {
                    dateFmtUtc.parse(trimmed.take(14))?.time
                }
            } catch (_: Exception) {
                null
            }
        }

        var eventType = parser.eventType
        var currentChannelId: String? = null
        var inProgramme = false
        var progChannelId = ""
        var progStartMs = 0L
        var progEndMs = 0L
        var progTitle = ""
        var progDesc = ""
        var progCategory = "General"
        var progIcon: String? = null
        var progRating: String? = null
        var inRating = false
        var progCount = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "channel" -> {
                            currentChannelId = parser.getAttributeValue(null, "id")?.trim()
                        }
                        "display-name" -> {
                            val chId = currentChannelId
                            if (chId != null) {
                                val display = runCatching { parser.nextText().trim() }.getOrDefault("")
                                if (display.isNotBlank() && !channelDisplayMap.containsKey(chId)) {
                                    channelDisplayMap[chId] = M3uParser.normalizeKey(display)
                                }
                            }
                        }
                        "programme" -> {
                            val chId = parser.getAttributeValue(null, "channel")?.trim().orEmpty()
                            val start = parseTimestamp(parser.getAttributeValue(null, "start"))
                            val stop = parseTimestamp(parser.getAttributeValue(null, "stop"))
                            val normId = M3uParser.normalizeKey(chId)
                            val displayNorm = channelDisplayMap[chId]
                            val matchesAllowed = allowedKeys.isEmpty() ||
                                chId.lowercase() in allowedKeys ||
                                normId in allowedKeys ||
                                (displayNorm != null && displayNorm in allowedKeys)

                            if (matchesAllowed && start != null && stop != null && stop > start &&
                                stop >= windowStartMs && start <= windowEndMs
                            ) {
                                inProgramme = true
                                progChannelId = chId
                                progStartMs = start
                                progEndMs = stop
                                progTitle = ""
                                progDesc = ""
                                progCategory = "General"
                                progIcon = null
                                progRating = null
                                inRating = false
                            } else {
                                inProgramme = false
                            }
                        }
                        "title" -> if (inProgramme && progTitle.isEmpty()) {
                            progTitle = runCatching { parser.nextText().trim() }.getOrDefault("")
                        }
                        "desc" -> if (inProgramme && progDesc.isEmpty()) {
                            progDesc = runCatching { parser.nextText().trim() }.getOrDefault("")
                        }
                        "category" -> if (inProgramme && progCategory == "General") {
                            val cat = runCatching { parser.nextText().trim() }.getOrDefault("")
                            if (cat.isNotBlank()) progCategory = cat
                        }
                        "icon" -> if (inProgramme && progIcon == null) {
                            progIcon = parser.getAttributeValue(null, "src")?.trim()
                        }
                        "rating" -> if (inProgramme) {
                            inRating = true
                        }
                        "value" -> if (inProgramme && inRating && progRating == null) {
                            progRating = runCatching { parser.nextText().trim() }.getOrNull()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "channel" -> currentChannelId = null
                        "rating" -> inRating = false
                        "programme" -> {
                            if (inProgramme && progTitle.isNotBlank()) {
                                val keysToStore = buildSet {
                                    add(progChannelId.lowercase())
                                    add(M3uParser.normalizeKey(progChannelId))
                                    channelDisplayMap[progChannelId]?.let { add(it) }
                                }
                                val item = EpgProgram(
                                    id = "${progChannelId}_${progStartMs}_${progCount++}",
                                    channelKey = progChannelId,
                                    title = progTitle,
                                    description = progDesc,
                                    category = progCategory,
                                    iconUrl = progIcon,
                                    rating = progRating,
                                    startMs = progStartMs,
                                    endMs = progEndMs
                                )
                                for (k in keysToStore) {
                                    if (k.isNotBlank() && (allowedKeys.isEmpty() || k in allowedKeys)) {
                                        result.getOrPut(k) { ArrayList() }.add(item)
                                    }
                                }
                            }
                            inProgramme = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        result.values.forEach { list -> list.sortBy { it.startMs } }
        return result
    }
}
