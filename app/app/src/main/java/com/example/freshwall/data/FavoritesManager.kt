package com.example.freshwall.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Stores favorited wallpapers as full [Wallpaper] objects (not just ids).
 * This lets the Favorites screen render wallpapers from *any* source —
 * Featured AND Pexels — without needing to re-query the original repository.
 *
 * Persisted as a JSON list in SharedPreferences.
 */
class FavoritesManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val _favorites = MutableStateFlow(read())
    val favorites: StateFlow<List<Wallpaper>> = _favorites.asStateFlow()

    fun isFavorite(id: String): Boolean = _favorites.value.any { it.id == id }

    /** Returns `true` if [wallpaper] is now favorited, `false` if it was un-favorited. */
    fun toggle(wallpaper: Wallpaper): Boolean {
        val current = _favorites.value
        val wasFavorite = current.any { it.id == wallpaper.id }
        val updated = if (wasFavorite) {
            current.filterNot { it.id == wallpaper.id }
        } else {
            // Prepend — newest favorites appear first in the list.
            listOf(wallpaper) + current
        }
        _favorites.value = updated
        prefs.edit()
            .remove(KEY_FAVS_LEGACY) // clean up the old Set<String> key if present
            .putString(KEY_FAVS, json.encodeToString(updated))
            .apply()
        return !wasFavorite
    }

    private fun read(): List<Wallpaper> {
        val raw = prefs.getString(KEY_FAVS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<Wallpaper>>(raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "freshwall_favorites"
        const val KEY_FAVS = "favorites_v2"
        // Old key, dropped on first write. Pre-Pexels favorites stored just
        // wallpaper ids here; we can't recover full Wallpaper objects from
        // an id alone, so we abandon legacy data on upgrade.
        const val KEY_FAVS_LEGACY = "favorites"
    }
}
