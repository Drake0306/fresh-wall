package com.example.freshwall.ui.unsplash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.unsplash.UnsplashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// 24 fits a 3-col grid with 8 rows per page. Unsplash caps per_page at 30,
// so 24 leaves a small safety margin and cuts pagination chatter ~3x.
private const val PAGE_SIZE = 24

data class UnsplashHomeUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val allLoaded: Boolean = false,
    val error: String? = null,
    /** Which category from the user's selection is currently driving the
     *  feed. `null` means "no selection — fell back to popular browse". */
    val activeCategory: String? = null,
)

/**
 * Owns the "Unsplash" home tab state. Direct sibling of
 * [com.example.freshwall.ui.pexels.PexelsHomeViewModel] — same lazy-load
 * pattern, same Mutex de-dupe guard, same allLoaded / isLoadingMore /
 * error contract. Anything that worked on the Pexels grid (auto-load
 * snapshot flow, manual "Load more" button) works here unchanged.
 */
class UnsplashHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val repository: UnsplashRepository =
        (application as FreshWallApplication).unsplashRepository
    private val categoryPreferences =
        (application as FreshWallApplication).categoryPreferences

    private val _uiState = MutableStateFlow(UnsplashHomeUiState())
    val uiState: StateFlow<UnsplashHomeUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var initialised = false
    private val loadMoreMutex = Mutex()
    private var activeCategory: String? = null

    private fun chooseCategory(): String? =
        categoryPreferences.config.value.unsplashActive().randomOrNull()

    private suspend fun fetchPage(page: Int, forceFresh: Boolean = false): Result<List<Wallpaper>> {
        val category = activeCategory
        return if (category == null) {
            repository.popular(page = page, perPage = PAGE_SIZE, forceFresh = forceFresh)
        } else {
            repository.search(category, page = page, perPage = PAGE_SIZE, forceFresh = forceFresh)
        }
    }

    fun loadIfNeeded() {
        if (initialised || _uiState.value.isLoading) return
        initialised = true
        activeCategory = chooseCategory()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val outcome = fetchPage(page = 1)
            outcome
                .onSuccess { list ->
                    currentPage = 1
                    _uiState.update {
                        UnsplashHomeUiState(
                            wallpapers = list,
                            isLoading = false,
                            allLoaded = list.size < PAGE_SIZE,
                            activeCategory = activeCategory,
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

    /** Pull-to-refresh entry point — picks a fresh random category and re-fetches. */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            activeCategory = chooseCategory()
            currentPage = 0
            val outcome = fetchPage(page = 1, forceFresh = true)
            outcome
                .onSuccess { list ->
                    currentPage = 1
                    _uiState.value = UnsplashHomeUiState(
                        wallpapers = list,
                        isLoading = false,
                        isRefreshing = false,
                        allLoaded = list.size < PAGE_SIZE,
                        activeCategory = activeCategory,
                    )
                    prefetchThumbnails(list)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isRefreshing = false, error = errorMessage(e))
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.allLoaded) return
        if (!initialised) return
        viewModelScope.launch {
            if (!loadMoreMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isLoadingMore = true) }
                val nextPage = currentPage + 1
                val outcome = fetchPage(page = nextPage)
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
        if (!repository.isConfigured) "Add your Unsplash API key to local.properties to load this tab."
        else e.message ?: "Couldn't load Unsplash."

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
