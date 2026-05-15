package io.github.drake0306.freshwall

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import io.github.drake0306.freshwall.actions.WallpaperActions
import io.github.drake0306.freshwall.ads.RewardedAdManager
import io.github.drake0306.freshwall.data.AutoRotatePreferences
import io.github.drake0306.freshwall.data.CategoryPreferences
import io.github.drake0306.freshwall.data.FavoritesManager
import io.github.drake0306.freshwall.data.SearchHistoryManager
import io.github.drake0306.freshwall.data.SourcePreferences
import io.github.drake0306.freshwall.data.ThemePreferences
import io.github.drake0306.freshwall.data.feedback.FeedbackRepository
import io.github.drake0306.freshwall.data.manifest.RemoteWallpaperRepository
import io.github.drake0306.freshwall.data.net.ApiCacheControlInterceptor
import io.github.drake0306.freshwall.data.net.RetryInterceptor
import io.github.drake0306.freshwall.data.pexels.PexelsRepository
import io.github.drake0306.freshwall.data.unsplash.UnsplashRepository
import io.github.drake0306.freshwall.work.AutoRotateScheduler
import com.google.android.gms.ads.MobileAds
import okhttp3.Cache
import okhttp3.OkHttpClient

class FreshWallApplication : Application(), ImageLoaderFactory {

    val rewardedAdManager: RewardedAdManager by lazy { RewardedAdManager(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    val favoritesManager: FavoritesManager by lazy { FavoritesManager(this) }
    val autoRotatePreferences: AutoRotatePreferences by lazy { AutoRotatePreferences(this) }
    val searchHistoryManager: SearchHistoryManager by lazy { SearchHistoryManager(this) }
    val pexelsRepository: PexelsRepository by lazy { PexelsRepository(httpClient = sharedHttpClient) }
    val unsplashRepository: UnsplashRepository by lazy { UnsplashRepository(httpClient = sharedHttpClient) }
    val wallpaperActions: WallpaperActions by lazy {
        WallpaperActions(this, unsplashRepository)
    }
    val feedbackRepository: FeedbackRepository by lazy { FeedbackRepository(this) }
    val sourcePreferences: SourcePreferences by lazy { SourcePreferences(this) }
    val categoryPreferences: CategoryPreferences by lazy { CategoryPreferences(this) }
    val wallpaperRepository: RemoteWallpaperRepository by lazy {
        RemoteWallpaperRepository(this, sharedHttpClient)
    }

    // Single OkHttpClient shared by Coil + the API repositories. The disk
    // cache + ApiCacheControlInterceptor mean repeated grid loads (cold
    // starts, tab toggles) are served from disk for an hour instead of
    // re-hitting the per-key Pexels / Unsplash hourly quotas. The cache is
    // scoped to api.* hosts — image CDN traffic still flows through Coil's
    // own DiskCache.
    private val sharedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .addNetworkInterceptor(ApiCacheControlInterceptor())
            .cache(
                Cache(
                    directory = cacheDir.resolve("http_api_cache"),
                    maxSize = 50L * 1024 * 1024,
                ),
            )
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // Touch persisted prefs early so the disk reads happen before first compose.
        themePreferences.themeMode
        autoRotatePreferences.config
        sourcePreferences.config
        categoryPreferences.config
        // Re-arm any auto-rotate schedule that was on before the app was killed/rebooted.
        AutoRotateScheduler.applyCurrent(this)
        MobileAds.initialize(this) {
            rewardedAdManager.loadAd()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(sharedHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .build()
    }
}
