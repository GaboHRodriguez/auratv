package com.auratv.player.data

object M3uParser {

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    fun parse(content: String, source: PlaylistSource): List<Channel> {
        val channels = ArrayList<Channel>(256)
        var pendingName: String? = null
        var pendingTvgId: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String = source.regionOrCategory
        var pendingUserAgent: String? = null
        var pendingReferer: String? = null

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val attrs = HashMap<String, String>()
                    for (match in attrRegex.findAll(line)) {
                        attrs[match.groupValues[1].lowercase()] = match.groupValues[2].trim()
                    }
                    val commaIdx = line.lastIndexOf(',')
                    val title = if (commaIdx >= 0 && commaIdx < line.length - 1) {
                        line.substring(commaIdx + 1).trim()
                    } else {
                        attrs["tvg-name"].orEmpty()
                    }

                    pendingName = cleanChannelName(title.ifBlank { attrs["tvg-name"].orEmpty() })
                    pendingTvgId = attrs["tvg-id"]?.ifBlank { null }
                    pendingLogo = attrs["tvg-logo"]?.ifBlank { null }
                    pendingGroup = attrs["group-title"]?.ifBlank { source.regionOrCategory } ?: source.regionOrCategory
                    pendingUserAgent = null
                    pendingReferer = null
                }

                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val opt = line.substringAfter(":", "").trim()
                    when {
                        opt.startsWith("http-user-agent=", ignoreCase = true) ->
                            pendingUserAgent = opt.substringAfter("=").trim()
                        opt.startsWith("http-referrer=", ignoreCase = true) ->
                            pendingReferer = opt.substringAfter("=").trim()
                    }
                }

                line.startsWith("#") -> {
                    // Other comments / tags ignored
                }

                line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true) -> {
                    val name = pendingName
                    if (!name.isNullOrBlank()) {
                        val normalizedCategory = normalizeCategory(pendingGroup, name, source.regionOrCategory)
                        val id = "${source.id}_${channels.size}_${line.hashCode()}"
                        channels.add(
                            Channel(
                                id = id,
                                number = 0,
                                name = name,
                                streamUrl = line,
                                logoUrl = pendingLogo,
                                tvgId = pendingTvgId,
                                rawGroup = pendingGroup,
                                category = normalizedCategory,
                                sourceId = source.id,
                                sourceName = source.name,
                                userAgent = pendingUserAgent,
                                referer = pendingReferer
                            )
                        )
                    }
                    pendingName = null
                    pendingTvgId = null
                    pendingLogo = null
                    pendingUserAgent = null
                    pendingReferer = null
                }
            }
        }
        return channels
    }

    private fun cleanChannelName(raw: String): String {
        return raw
            .replace(Regex("""\s*\[Geo-blocked\]|\s*\[Not 24/7\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    fun normalizeCategory(rawGroup: String, channelName: String, sourceHint: String): String {
        val text = "$rawGroup $channelName $sourceHint".lowercase()
        return when {
            listOf("sport", "deport", "futbol", "fútbol", "tyc", "espn", "fox sports", "directv sports", "gol", "motor").any { it in text } ->
                "Deportes"
            listOf("movie", "cine", "pelic", "pelíc", "series", "film", "cinema", "novela", "drama", "action", "classic").any { it in text } ->
                "Pelis y Series"
            listOf("news", "notic", "inform", "24h", "tn ", "c5n", "ln+", "crónica", "cronica", "a24", "cnn", "dw ", "france 24", "rtve 24").any { it in text } ->
                "Noticias"
            listOf("kid", "infantil", "cartoon", "dibujo", "paka", "clan", "anime", "animation", "niñ").any { it in text } ->
                "Kids"
            listOf("music", "músic", "radio", "mtv", "concert", "jazz", "rock", "pop", "fm").any { it in text } ->
                "Música"
            listOf("docu", "cultur", "histori", "scienc", "cienc", "educa", "encuentro", "nat geo", "discovery").any { it in text } ->
                "Documentales"
            else -> "Entretenimiento"
        }
    }

    fun normalizeKey(value: String): String {
        return value.lowercase()
            .replace(Regex("""\.(ar|es|cl|mx|co|uy|py|pe|us|tv)$"""), "")
            .replace(Regex("""[^a-z0-9]"""), "")
    }
}
