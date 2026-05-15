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
)

/**
 * Owns the "Pexels" home tab state. The feed rotates through every category
 * the user picked in onboarding so the whole selection gets airtime rather
 * than a single random pick driving the entire session.
 *
 * Each page request advances the rotation by one slot and pulls the next
 * unseen page of that slot's category — so page 1 might be `mountains`,
 * page 2 `sunset`, page 3 `forest`, and once the rotation wraps, page 4 is
 * `mountains` page 2, etc. Pull-to-refresh reshuffles the rotation and
 * resets per-category cursors.
 */
class PexelsHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val repository: PexelsRepository =
        (application as FreshWallApplication).pexelsRepository
    private val categoryPreferences =
        (application as FreshWallApplication).categoryPreferences

    private val _uiState = MutableStateFlow(PexelsHomeUiState())
    val uiState: StateFlow<PexelsHomeUiState> = _uiState.asStateFlow()

    private var initialised = false
    private val loadMoreMutex = Mutex()

    // Round-robin state for the user's selected categories.
    private var categoryRotation: List<String> = emptyList()
    private var rotationIndex: Int = 0
    /** Last page successfully fetched for each rotation category. */
    private val categoryPages: MutableMap<String, Int> = mutableMapOf()
    /** Categories that returned a short page — no more content to pull. */
    private val exhaustedCategories: MutableSet<String> = mutableSetOf()
    /** Fallback cursor used when the user has no Pexels categories selected. */
    private var curatedPage: Int = 0

    private fun resetRotation() {
        val config = categoryPreferences.config.value
        val active = config.pexelsActive()
        val starred = config.pexelsStarredActive()
        // Weighted rotation: starred categories appear STARRED_WEIGHT times in
        // the rotation list, unstarred appear once. With a 3:1 ratio and the
        // 3-star cap, a fully-starred user sees ~70%+ of pages drawn from
        // their top picks while the rest of the selection still shows up.
        categoryRotation = active.flatMap { category ->
            val weight = if (category in starred) STARRED_WEIGHT else 1
            List(weight) { category }
        }.shuffled()
        rotationIndex = 0
        categoryPages.clear()
        exhaustedCategories.clear()
        curatedPage = 0
    }

    private companion object {
        const val STARRED_WEIGHT = 3
    }

    /**
     * Picks the next category in the rotation and pulls its next unseen page.
     * Falls back to Pexels' curated browse when the user has no categories.
     * Returns `Result.success(emptyList())` once every selected category is
     * exhausted — the caller treats that as `allLoaded`.
     */
    private suspend fun fetchNext(forceFresh: Boolean = false): Result<List<Wallpaper>> {
        if (categoryRotation.isEmpty()) {
            val page = curatedPage + 1
            return repository.curated(
                page = page, perPage = PAGE_SIZE, forceFresh = forceFresh,
            ).also { result ->
                result.onSuccess { list -> if (list.isNotEmpty()) curatedPage = page }
            }
        }

        val pool = categoryRotation
        val total = pool.size
        for (offset in 0 until total) {
            val idx = (rotationIndex + offset) % total
            val candidate = pool[idx]
            if (candidate in exhaustedCategories) continue

            // Advance the rotation eagerly so a transient failure on this
            // category doesn't pin the next loadMore() to the same slot.
            rotationIndex = (idx + 1) % total
            val nextPage = (categoryPages[candidate] ?: 0) + 1
            val outcome = repository.search(
                query = candidate,
                page = nextPage,
                perPage = PAGE_SIZE,
                forceFresh = forceFresh,
            )
            outcome.onSuccess { list ->
                if (list.isNotEmpty()) categoryPages[candidate] = nextPage
                if (list.size < PAGE_SIZE) exhaustedCategories.add(candidate)
            }
            return outcome
        }
        return Result.success(emptyList())
    }

    private fun isAllExhausted(): Boolean =
        categoryRotation.isNotEmpty() && categoryRotation.all { it in exhaustedCategories }

    fun loadIfNeeded() {
        if (initialised || _uiState.value.isLoading) return
        initialised = true
        resetRotation()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchNext(forceFresh = false)
                .onSuccess { list ->
                    _uiState.update {
                        PexelsHomeUiState(
                            wallpapers = list,
                            isLoading = false,
                            allLoaded = list.isEmpty() || isAllExhausted(),
                        )
                    }
                    prefetchThumbnails(list)
                }
                .onFailure { e ->
                    initialised = false
                    _uiState.update {
                        it.copy(isLoading = false, error = errorMessage(e))
                    }
                }
        }
    }

    /** Pull-to-refresh entry point — re-shuffles the rotation and re-fetches. */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            resetRotation()
            fetchNext(forceFresh = true)
                .onSuccess { list ->
                    _uiState.value = PexelsHomeUiState(
                        wallpapers = list,
                        isLoading = false,
                        isRefreshing = false,
                        allLoaded = list.isEmpty() || isAllExhausted(),
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
                fetchNext(forceFresh = false)
                    .onSuccess { list ->
                        if (list.isEmpty()) {
                            _uiState.update { it.copy(isLoadingMore = false, allLoaded = true) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    wallpapers = (it.wallpapers + list).distinctBy { w -> w.id },
                                    isLoadingMore = false,
                                    allLoaded = isAllExhausted(),
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
