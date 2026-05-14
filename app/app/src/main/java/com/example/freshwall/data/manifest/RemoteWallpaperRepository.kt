package com.example.freshwall.data.manifest

import android.content.Context
import android.util.Log
import com.example.freshwall.BuildConfig
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.WallpaperManifest
import com.example.freshwall.data.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "FreshWallManifest"
private const val CACHE_FILE = "wallpaper-manifest.json"
private const val NETWORK_TIMEOUT_SEC = 10L

/**
 * Wallpaper catalog backed by a remote manifest (`manifest.json`) served
 * from Cloudflare R2 (or any static HTTPS endpoint).
 *
 * Fetch strategy on [getWallpapers]:
 * 1. Hit the network with a short timeout.
 * 2. On success: overwrite the local cache, return the fresh list.
 * 3. On failure: fall back to the cached copy if we have one.
 * 4. If neither network nor cache: throw the network error so the UI can
 *    show an explicit empty / try-again state.
 *
 * The manifest URL is configured at build time via `BuildConfig.WALLPAPER_MANIFEST_URL`,
 * which the `app/local.properties` `wallpaper.manifest.url` entry feeds.
 * If that property is empty the repository is "unconfigured" — getWallpapers()
 * returns an empty list immediately so the app still builds + runs cleanly
 * for forks / CI / pre-R2-setup state.
 */
class RemoteWallpaperRepository(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val manifestUrl: String = BuildConfig.WALLPAPER_MANIFEST_URL,
    private val json: Json = DefaultJson,
) : WallpaperRepository {

    /** `true` when a manifest URL has actually been configured. */
    val isConfigured: Boolean get() = manifestUrl.isNotBlank()

    private val cacheFile: File by lazy { File(context.filesDir, CACHE_FILE) }

    override suspend fun getWallpapers(): List<Wallpaper> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.i(TAG, "WALLPAPER_MANIFEST_URL not set — returning empty catalog.")
            return@withContext emptyList()
        }
        val fresh = runCatching { fetchFromNetwork() }
        fresh
            .onSuccess { saveCache(it) }
            .onFailure { Log.w(TAG, "Network fetch failed, falling back to cache: ${it.message}") }
        val payload = fresh.getOrNull() ?: loadCache()
            ?: throw fresh.exceptionOrNull() ?: IllegalStateException("No manifest available")
        payload.wallpapers
    }

    private fun fetchFromNetwork(): WallpaperManifest {
        val client = httpClient.newBuilder()
            .callTimeout(NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(manifestUrl)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Manifest fetch ${response.code}: ${response.message}")
            }
            val body = response.body?.string()
                ?: error("Empty manifest response body")
            return json.decodeFromString(WallpaperManifest.serializer(), body)
        }
    }

    private fun loadCache(): WallpaperManifest? = runCatching {
        if (!cacheFile.exists()) return@runCatching null
        json.decodeFromString(WallpaperManifest.serializer(), cacheFile.readText())
    }.getOrNull()

    private fun saveCache(manifest: WallpaperManifest) {
        runCatching {
            cacheFile.writeText(json.encodeToString(WallpaperManifest.serializer(), manifest))
        }.onFailure { Log.w(TAG, "Cache write failed: ${it.message}") }
    }

    private companion object {
        val DefaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
