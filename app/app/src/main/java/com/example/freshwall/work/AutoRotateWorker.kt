package com.example.freshwall.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.AutoRotateSource
import com.example.freshwall.data.FeaturedRepository

class AutoRotateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as FreshWallApplication
        val config = app.autoRotatePreferences.config.value
        if (!config.enabled) return Result.success()

        val candidates = when (config.source) {
            AutoRotateSource.FEATURED -> runCatching {
                FeaturedRepository(applicationContext).getWallpapers()
            }.getOrNull() ?: return Result.retry()

            // Favorites now contains the full Wallpaper objects (Featured AND
            // Pexels), so we can rotate among them directly without going
            // through FeaturedRepository.
            AutoRotateSource.FAVORITES -> app.favoritesManager.favorites.value
        }
        if (candidates.isEmpty()) return Result.success()

        val pick = candidates.random()
        val applied = app.wallpaperActions.setAsWallpaper(
            url = pick.fullUrl,
            target = config.target,
            crop = null,
        )
        return if (applied.isSuccess) Result.success() else Result.retry()
    }
}
