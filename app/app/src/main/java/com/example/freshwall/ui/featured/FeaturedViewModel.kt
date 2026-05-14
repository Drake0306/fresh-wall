package com.example.freshwall.ui.featured

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.WallpaperRepository
import com.example.freshwall.data.manifest.RemoteWallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeaturedUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val heroes: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /**
     * `false` when the build has no manifest URL configured (early-dev,
     * forks, CI). The UI uses this to show a "setup pending" empty state
     * rather than a generic error message.
     */
    val unconfigured: Boolean = false,
)

class FeaturedViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val remote: RemoteWallpaperRepository =
        (application as FreshWallApplication).wallpaperRepository
    private val repository: WallpaperRepository = remote

    private val _uiState = MutableStateFlow(FeaturedUiState())
    val uiState: StateFlow<FeaturedUiState> = _uiState.asStateFlow()

    init { load(isRefresh = false) }

    /** Pull-to-refresh hook. Repeats the manifest fetch + image prefetch. */
    fun refresh() = load(isRefresh = true)

    private fun load(isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    error = null,
                )
            }
            if (!remote.isConfigured) {
                _uiState.update {
                    FeaturedUiState(
                        isLoading = false,
                        isRefreshing = false,
                        unconfigured = true,
                    )
                }
                return@launch
            }
            runCatching { repository.getWallpapers() }
                .onSuccess { list ->
                    val heroCount = HERO_COUNT.coerceAtMost(list.size)
                    _uiState.update {
                        FeaturedUiState(
                            heroes = list.take(heroCount),
                            wallpapers = list.drop(heroCount),
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
                    prefetchThumbnails(list)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message ?: "Couldn't load wallpapers.",
                        )
                    }
                }
        }
    }

    private fun prefetchThumbnails(list: List<Wallpaper>) {
        val loader = appContext.imageLoader
        list.forEach { w ->
            loader.enqueue(
                ImageRequest.Builder(appContext)
                    .data(w.thumbnailUrl)
                    .build()
            )
        }
    }

    private companion object {
        const val HERO_COUNT = 3
    }
}
