package com.example.freshwall.ui.pexels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.pexels.PexelsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

private const val PAGE_SIZE = 9

data class PexelsHomeUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val allLoaded: Boolean = false,
    val error: String? = null,
)

/**
 * Owns the "Pexels" home tab state. Lazy-loads `PAGE_SIZE` items on first tab
 * activation, then more pages on demand as the user scrolls near the end.
 */
class PexelsHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val repository: PexelsRepository =
        (application as FreshWallApplication).pexelsRepository

    private val _uiState = MutableStateFlow(PexelsHomeUiState())
    val uiState: StateFlow<PexelsHomeUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var initialised = false
    private val loadMoreMutex = Mutex()

    fun loadIfNeeded() {
        if (initialised || _uiState.value.isLoading) return
        initialised = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val outcome = repository.curated(page = 1, perPage = PAGE_SIZE)
            outcome
                .onSuccess { list ->
                    currentPage = 1
                    _uiState.update {
                        PexelsHomeUiState(
                            wallpapers = list,
                            isLoading = false,
                            allLoaded = list.size < PAGE_SIZE,
                        )
                    }
                    prefetchThumbnails(list)
                }
                .onFailure { e ->
                    initialised = false
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage(e),
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.allLoaded) return
        if (!initialised) return
        viewModelScope.launch {
            // tryLock drops the call if another loadMore is already in flight,
            // saving a redundant API request on top of the distinctBy safety.
            if (!loadMoreMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isLoadingMore = true) }
                val nextPage = currentPage + 1
                val outcome = repository.curated(page = nextPage, perPage = PAGE_SIZE)
                outcome
                    .onSuccess { list ->
                        if (list.isEmpty()) {
                            _uiState.update { it.copy(isLoadingMore = false, allLoaded = true) }
                        } else {
                            currentPage = nextPage
                            _uiState.update {
                                it.copy(
                                    wallpapers = (it.wallpapers + list).distinctBy { w -> w.id },
                                    isLoadingMore = false,
                                    allLoaded = list.size < PAGE_SIZE,
                                )
                            }
                            prefetchThumbnails(list)
                        }
                    }
                    .onFailure {
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
            } finally {
                loadMoreMutex.unlock()
            }
        }
    }

    private fun errorMessage(e: Throwable): String =
        if (!repository.isConfigured) "Add your Pexels API key to local.properties to load this tab."
        else e.message ?: "Couldn't load Pexels."

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
}
