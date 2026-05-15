package io.github.drake0306.freshwall.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.drake0306.freshwall.FreshWallApplication
import io.github.drake0306.freshwall.data.SearchHistoryManager
import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.data.WallpaperSource
import io.github.drake0306.freshwall.data.pexels.PexelsRepository
import io.github.drake0306.freshwall.data.unsplash.UnsplashRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// Matches the home tabs' PAGE_SIZE — 24 per page. Unsplash caps per_page
// at 30, so this stays within both APIs' limits.
internal const val SEARCH_PAGE_SIZE = 24

/**
 * Per-source result state (Pexels and Unsplash both use this shape). The
 * Featured/local-collection "search" is handled in the screen by simply
 * filtering already-loaded wallpapers — no network state needed there.
 */
data class RemoteSearchState(
    val results: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val allLoaded: Boolean = false,
    val error: String? = null,
)

/**
 * Owns the SearchScreen's persistent state. Survives navigating to Detail
 * and back (Activity-scoped AndroidViewModel) so the user doesn't see the
 * "two steps back" empty state on return.
 *
 * [searchSource] picks which backend the query runs against:
 *   - [WallpaperSource.FEATURED] — client-side substring filter on the
 *     already-loaded Featured catalog. No network call.
 *   - [WallpaperSource.PEXELS] — Pexels search API, paged.
 *   - [WallpaperSource.UNSPLASH] — Unsplash search API, paged.
 *
 * Pexels and Unsplash each keep their own [RemoteSearchState] so switching
 * back to a previously-used source doesn't lose its results.
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val pexels: PexelsRepository =
        (application as FreshWallApplication).pexelsRepository
    private val unsplash: UnsplashRepository =
        (application as FreshWallApplication).unsplashRepository
    private val historyManager: SearchHistoryManager =
        (application as FreshWallApplication).searchHistoryManager

    private val _searchSource = MutableStateFlow(WallpaperSource.PEXELS)
    val searchSource: StateFlow<WallpaperSource> = _searchSource.asStateFlow()

    private val _submittedQuery = MutableStateFlow("")
    val submittedQuery: StateFlow<String> = _submittedQuery.asStateFlow()

    private val _pexelsState = MutableStateFlow(RemoteSearchState())
    val pexelsState: StateFlow<RemoteSearchState> = _pexelsState.asStateFlow()

    private val _unsplashState = MutableStateFlow(RemoteSearchState())
    val unsplashState: StateFlow<RemoteSearchState> = _unsplashState.asStateFlow()

    // Page cursors per source. Indexed by WallpaperSource so a switch back
    // doesn't reset the other source's pagination.
    private val pageCursors = mutableMapOf(
        WallpaperSource.PEXELS to 0,
        WallpaperSource.UNSPLASH to 0,
    )
    private var activeJob: Job? = null
    private val loadMoreMutex = Mutex()

    fun setSearchSource(source: WallpaperSource) {
        if (_searchSource.value == source) return
        _searchSource.value = source
        // If there's an active query, re-run it against the new source.
        val q = _submittedQuery.value
        if (q.isNotEmpty() && source != WallpaperSource.FEATURED) {
            executeRemoteSearch(source, q)
        }
    }

    fun submit(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _submittedQuery.value = trimmed
        historyManager.record(trimmed)
        when (_searchSource.value) {
            WallpaperSource.PEXELS -> executeRemoteSearch(WallpaperSource.PEXELS, trimmed)
            WallpaperSource.UNSPLASH -> executeRemoteSearch(WallpaperSource.UNSPLASH, trimmed)
            WallpaperSource.FEATURED -> Unit // local filter, handled in screen
        }
    }

    fun loadMore() {
        val source = _searchSource.value
        if (source == WallpaperSource.FEATURED) return
        val state = currentRemoteState(source)
        if (state.isLoading || state.isLoadingMore || state.allLoaded) return
        if (_submittedQuery.value.isEmpty()) return
        viewModelScope.launch {
            if (!loadMoreMutex.tryLock()) return@launch
            try {
                updateRemoteState(source) { it.copy(isLoadingMore = true) }
                val nextPage = (pageCursors[source] ?: 0) + 1
                val outcome = searchRepo(source, _submittedQuery.value, nextPage)
                outcome
                    .onSuccess { list ->
                        val current = currentRemoteState(source)
                        if (list.isEmpty()) {
                            updateRemoteState(source) {
                                it.copy(isLoadingMore = false, allLoaded = true)
                            }
                        } else {
                            pageCursors[source] = nextPage
                            updateRemoteState(source) {
                                it.copy(
                                    results = (current.results + list).distinctBy { w -> w.id },
                                    isLoadingMore = false,
                                    allLoaded = list.size < SEARCH_PAGE_SIZE,
                                )
                            }
                        }
                    }
                    .onFailure {
                        updateRemoteState(source) { it.copy(isLoadingMore = false) }
                    }
            } finally {
                loadMoreMutex.unlock()
            }
        }
    }

    private fun executeRemoteSearch(source: WallpaperSource, query: String) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            updateRemoteState(source) { RemoteSearchState(isLoading = true) }
            pageCursors[source] = 0
            val outcome = searchRepo(source, query, page = 1)
            outcome
                .onSuccess { list ->
                    val deduped = list.distinctBy { it.id }
                    updateRemoteState(source) {
                        RemoteSearchState(
                            results = deduped,
                            isLoading = false,
                            allLoaded = list.size < SEARCH_PAGE_SIZE,
                        )
                    }
                    pageCursors[source] = 1
                }
                .onFailure { e ->
                    updateRemoteState(source) {
                        RemoteSearchState(
                            isLoading = false,
                            error = errorMessage(source, e),
                        )
                    }
                }
        }
    }

    private suspend fun searchRepo(
        source: WallpaperSource,
        query: String,
        page: Int,
    ): Result<List<Wallpaper>> = when (source) {
        WallpaperSource.PEXELS -> pexels.search(query, page = page, perPage = SEARCH_PAGE_SIZE)
        WallpaperSource.UNSPLASH -> unsplash.search(query, page = page, perPage = SEARCH_PAGE_SIZE)
        WallpaperSource.FEATURED -> Result.success(emptyList()) // unreachable in practice
    }

    private fun currentRemoteState(source: WallpaperSource): RemoteSearchState = when (source) {
        WallpaperSource.PEXELS -> _pexelsState.value
        WallpaperSource.UNSPLASH -> _unsplashState.value
        WallpaperSource.FEATURED -> RemoteSearchState()
    }

    private fun updateRemoteState(
        source: WallpaperSource,
        transform: (RemoteSearchState) -> RemoteSearchState,
    ) {
        when (source) {
            WallpaperSource.PEXELS -> _pexelsState.value = transform(_pexelsState.value)
            WallpaperSource.UNSPLASH -> _unsplashState.value = transform(_unsplashState.value)
            WallpaperSource.FEATURED -> Unit
        }
    }

    private fun errorMessage(source: WallpaperSource, e: Throwable): String = when (source) {
        WallpaperSource.PEXELS -> if (!pexels.isConfigured) {
            "Pexels API key not configured (set pexels.api.key in local.properties)"
        } else e.message ?: "Pexels search failed"
        WallpaperSource.UNSPLASH -> if (!unsplash.isConfigured) {
            "Unsplash API key not configured (set unsplash.api.key in local.properties)"
        } else e.message ?: "Unsplash search failed"
        WallpaperSource.FEATURED -> e.message ?: "Search failed"
    }
}
