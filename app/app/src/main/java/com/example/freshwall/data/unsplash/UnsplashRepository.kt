package com.example.freshwall.data.unsplash

import com.example.freshwall.BuildConfig
import com.example.freshwall.data.Wallpaper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class UnsplashRepository(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = BuildConfig.UNSPLASH_API_KEY.isNotBlank()

    /**
     * Search Unsplash. Mirrors [com.example.freshwall.data.pexels.PexelsRepository.search]
     * — same Result shape, same pagination contract, same 9-items-per-page
     * default so the LazyVerticalGrid renders identically for either source.
     */
    suspend fun search(
        query: String,
        page: Int = 1,
        perPage: Int = DEFAULT_PER_PAGE,
    ): Result<List<Wallpaper>> = withContext(Dispatchers.IO) {
        cancellationAwareCatch {
            check(isConfigured) { CONFIG_ERROR }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$BASE_URL/search/photos?query=$encoded&page=$page&per_page=$perPage&orientation=portrait")
                .header("Authorization", "Client-ID ${BuildConfig.UNSPLASH_API_KEY}")
                .header("Accept-Version", "v1")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error(httpErrorMessage(response.code))
                val body = response.body?.string().orEmpty()
                json.decodeFromString<UnsplashSearchResponse>(body)
                    .results.map { it.toWallpaper() }
            }
        }
    }

    /**
     * "Default browse" feed — analogous to Pexels' curated endpoint.
     * Uses `/photos?order_by=popular` so the home tab opens to something
     * eye-catching rather than the most recent submission.
     */
    suspend fun popular(
        page: Int = 1,
        perPage: Int = DEFAULT_PER_PAGE,
    ): Result<List<Wallpaper>> = withContext(Dispatchers.IO) {
        cancellationAwareCatch {
            check(isConfigured) { CONFIG_ERROR }
            val request = Request.Builder()
                .url("$BASE_URL/photos?page=$page&per_page=$perPage&order_by=popular")
                .header("Authorization", "Client-ID ${BuildConfig.UNSPLASH_API_KEY}")
                .header("Accept-Version", "v1")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error(httpErrorMessage(response.code))
                val body = response.body?.string().orEmpty()
                // The /photos endpoint returns a bare JSON array, not a
                // wrapping object — different from /search/photos.
                json.decodeFromString<List<UnsplashPhoto>>(body)
                    .map { it.toWallpaper() }
            }
        }
    }

    private inline fun <T> cancellationAwareCatch(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    private fun httpErrorMessage(code: Int): String = when (code) {
        429 -> "Unsplash rate limit reached. Try again in an hour."
        401, 403 -> "Unsplash API key was rejected. Check unsplash.api.key in local.properties."
        in 500..599 -> "Unsplash is having trouble right now. Try again later."
        else -> "Unsplash request failed (HTTP $code)."
    }

    private companion object {
        const val BASE_URL = "https://api.unsplash.com"
        const val DEFAULT_PER_PAGE = 9
        const val CONFIG_ERROR = "Unsplash API key not configured"
    }
}
