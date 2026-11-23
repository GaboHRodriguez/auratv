package com.auratv.player.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auratv.player.data.AppScreen
import com.auratv.player.data.Channel
import com.auratv.player.data.EpgProgram
import com.auratv.player.data.EpgSource
import com.auratv.player.data.LegalSourceCatalog
import com.auratv.player.data.LocalStore
import com.auratv.player.data.PlaylistSource
import com.auratv.player.data.TvRepository
import com.auratv.player.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AuraTvUiState(
    val currentScreen: AppScreen = AppScreen.PROFILES,
    val profiles: List<UserProfile> = emptyList(),
    val activeProfile: UserProfile? = null,
    val allPlaylists: List<PlaylistSource> = LegalSourceCatalog.defaultPlaylists,
    val enabledPlaylistIds: Set<String> = emptySet(),
    val allEpgSources: List<EpgSource> = LegalSourceCatalog.defaultEpgSources,
    val enabledEpgIds: Set<String> = emptySet(),
    val sourceChannelCounts: Map<String, Int> = emptyMap(),
    val failedSources: List<String> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val epgByKey: Map<String, List<EpgProgram>> = emptyMap(),
    val favoriteChannelIds: Set<String> = emptySet(),
    val activeChannel: Channel? = null,
    val selectedCategory: String = "Todos",
    val selectedDayOffset: Int = 0, // -1 (Ayer), 0 (Hoy), 1..5
    val searchQuery: String = "",
    val showEpgOverlay: Boolean = true,
    val showQuickGuide: Boolean = false,
    val showSourcesModal: Boolean = false,
    val showExitConfirmDialog: Boolean = false,
    val arePlayerControlsVisible: Boolean = true,
    val isMuted: Boolean = false,
    val isLoadingChannels: Boolean = false,
    val isLoadingEpg: Boolean = false,
    val statusMessage: String? = null
) {
    val filteredChannels: List<Channel>
        get() {
            val q = searchQuery.trim().lowercase()
            return channels.filter { ch ->
                val matchesCat = when (selectedCategory) {
                    "Todos" -> true
                    "Favoritos" -> ch.id in favoriteChannelIds || ch.streamUrl in favoriteChannelIds
                    else -> ch.category.equals(selectedCategory, ignoreCase = true)
                }
                val matchesQuery = q.isEmpty() ||
                    ch.name.lowercase().contains(q) ||
                    ch.number.toString() == q ||
                    ch.rawGroup.lowercase().contains(q)
                matchesCat && matchesQuery
            }
        }
}

class AuraTvViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LocalStore(application.applicationContext)
    private val repository = TvRepository()

    private val _uiState = MutableStateFlow(AuraTvUiState())
    val uiState: StateFlow<AuraTvUiState> = _uiState.asStateFlow()

    init {
        val profiles = store.loadProfiles()
        val customPlaylists = store.loadCustomPlaylists()
        val customEpg = store.loadCustomEpgSources()
        val enabledPlaylists = store.loadEnabledPlaylistIds()
        val enabledEpg = store.loadEnabledEpgIds()

        _uiState.update {
            it.copy(
                profiles = profiles,
                allPlaylists = LegalSourceCatalog.defaultPlaylists + customPlaylists,
                enabledPlaylistIds = enabledPlaylists,
                allEpgSources = LegalSourceCatalog.defaultEpgSources + customEpg,
                enabledEpgIds = enabledEpg
            )
        }
    }

    // === Perfiles ===
    fun selectProfile(profile: UserProfile) {
        val favs = store.loadFavorites(profile.id)
        val needsLoad = _uiState.value.channels.isEmpty()
        _uiState.update {
            it.copy(
                activeProfile = profile,
                favoriteChannelIds = favs,
                currentScreen = AppScreen.LIVE_TV,
                showEpgOverlay = true,
                showQuickGuide = false
            )
        }
        if (needsLoad) {
            syncSources()
        }
    }

    fun switchToProfiles() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.PROFILES,
                showSourcesModal = false,
                showQuickGuide = false
            )
        }
    }

    fun addProfile(name: String, avatarIndex: Int) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val created = store.createProfile(clean, avatarIndex)
        val updated = _uiState.value.profiles + created
        store.saveProfiles(updated)
        _uiState.update { it.copy(profiles = updated) }
    }

    fun updateProfile(profileId: String, newName: String, newAvatarIndex: Int) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val updated = _uiState.value.profiles.map { p ->
            if (p.id == profileId) p.copy(name = clean, avatarIndex = newAvatarIndex) else p
        }
        store.saveProfiles(updated)
        _uiState.update { state ->
            state.copy(
                profiles = updated,
                activeProfile = if (state.activeProfile?.id == profileId) {
                    state.activeProfile.copy(name = clean, avatarIndex = newAvatarIndex)
                } else state.activeProfile
            )
        }
    }

    fun deleteProfile(profileId: String) {
        if (_uiState.value.profiles.size <= 1) return
        val updated = _uiState.value.profiles.filterNot { it.id == profileId }
        store.saveProfiles(updated)
        _uiState.update { state ->
            state.copy(
                profiles = updated,
                activeProfile = if (state.activeProfile?.id == profileId) null else state.activeProfile
            )
        }
    }

    // === Sincronización de Fuentes & EPG ===
    fun syncSources() {
        val state = _uiState.value
        val activePlaylists = state.allPlaylists.filter { it.id in state.enabledPlaylistIds }
        val activeEpgs = state.allEpgSources.filter { it.id in state.enabledEpgIds }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingChannels = true,
                    statusMessage = "Cargando ${activePlaylists.size} fuentes legales abiertas..."
                )
            }
            val catalog = repository.loadPlaylists(activePlaylists)
            val currentActive = _uiState.value.activeChannel
            val nextActive = currentActive?.takeIf { cur -> catalog.channels.any { it.streamUrl == cur.streamUrl } }
                ?: catalog.channels.firstOrNull()

            _uiState.update {
                it.copy(
                    channels = catalog.channels,
                    sourceChannelCounts = catalog.sourceChannelCounts,
                    failedSources = catalog.failedSources,
                    activeChannel = nextActive,
                    isLoadingChannels = false,
                    isLoadingEpg = activeEpgs.isNotEmpty() && catalog.channels.isNotEmpty(),
                    statusMessage = if (catalog.channels.isEmpty()) {
                        "No se cargaron canales. Activa al menos una fuente en Fuentes."
                    } else {
                        "${catalog.channels.size} canales abiertos cargados"
                    }
                )
            }

            if (activeEpgs.isNotEmpty() && catalog.channels.isNotEmpty()) {
                val epgMap = repository.loadEpgGuides(activeEpgs, catalog.channels)
                _uiState.update {
                    it.copy(
                        epgByKey = epgMap,
                        isLoadingEpg = false
                    )
                }
            }
        }
    }

    fun togglePlaylistSource(sourceId: String) {
        val updated = _uiState.value.enabledPlaylistIds.toMutableSet().apply {
            if (!add(sourceId)) remove(sourceId)
        }
        store.saveEnabledPlaylistIds(updated)
        _uiState.update { it.copy(enabledPlaylistIds = updated) }
    }

    fun toggleEpgSource(sourceId: String) {
        val updated = _uiState.value.enabledEpgIds.toMutableSet().apply {
            if (!add(sourceId)) remove(sourceId)
        }
        store.saveEnabledEpgIds(updated)
        _uiState.update { it.copy(enabledEpgIds = updated) }
    }

    fun enableAllSpanishAndRegionalPresets() {
        val ids = LegalSourceCatalog.defaultPlaylists
            .filter { it.regionOrCategory in setOf("Argentina", "LatAm", "España") }
            .map { it.id }
            .toSet()
        val merged = _uiState.value.enabledPlaylistIds + ids
        store.saveEnabledPlaylistIds(merged)
        _uiState.update { it.copy(enabledPlaylistIds = merged) }
    }

    fun addCustomPlaylist(name: String, url: String) {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (cleanName.isEmpty() || !cleanUrl.startsWith("http", ignoreCase = true)) return

        val item = PlaylistSource(
            id = "custom_m3u_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            regionOrCategory = "Personalizada",
            provider = "Lista Propia",
            url = cleanUrl,
            estimatedChannels = 0,
            enabledByDefault = true,
            isCustom = true
        )
        val customList = store.loadCustomPlaylists() + item
        store.saveCustomPlaylists(customList)
        val enabled = _uiState.value.enabledPlaylistIds + item.id
        store.saveEnabledPlaylistIds(enabled)
        _uiState.update {
            it.copy(
                allPlaylists = LegalSourceCatalog.defaultPlaylists + customList,
                enabledPlaylistIds = enabled
            )
        }
    }

    fun removeCustomPlaylist(sourceId: String) {
        val customList = store.loadCustomPlaylists().filterNot { it.id == sourceId }
        store.saveCustomPlaylists(customList)
        val enabled = _uiState.value.enabledPlaylistIds - sourceId
        store.saveEnabledPlaylistIds(enabled)
        _uiState.update {
            it.copy(
                allPlaylists = LegalSourceCatalog.defaultPlaylists + customList,
                enabledPlaylistIds = enabled
            )
        }
    }

    fun addCustomEpg(name: String, url: String) {
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        if (cleanName.isEmpty() || !cleanUrl.startsWith("http", ignoreCase = true)) return

        val item = EpgSource(
            id = "custom_epg_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            region = "Personalizada",
            url = cleanUrl,
            enabledByDefault = true,
            isCustom = true
        )
        val customList = store.loadCustomEpgSources() + item
        store.saveCustomEpgSources(customList)
        val enabled = _uiState.value.enabledEpgIds + item.id
        store.saveEnabledEpgIds(enabled)
        _uiState.update {
            it.copy(
                allEpgSources = LegalSourceCatalog.defaultEpgSources + customList,
                enabledEpgIds = enabled
            )
        }
    }

    fun removeCustomEpg(sourceId: String) {
        val customList = store.loadCustomEpgSources().filterNot { it.id == sourceId }
        store.saveCustomEpgSources(customList)
        val enabled = _uiState.value.enabledEpgIds - sourceId
        store.saveEnabledEpgIds(enabled)
        _uiState.update {
            it.copy(
                allEpgSources = LegalSourceCatalog.defaultEpgSources + customList,
                enabledEpgIds = enabled
            )
        }
    }

    // === Reproducción y Guía ===
    fun selectChannel(channel: Channel, hideEpg: Boolean = false) {
        _uiState.update {
            it.copy(
                activeChannel = channel,
                showEpgOverlay = if (hideEpg) false else it.showEpgOverlay
            )
        }
    }

    fun stepChannel(delta: Int) {
        val list = _uiState.value.filteredChannels.ifEmpty { _uiState.value.channels }
        if (list.isEmpty()) return
        val currentIdx = list.indexOfFirst { it.id == _uiState.value.activeChannel?.id }
        val nextIdx = if (currentIdx < 0) 0 else (currentIdx + delta).floorMod(list.size)
        _uiState.update { it.copy(activeChannel = list[nextIdx]) }
    }

    fun toggleFavorite(channel: Channel) {
        val profile = _uiState.value.activeProfile ?: return
        val key = channel.streamUrl
        val updated = _uiState.value.favoriteChannelIds.toMutableSet().apply {
            if (!add(key)) remove(key)
        }
        store.saveFavorites(profile.id, updated)
        _uiState.update { it.copy(favoriteChannelIds = updated) }
    }

    fun isFavorite(channel: Channel): Boolean {
        val favs = _uiState.value.favoriteChannelIds
        return channel.streamUrl in favs || channel.id in favs
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectDayOffset(dayOffset: Int) {
        _uiState.update { it.copy(selectedDayOffset = dayOffset.coerceIn(-1, 5)) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setShowEpgOverlay(show: Boolean) {
        _uiState.update {
            it.copy(
                showEpgOverlay = show,
                showQuickGuide = if (show) false else it.showQuickGuide
            )
        }
    }

    fun setShowQuickGuide(show: Boolean) {
        _uiState.update {
            it.copy(
                showQuickGuide = show,
                showEpgOverlay = if (show) false else it.showEpgOverlay
            )
        }
    }

    fun setShowSourcesModal(show: Boolean) {
        _uiState.update { it.copy(showSourcesModal = show) }
    }

    fun setShowExitConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showExitConfirmDialog = show) }
    }

    fun setPlayerControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(arePlayerControlsVisible = visible) }
    }

    fun stopPlayback() {
        _uiState.update {
            it.copy(
                activeChannel = null,
                showExitConfirmDialog = false,
                showQuickGuide = false,
                showSourcesModal = false
            )
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun programsFor(channel: Channel, dayOffset: Int = _uiState.value.selectedDayOffset): List<EpgProgram> {
        return repository.resolveProgramsForChannel(channel, _uiState.value.epgByKey, dayOffset)
    }

    fun currentProgramFor(channel: Channel): EpgProgram? {
        val now = System.currentTimeMillis()
        val today = programsFor(channel, 0)
        return today.firstOrNull { now in it.startMs until it.endMs } ?: today.firstOrNull()
    }

    fun dayBoundsMs(dayOffset: Int): Pair<Long, Long> = repository.dayBoundsMs(dayOffset)

    private fun Int.floorMod(mod: Int): Int {
        val r = this % mod
        return if (r < 0) r + mod else r
    }
}
