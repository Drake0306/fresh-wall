package io.github.drake0306.freshwall.data.net

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Retries once on transient failures: 5xx server errors and IOException
 * (network drops mid-request). Deliberately does NOT retry on 4xx — those
 * are client errors that won't change with an immediate retry (especially
 * 429 Too Many Requests, which would just make rate-limiting worse).
 */
class RetryInterceptor(
    private val maxRetries: Int = 1,
    private val retryOnHttpCodes: Set<Int> = setOf(500, 502, 503, 504),
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempts = 0

        while (true) {
            attempts++
            val response: Response? = try {
                chain.proceed(request)
            } catch (e: IOException) {
                if (attempts > maxRetries) throw e
                null
            }

            if (response != null) {
                if (response.code in retryOnHttpCodes && attempts <= maxRetries) {
                    response.close()
                    // fall through to backoff + retry
                } else {
                    return response
                }
            }

            try {
                Thread.sleep(BASE_BACKOFF_MS * attempts)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Retry interrupted", e)
            }
        }
    }

    private companion object {
        const val BASE_BACKOFF_MS = 500L
    }
}
