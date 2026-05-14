package com.example.freshwall.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class FeaturedRepository(
    private val context: Context,
    private val json: Json = DefaultJson,
) : WallpaperRepository {

    override suspend fun getWallpapers(): List<Wallpaper> = withContext(Dispatchers.IO) {
        context.assets.open(ASSET_FILE).bufferedReader().use { reader ->
            json.decodeFromString<WallpaperManifest>(reader.readText()).wallpapers
        }
    }

    private companion object {
        const val ASSET_FILE = "wallpapers.json"
        val DefaultJson = Json { ignoreUnknownKeys = true }
    }
}
