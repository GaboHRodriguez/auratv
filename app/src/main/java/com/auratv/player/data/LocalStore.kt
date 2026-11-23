package com.auratv.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalStore(context: Context) {

    private val prefs = context.getSharedPreferences("auratv_store_v1", Context.MODE_PRIVATE)

    fun loadProfiles(): List<UserProfile> {
        val raw = prefs.getString(KEY_PROFILES, null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                UserProfile(id = "profile_main", name = "Principal", avatarIndex = 0),
                UserProfile(id = "profile_living", name = "Living TV", avatarIndex = 3)
            )
            saveProfiles(defaults)
            return defaults
        }
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                UserProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    avatarIndex = obj.optInt("avatarIndex", 0)
                )
            }
        }.getOrElse {
            listOf(UserProfile(id = "profile_main", name = "Principal", avatarIndex = 0))
        }
    }

    fun saveProfiles(profiles: List<UserProfile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("avatarIndex", p.avatarIndex)
            )
        }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }

    fun createProfile(name: String, avatarIndex: Int): UserProfile {
        return UserProfile(
            id = "profile_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            avatarIndex = avatarIndex
        )
    }

    fun loadFavorites(profileId: String): Set<String> {
        return prefs.getStringSet("${KEY_FAVS_PREFIX}$profileId", emptySet())?.toSet() ?: emptySet()
    }

    fun saveFavorites(profileId: String, favorites: Set<String>) {
        prefs.edit().putStringSet("${KEY_FAVS_PREFIX}$profileId", favorites).apply()
    }

    fun loadEnabledPlaylistIds(): Set<String> {
        val saved = prefs.getStringSet(KEY_ENABLED_PLAYLISTS, null)
        if (saved != null) return saved.toSet()
        val defaults = LegalSourceCatalog.defaultPlaylists
            .filter { it.enabledByDefault }
            .map { it.id }
            .toSet()
        saveEnabledPlaylistIds(defaults)
        return defaults
    }

    fun saveEnabledPlaylistIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_ENABLED_PLAYLISTS, ids).apply()
    }

    fun loadEnabledEpgIds(): Set<String> {
        val saved = prefs.getStringSet(KEY_ENABLED_EPG, null)
        if (saved != null) return saved.toSet()
        val defaults = LegalSourceCatalog.defaultEpgSources
            .filter { it.enabledByDefault }
            .map { it.id }
            .toSet()
        saveEnabledEpgIds(defaults)
        return defaults
    }

    fun saveEnabledEpgIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_ENABLED_EPG, ids).apply()
    }

    fun loadCustomPlaylists(): List<PlaylistSource> {
        val raw = prefs.getString(KEY_CUSTOM_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                PlaylistSource(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    regionOrCategory = obj.optString("regionOrCategory", "Personalizada"),
                    provider = "Lista Propia",
                    url = obj.getString("url"),
                    estimatedChannels = obj.optInt("estimatedChannels", 0),
                    enabledByDefault = true,
                    isCustom = true
                )
            }
        }.getOrElse { emptyList() }
    }

    fun saveCustomPlaylists(list: List<PlaylistSource>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("regionOrCategory", s.regionOrCategory)
                    .put("url", s.url)
                    .put("estimatedChannels", s.estimatedChannels)
            )
        }
        prefs.edit().putString(KEY_CUSTOM_PLAYLISTS, arr.toString()).apply()
    }

    fun loadCustomEpgSources(): List<EpgSource> {
        val raw = prefs.getString(KEY_CUSTOM_EPG, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                EpgSource(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    region = "Personalizada",
                    url = obj.getString("url"),
                    enabledByDefault = true,
                    isCustom = true
                )
            }
        }.getOrElse { emptyList() }
    }

    fun saveCustomEpgSources(list: List<EpgSource>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("url", s.url)
            )
        }
        prefs.edit().putString(KEY_CUSTOM_EPG, arr.toString()).apply()
    }

    companion object {
        private const val KEY_PROFILES = "profiles_json"
        private const val KEY_FAVS_PREFIX = "favs_"
        private const val KEY_ENABLED_PLAYLISTS = "enabled_playlists"
        private const val KEY_ENABLED_EPG = "enabled_epg"
        private const val KEY_CUSTOM_PLAYLISTS = "custom_playlists_json"
        private const val KEY_CUSTOM_EPG = "custom_epg_json"
    }
}
