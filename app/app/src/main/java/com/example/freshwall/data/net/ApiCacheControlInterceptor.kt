package com.example.freshwall.data.net

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites the `Cache-Control` header on Pexels / Unsplash JSON responses so
 * OkHttp's disk cache can actually store and replay them. Neither API sends
 * a `Cache-Control: max-age=*` header itself, which means without this hook
 * every (query, page) hit goes to the network — quickly burning through the
 * per-key hourly quotas as the user base grows.
 *
 * Scope:
 *   - Only the **JSON API hosts** (`api.pexels.com`, `api.unsplash.com`) get
 *     rewritten. Image CDN responses on `images.*` are left alone — Coil
 *     owns image caching, and we don't want to double-cache binaries.
 *   - The Unsplash `download_location` endpoint (`/photos/{id}/download`)
 *     is excluded too: that's a side-effecting tracking ping and MUST hit
 *     the network on every successful apply / save. Callers that fire it
 *     also pass `Cache-Control: no-store` on the request side, but this
 *     guard is a belt for that suspender.
 *
 * Install as a **network interceptor** (not application) so the rewrite
 * happens before OkHttp's cache decides whether the response is storable.
 */
class ApiCacheControlInterceptor(
    private val maxAgeSeconds: Long = DEFAULT_MAX_AGE_SECONDS,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        // Never inject max-age on error responses. Caching a 429 (rate-limit)
        // would lock the user out for the full max-age window — well past the
        // hour Pexels/Unsplash actually rate-limit for. Same logic for 401/403
        // (bad key) and 5xx (server outages): the right behaviour is to
        // re-try on the next user action, not serve a stale failure.
        if (!response.isSuccessful) return response

        val host = chain.request().url.host
        val path = chain.request().url.encodedPath

        val cacheable = when (host) {
            "api.pexels.com" -> true
            "api.unsplash.com" -> !path.endsWith("/download")
            else -> false
        }
        if (!cacheable) return response

        return response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", "public, max-age=$maxAgeSeconds")
            .build()
    }

    private companion object {
        // 1 hour. Wallpaper grids don't shift moment-to-moment, so an hour
        // of cache life is enough to absorb cold-starts and tab toggles
        // without making the feed feel stale.
        const val DEFAULT_MAX_AGE_SECONDS = 60L * 60L
    }
}
