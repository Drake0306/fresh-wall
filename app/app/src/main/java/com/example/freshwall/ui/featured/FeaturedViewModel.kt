package com.example.freshwall.ui.featured

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.freshwall.data.FeaturedRepository
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeaturedUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val heroes: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class FeaturedViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val repository: WallpaperRepository = FeaturedRepository(application)

    private val _uiState = MutableStateFlow(FeaturedUiState())
    val uiState: StateFlow<FeaturedUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getWallpapers() }
                .onSuccess { list ->
                    val heroCount = HERO_COUNT.coerceAtMost(list.size)
                    _uiState.update {
                        FeaturedUiState(
                            heroes = list.take(heroCount),
                            wallpapers = list.drop(heroCount),
                            isLoading = false,
                        )
                    }
                    prefetchThumbnails(list)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Unknown error")
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
