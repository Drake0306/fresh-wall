package com.example.freshwall.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.SearchHistoryManager
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.pexels.PexelsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal const val SEARCH_PAGE_SIZE = 9

data class PexelsSearchState(
    val results: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val allLoaded: Boolean = false,
    val error: String? = null,
)

/**
 * Owns the SearchScreen's persistent state. Because this is an
 * `AndroidViewModel` scoped to the Activity, every piece of state on it
 * survives navigating to Detail and back — that's what stops the screen
 * from looking "two steps back" when the user returns.
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val pexels: PexelsRepository =
        (application as FreshWallApplication).pexelsRepository
    private val historyManager: SearchHistoryManager =
        (application as FreshWallApplication).searchHistoryManager

    private val _searchPexels = MutableStateFlow(false)
    val searchPexels: StateFlow<Boolean> = _searchPexels.asStateFlow()

    private val _submittedQuery = MutableStateFlow("")
    val submittedQuery: StateFlow<String> = _submittedQuery.asStateFlow()

    private val _pexelsState = MutableStateFlow(PexelsSearchState())
    val pexelsState: StateFlow<PexelsSearchState> = _pexelsState.asStateFlow()

    private var currentPage = 0
    private var activeJob: Job? = null
    private val loadMoreMutex = Mutex()

    fun setSearchPexels(enabled: Boolean) {
        if (_searchPexels.value == enabled) return
        _searchPexels.value = enabled
        // If toggle is flipped on with an existing query, re-run as Pexels.
        if (enabled && _submittedQuery.value.isNotEmpty()) {
            executePexelsSearch(_submittedQuery.value)
        }
        if (!enabled) {
            // Clear Pexels state so toggling off doesn't keep stale results.
            _pexelsState.value = PexelsSearchState()
            currentPage = 0
            activeJob?.cancel()
        }
    }

    fun submit(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        _submittedQuery.value = trimmed
        historyManager.record(trimmed)
        if (_searchPexels.value) executePexelsSearch(trimmed)
    }

    fun loadMore() {
        val state = _pexelsState.value
        if (state.isLoading || state.isLoadingMore || state.allLoaded) return
        if (_submittedQuery.value.isEmpty() || !_searchPexels.value) return
        viewModelScope.launch {
            // tryLock = drop the call entirely if another loadMore is in flight.
            // distinctBy on the append already covers any duplicate-key crash,
            // but this saves us a redundant network round-trip.
            if (!loadMoreMutex.tryLock()) return@launch
            try {
                _pexelsState.value = state.copy(isLoadingMore = true)
                val nextPage = currentPage + 1
                val outcome = pexels.search(
                    _submittedQuery.value, page = nextPage, perPage = SEARCH_PAGE_SIZE,
                )
                outcome
                    .onSuccess { list ->
                        val current = _pexelsState.value
                        if (list.isEmpty()) {
                            _pexelsState.value = current.copy(
                                isLoadingMore = false, allLoaded = true,
                            )
                        } else {
                            currentPage = nextPage
                            _pexelsState.value = current.copy(
                                results = (current.results + list).distinctBy { it.id },
                                isLoadingMore = false,
                                allLoaded = list.size < SEARCH_PAGE_SIZE,
                            )
                        }
                    }
                    .onFailure {
                        _pexelsState.value = _pexelsState.value.copy(isLoadingMore = false)
                    }
            } finally {
                loadMoreMutex.unlock()
            }
        }
    }

    private fun executePexelsSearch(query: String) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _pexelsState.value = PexelsSearchState(isLoading = true)
            currentPage = 0
            val outcome = pexels.search(query, page = 1, perPage = SEARCH_PAGE_SIZE)
            outcome
                .onSuccess { list ->
                    val deduped = list.distinctBy { it.id }
                    _pexelsState.value = PexelsSearchState(
                        results = deduped,
                        isLoading = false,
                        allLoaded = list.size < SEARCH_PAGE_SIZE,
                    )
                    currentPage = 1
                }
                .onFailure { e ->
                    _pexelsState.value = PexelsSearchState(
                        isLoading = false,
                        error = if (!pexels.isConfigured) {
                            "Pexels API key not configured (set pexels.api.key in local.properties)"
                        } else {
                            e.message ?: "Pexels search failed"
                        },
                    )
                }
        }
    }
}
