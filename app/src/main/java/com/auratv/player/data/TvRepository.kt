package com.auratv.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class LoadedCatalog(
    val channels: List<Channel>,
    val sourceChannelCounts: Map<String, Int>,
    val failedSources: List<String>
)

class TvRepository {

    suspend fun loadPlaylists(sources: List<PlaylistSource>): LoadedCatalog = withContext(Dispatchers.IO) {
        val counts = LinkedHashMap<String, Int>()
        val failed = ArrayList<String>()

        val fetched = sources.map { source ->
            async {
                runCatching {
                    val body = downloadText(source.url)
                    source to M3uParser.parse(body, source)
                }.getOrElse {
                    source to null
                }
            }
        }.awaitAll()

        val seenUrls = HashSet<String>(4096)
        val merged = ArrayList<Channel>(2048)

        for ((source, parsed) in fetched) {
            if (parsed == null) {
                failed.add(source.name)
                counts[source.id] = 0
                continue
            }
            counts[source.id] = parsed.size
            for (ch in parsed) {
                val urlKey = ch.streamUrl.trim()
                if (seenUrls.add(urlKey)) {
                    merged.add(ch)
                }
            }
        }

        val numbered = merged.mapIndexed { idx, ch ->
            ch.copy(number = idx + 1)
        }

        LoadedCatalog(
            channels = numbered,
            sourceChannelCounts = counts,
            failedSources = failed
        )
    }

    suspend fun loadEpgGuides(
        epgSources: List<EpgSource>,
        channels: List<Channel>
    ): Map<String, List<EpgProgram>> = withContext(Dispatchers.IO) {
        if (epgSources.isEmpty() || channels.isEmpty()) return@withContext emptyMap()

        val allowedKeys = HashSet<String>(channels.size * 2)
        for (ch in channels) {
            ch.tvgId?.let { id ->
                allowedKeys.add(id.lowercase())
                allowedKeys.add(M3uParser.normalizeKey(id))
            }
            allowedKeys.add(M3uParser.normalizeKey(ch.name))
        }

        val now = System.currentTimeMillis()
        val windowStart = now - 24L * 60L * 60L * 1000L
        val windowEnd = now + 5L * 24L * 60L * 60L * 1000L

        val merged = HashMap<String, MutableList<EpgProgram>>()
        val parsedMaps = epgSources.map { src ->
            async {
                runCatching {
                    val conn = openConnection(src.url, timeoutMs = 20_000)
                    conn.inputStream.use { input ->
                        XmltvParser.parse(input, allowedKeys, windowStart, windowEnd)
                    }
                }.getOrDefault(emptyMap())
            }
        }.awaitAll()

        for (map in parsedMaps) {
            for ((key, programs) in map) {
                val target = merged.getOrPut(key) { ArrayList() }
                if (target.isEmpty()) {
                    target.addAll(programs)
                }
            }
        }

        merged
    }

    fun resolveProgramsForChannel(
        channel: Channel,
        epgByKey: Map<String, List<EpgProgram>>,
        dayOffset: Int
    ): List<EpgProgram> {
        val exact = channel.tvgId?.let { epgByKey[it.lowercase()] }
            ?: channel.tvgId?.let { epgByKey[M3uParser.normalizeKey(it)] }
            ?: epgByKey[M3uParser.normalizeKey(channel.name)]

        val (dayStart, dayEnd) = dayBoundsMs(dayOffset)
        if (!exact.isNullOrEmpty()) {
            val filtered = exact.filter { it.endMs > dayStart && it.startMs < dayEnd }
            if (filtered.isNotEmpty()) return filtered
        }

        // Bloques de programación fallback de 2 horas para señales abiertas sin XMLTV
        val slotMs = 2L * 60L * 60L * 1000L
        val blocks = ArrayList<EpgProgram>(12)
        var cursor = dayStart
        var index = 0
        while (cursor < dayEnd) {
            val next = (cursor + slotMs).coerceAtMost(dayEnd)
            blocks.add(
                EpgProgram(
                    id = "fallback_${channel.id}_${dayOffset}_$index",
                    channelKey = channel.id,
                    title = "${channel.name} · Emisión abierta",
                    description = "Transmisión oficial Free-To-Air (${channel.sourceName}). Conecta una guía XMLTV compatible en Fuentes para ver programación detallada.",
                    category = channel.category,
                    iconUrl = channel.logoUrl,
                    rating = "ATP",
                    startMs = cursor,
                    endMs = next
                )
            )
            cursor = next
            index++
        }
        return blocks
    }

    fun dayBoundsMs(dayOffset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + 24L * 60L * 60L * 1000L
        return start to end
    }

    private fun downloadText(urlStr: String): String {
        val conn = openConnection(urlStr, timeoutMs = 15_000)
        return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun openConnection(urlStr: String, timeoutMs: Int): HttpURLConnection {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AuraTV/1.0")
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            error("HTTP $code")
        }
        return conn
    }
}
