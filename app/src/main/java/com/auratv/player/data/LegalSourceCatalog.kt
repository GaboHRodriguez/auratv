package com.auratv.player.data

/**
 * Catálogo de fuentes legales abiertas (Free-To-Air / emisiones públicas oficiales)
 * y guías XMLTV verificadas en vivo. El usuario puede activar/desactivar cualquier fuente
 * o añadir sus propias listas M3U/XMLTV autorizadas desde la pantalla de Fuentes.
 */
object LegalSourceCatalog {

    val defaultPlaylists: List<PlaylistSource> = listOf(
        // === Argentina ===
        PlaylistSource(
            id = "iptv-org-ar",
            name = "Argentina · TV Abierta",
            regionOrCategory = "Argentina",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/ar.m3u",
            estimatedChannels = 221,
            enabledByDefault = true
        ),
        PlaylistSource(
            id = "freetv-ar",
            name = "Argentina · Curado HD",
            regionOrCategory = "Argentina",
            provider = "Free-TV",
            url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_argentina.m3u8",
            estimatedChannels = 26,
            enabledByDefault = true
        ),
        PlaylistSource(
            id = "m3ucl-ar",
            name = "Argentina · Comunitaria",
            regionOrCategory = "Argentina",
            provider = "M3U.CL",
            url = "https://www.m3u.cl/lista/AR.m3u",
            estimatedChannels = 147,
            enabledByDefault = true
        ),

        // === Latinoamérica ===
        PlaylistSource(
            id = "iptv-org-uy",
            name = "Uruguay · TV Abierta",
            regionOrCategory = "LatAm",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/uy.m3u",
            estimatedChannels = 52
        ),
        PlaylistSource(
            id = "iptv-org-cl",
            name = "Chile · TV Abierta",
            regionOrCategory = "LatAm",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/cl.m3u",
            estimatedChannels = 213
        ),
        PlaylistSource(
            id = "freetv-cl",
            name = "Chile · Curado HD",
            regionOrCategory = "LatAm",
            provider = "Free-TV",
            url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_chile.m3u8",
            estimatedChannels = 26
        ),
        PlaylistSource(
            id = "m3ucl-cl",
            name = "Chile · Comunitaria",
            regionOrCategory = "LatAm",
            provider = "M3U.CL",
            url = "https://www.m3u.cl/lista/CL.m3u",
            estimatedChannels = 395
        ),
        PlaylistSource(
            id = "iptv-org-py",
            name = "Paraguay · TV Abierta",
            regionOrCategory = "LatAm",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/py.m3u",
            estimatedChannels = 110
        ),
        PlaylistSource(
            id = "iptv-org-co",
            name = "Colombia · TV Abierta",
            regionOrCategory = "LatAm",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/co.m3u",
            estimatedChannels = 169
        ),
        PlaylistSource(
            id = "iptv-org-mx",
            name = "México · TV Abierta",
            regionOrCategory = "LatAm",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/mx.m3u",
            estimatedChannels = 178
        ),
        PlaylistSource(
            id = "freetv-mx",
            name = "México · Curado HD",
            regionOrCategory = "LatAm",
            provider = "Free-TV",
            url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_mexico.m3u8",
            estimatedChannels = 14
        ),
        PlaylistSource(
            id = "iptv-org-pe",
            name = "Perú · TV Abierta",
            regionOrCategory = "LatAm",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/pe.m3u",
            estimatedChannels = 219
        ),

        // === España & Hispanoamérica ===
        PlaylistSource(
            id = "tdt-tv",
            name = "TDTChannels · TV Abierta",
            regionOrCategory = "España",
            provider = "TDTChannels",
            url = "https://www.tdtchannels.com/lists/tv.m3u8",
            estimatedChannels = 574,
            enabledByDefault = true
        ),
        PlaylistSource(
            id = "iptv-org-es",
            name = "España · TV Abierta",
            regionOrCategory = "España",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/countries/es.m3u",
            estimatedChannels = 299
        ),
        PlaylistSource(
            id = "freetv-es",
            name = "España · Curado HD",
            regionOrCategory = "España",
            provider = "Free-TV",
            url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_spain.m3u8",
            estimatedChannels = 48
        ),
        PlaylistSource(
            id = "m3ucl-es",
            name = "España · Comunitaria",
            regionOrCategory = "España",
            provider = "M3U.CL",
            url = "https://www.m3u.cl/lista/ES.m3u",
            estimatedChannels = 58
        ),
        PlaylistSource(
            id = "m3ucl-total",
            name = "LatAm & España · Total",
            regionOrCategory = "Español",
            provider = "M3U.CL",
            url = "https://www.m3u.cl/lista/total.m3u",
            estimatedChannels = 1094
        ),
        PlaylistSource(
            id = "iptv-org-spa",
            name = "Todo en Español (Global)",
            regionOrCategory = "Español",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/languages/spa.m3u",
            estimatedChannels = 2427
        ),

        // === Categorías Temáticas ===
        PlaylistSource(
            id = "iptv-org-movies",
            name = "Películas · Señales Abiertas",
            regionOrCategory = "Pelis y Series",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
            estimatedChannels = 782
        ),
        PlaylistSource(
            id = "iptv-org-series",
            name = "Series · Señales Abiertas",
            regionOrCategory = "Pelis y Series",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/series.m3u",
            estimatedChannels = 476
        ),
        PlaylistSource(
            id = "iptv-org-sports",
            name = "Deportes · Señales Abiertas",
            regionOrCategory = "Deportes",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
            estimatedChannels = 538
        ),
        PlaylistSource(
            id = "iptv-org-news",
            name = "Noticias · Señales Abiertas",
            regionOrCategory = "Noticias",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/news.m3u",
            estimatedChannels = 1067
        ),
        PlaylistSource(
            id = "iptv-org-music",
            name = "Música · Señales Abiertas",
            regionOrCategory = "Música",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/music.m3u",
            estimatedChannels = 782
        ),
        PlaylistSource(
            id = "tdt-radio",
            name = "TDTChannels · Radios Abiertas",
            regionOrCategory = "Música",
            provider = "TDTChannels",
            url = "https://www.tdtchannels.com/lists/radio.m3u8",
            estimatedChannels = 1052
        ),
        PlaylistSource(
            id = "iptv-org-kids",
            name = "Kids · Señales Abiertas",
            regionOrCategory = "Kids",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/kids.m3u",
            estimatedChannels = 458
        ),
        PlaylistSource(
            id = "iptv-org-doc",
            name = "Documentales · Señales Abiertas",
            regionOrCategory = "Documentales",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/documentary.m3u",
            estimatedChannels = 251
        ),
        PlaylistSource(
            id = "iptv-org-ent",
            name = "Entretenimiento · Señales Abiertas",
            regionOrCategory = "Entretenimiento",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
            estimatedChannels = 897
        ),

        // === Internacional / Global ===
        PlaylistSource(
            id = "freetv-us",
            name = "Estados Unidos · Curado HD",
            regionOrCategory = "Global",
            provider = "Free-TV",
            url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_usa.m3u8",
            estimatedChannels = 28
        ),
        PlaylistSource(
            id = "freetv-all",
            name = "Free-TV · Catálogo Curado Global",
            regionOrCategory = "Global",
            provider = "Free-TV",
            url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8",
            estimatedChannels = 2203
        ),
        PlaylistSource(
            id = "iptv-org-all",
            name = "iptv-org · Catálogo Mundial Completo",
            regionOrCategory = "Global",
            provider = "iptv-org",
            url = "https://iptv-org.github.io/iptv/index.m3u",
            estimatedChannels = 11512
        )
    )

    val defaultEpgSources: List<EpgSource> = listOf(
        EpgSource(
            id = "epg-ar",
            name = "EPGShare01 · Argentina",
            region = "Argentina",
            url = "https://epgshare01.online/epgshare01/epg_ripper_AR1.xml.gz",
            enabledByDefault = true
        ),
        EpgSource(
            id = "epg-tdt",
            name = "TDTChannels · Guía TV",
            region = "España",
            url = "https://www.tdtchannels.com/epg/TV.xml.gz",
            enabledByDefault = true
        ),
        EpgSource(
            id = "epg-cl",
            name = "EPGShare01 · Chile",
            region = "LatAm",
            url = "https://epgshare01.online/epgshare01/epg_ripper_CL1.xml.gz"
        ),
        EpgSource(
            id = "epg-co",
            name = "EPGShare01 · Colombia",
            region = "LatAm",
            url = "https://epgshare01.online/epgshare01/epg_ripper_CO1.xml.gz"
        ),
        EpgSource(
            id = "epg-mx",
            name = "EPGShare01 · México",
            region = "LatAm",
            url = "https://epgshare01.online/epgshare01/epg_ripper_MX1.xml.gz"
        ),
        EpgSource(
            id = "epg-es",
            name = "EPGShare01 · España",
            region = "España",
            url = "https://epgshare01.online/epgshare01/epg_ripper_ES1.xml.gz"
        )
    )

    val categoryTabs: List<String> = listOf(
        "Todos",
        "Favoritos",
        "Noticias",
        "Deportes",
        "Pelis y Series",
        "Música",
        "Kids",
        "Documentales",
        "Entretenimiento"
    )
}
