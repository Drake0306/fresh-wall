package io.github.drake0306.freshwall.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.drake0306.freshwall.FreshWallApplication
import io.github.drake0306.freshwall.data.AutoRotateSource

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
                app.wallpaperRepository.getWallpapers()
            }.getOrNull() ?: return Result.retry()

            // Favorites stores full Wallpaper objects (Featured AND Pexels),
            // so we can rotate them directly without hitting the network.
            AutoRotateSource.FAVORITES -> app.favoritesManager.favorites.value
        }
        if (candidates.isEmpty()) return Result.success()

        val pick = candidates.random()
        val applied = app.wallpaperActions.setAsWallpaper(
            wallpaper = pick,
            target = config.target,
            crop = null,
        )
        return if (applied.isSuccess) Result.success() else Result.retry()
    }
}
