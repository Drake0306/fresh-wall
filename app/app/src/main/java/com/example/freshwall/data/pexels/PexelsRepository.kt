package com.example.freshwall.data.pexels

import com.example.freshwall.BuildConfig
import com.example.freshwall.data.Wallpaper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class PexelsRepository(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = BuildConfig.PEXELS_API_KEY.isNotBlank()

    suspend fun search(
        query: String,
        page: Int = 1,
        perPage: Int = DEFAULT_PER_PAGE,
        forceFresh: Boolean = false,
    ): Result<List<Wallpaper>> = withContext(Dispatchers.IO) {
        cancellationAwareCatch {
            check(isConfigured) { CONFIG_ERROR }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$BASE_URL/search?query=$encoded&page=$page&per_page=$perPage&orientation=portrait")
                .header("Authorization", BuildConfig.PEXELS_API_KEY)
                .apply { if (forceFresh) cacheControl(CacheControl.FORCE_NETWORK) }
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error(httpErrorMessage(response.code))
                val body = response.body?.string().orEmpty()
                json.decodeFromString<PexelsSearchResponse>(body).photos.map { it.toWallpaper() }
            }
        }
    }

    suspend fun curated(
        page: Int = 1,
        perPage: Int = DEFAULT_PER_PAGE,
        forceFresh: Boolean = false,
    ): Result<List<Wallpaper>> = withContext(Dispatchers.IO) {
        cancellationAwareCatch {
            check(isConfigured) { CONFIG_ERROR }
            val request = Request.Builder()
                .url("$BASE_URL/curated?page=$page&per_page=$perPage")
                .header("Authorization", BuildConfig.PEXELS_API_KEY)
                .apply { if (forceFresh) cacheControl(CacheControl.FORCE_NETWORK) }
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error(httpErrorMessage(response.code))
                val body = response.body?.string().orEmpty()
                json.decodeFromString<PexelsCuratedResponse>(body).photos.map { it.toWallpaper() }
            }
        }
    }

    /**
     * Wraps [block] in a try/catch that lets [CancellationException] propagate
     * (so coroutine cancellation works correctly) while turning every other
     * throwable into a `Result.failure`. Replaces a plain `runCatching {}`,
     * which would otherwise swallow cancellation and leave callers in a stuck
     * "loading" state.
     */
    private inline fun <T> cancellationAwareCatch(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    private fun httpErrorMessage(code: Int): String = when (code) {
        429 -> "Pexels rate limit reached. Try again in a few minutes."
        401, 403 -> "Pexels API key was rejected. Check pexels.api.key in local.properties."
        in 500..599 -> "Pexels is having trouble right now. Try again later."
        else -> "Pexels request failed (HTTP $code)."
    }

    private companion object {
        const val BASE_URL = "https://api.pexels.com/v1"
        const val DEFAULT_PER_PAGE = 9
        const val CONFIG_ERROR = "Pexels API key not configured"
    }
}
