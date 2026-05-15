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

// 24 fits a 3-col grid with 8 rows per page. Pexels caps per_page at 80
// so we still have headroom; bigger pages cut pagination calls ~3x for
// scroll-heavy users.
private const val PAGE_SIZE = 24

data class PexelsHomeUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** True while a pull-to-refresh is in flight. Distinct from [isLoading]
     *  (initial load) so the M3 indicator can stay visible without blocking
     *  the existing grid contents. */
    val isRefreshing: Boolean = false,
    val allLoaded: Boolean = false,
    val error: String? = null,
    /** Which category from the user's selection is currently driving the feed.
     *  `null` means "no selection — fell back to curated browse". */
    val activeCategory: String? = null,
)

/**
 * Owns the "Pexels" home tab state. Lazy-loads `PAGE_SIZE` items on first tab
 * activation, then more pages on demand as the user scrolls near the end.
 */
class PexelsHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val repository: PexelsRepository =
        (application as FreshWallApplication).pexelsRepository
    private val categoryPreferences =
        (application as FreshWallApplication).categoryPreferences

    private val _uiState = MutableStateFlow(PexelsHomeUiState())
    val uiState: StateFlow<PexelsHomeUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var initialised = false
    private val loadMoreMutex = Mutex()
    /** Category we're currently paging through (null = fell back to /curated). */
    private var activeCategory: String? = null

    /** Picks one of the user's Pexels categories at random. Returns `null`
     *  if the user has none selected — in that case the feed falls back to
     *  Pexels' curated endpoint so the tab still shows something usable. */
    private fun chooseCategory(): String? {
        val pool = categoryPreferences.config.value.pexelsActive()
        return pool.randomOrNull()
    }

    private suspend fun fetchPage(page: Int, forceFresh: Boolean = false): Result<List<Wallpaper>> {
        val category = activeCategory
        return if (category == null) {
            repository.curated(page = page, perPage = PAGE_SIZE, forceFresh = forceFresh)
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
                        PexelsHomeUiState(
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

    /** Pull-to-refresh entry point — picks a fresh random category from the
     *  user's selection and re-fetches page 1. */
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
                    _uiState.value = PexelsHomeUiState(
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
            // tryLock drops the call if another loadMore is already in flight,
            // saving a redundant API request on top of the distinctBy safety.
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
