package com.example.freshwall

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.freshwall.actions.WallpaperActions
import com.example.freshwall.ads.RewardedAdManager
import com.example.freshwall.data.AutoRotatePreferences
import com.example.freshwall.data.CategoryPreferences
import com.example.freshwall.data.FavoritesManager
import com.example.freshwall.data.SearchHistoryManager
import com.example.freshwall.data.SourcePreferences
import com.example.freshwall.data.ThemePreferences
import com.example.freshwall.data.feedback.FeedbackRepository
import com.example.freshwall.data.manifest.RemoteWallpaperRepository
import com.example.freshwall.data.net.RetryInterceptor
import com.example.freshwall.data.pexels.PexelsRepository
import com.example.freshwall.data.unsplash.UnsplashRepository
import com.example.freshwall.work.AutoRotateScheduler
import com.google.android.gms.ads.MobileAds
import okhttp3.OkHttpClient

class FreshWallApplication : Application(), ImageLoaderFactory {

    val rewardedAdManager: RewardedAdManager by lazy { RewardedAdManager(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    val favoritesManager: FavoritesManager by lazy { FavoritesManager(this) }
    val autoRotatePreferences: AutoRotatePreferences by lazy { AutoRotatePreferences(this) }
    val wallpaperActions: WallpaperActions by lazy { WallpaperActions(this) }
    val searchHistoryManager: SearchHistoryManager by lazy { SearchHistoryManager(this) }
    val pexelsRepository: PexelsRepository by lazy { PexelsRepository(httpClient = sharedHttpClient) }
    val unsplashRepository: UnsplashRepository by lazy { UnsplashRepository(httpClient = sharedHttpClient) }
    val feedbackRepository: FeedbackRepository by lazy { FeedbackRepository(this) }
    val sourcePreferences: SourcePreferences by lazy { SourcePreferences(this) }
    val categoryPreferences: CategoryPreferences by lazy { CategoryPreferences(this) }
    val wallpaperRepository: RemoteWallpaperRepository by lazy {
        RemoteWallpaperRepository(this, sharedHttpClient)
    }

    // Single OkHttpClient shared by Coil + PexelsRepository so the retry
    // interceptor covers BOTH image loads and JSON API calls in one place.
    private val sharedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
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
