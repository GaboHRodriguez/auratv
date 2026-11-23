package com.auratv.player.data

data class UserProfile(
    val id: String,
    val name: String,
    val avatarIndex: Int = 0
)

data class PlaylistSource(
    val id: String,
    val name: String,
    val regionOrCategory: String,
    val provider: String,
    val url: String,
    val estimatedChannels: Int,
    val enabledByDefault: Boolean = false,
    val isCustom: Boolean = false
)

data class EpgSource(
    val id: String,
    val name: String,
    val region: String,
    val url: String,
    val enabledByDefault: Boolean = false,
    val isCustom: Boolean = false
)

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val tvgId: String? = null,
    val rawGroup: String = "General",
    val category: String = "Entretenimiento",
    val sourceId: String = "",
    val sourceName: String = "",
    val userAgent: String? = null,
    val referer: String? = null
)

data class EpgProgram(
    val id: String,
    val channelKey: String,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val iconUrl: String? = null,
    val rating: String? = null,
    val startMs: Long,
    val endMs: Long
) {
    val durationMinutes: Int
        get() = (((endMs - startMs) / 60_000L).toInt()).coerceAtLeast(1)

    fun stateAt(nowMs: Long): ProgramState = when {
        nowMs in startMs until endMs -> ProgramState.LIVE
        endMs <= nowMs -> ProgramState.PAST
        else -> ProgramState.FUTURE
    }
}

enum class ProgramState {
    PAST, LIVE, FUTURE
}

enum class AppScreen {
    PROFILES,
    LIVE_TV
}
