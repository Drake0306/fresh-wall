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
)

/**
 * Owns the "Unsplash" home tab state. Direct sibling of
 * [com.example.freshwall.ui.pexels.PexelsHomeViewModel] — same round-robin
 * rotation over the user's selected categories, same Mutex de-dupe, same
 * allLoaded / isLoadingMore / error contract. Falls back to Unsplash's
 * `popular` browse when the user has no Unsplash categories selected.
 */
class UnsplashHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val repository: UnsplashRepository =
        (application as FreshWallApplication).unsplashRepository
    private val categoryPreferences =
        (application as FreshWallApplication).categoryPreferences

    private val _uiState = MutableStateFlow(UnsplashHomeUiState())
    val uiState: StateFlow<UnsplashHomeUiState> = _uiState.asStateFlow()

    private var initialised = false
    private val loadMoreMutex = Mutex()

    private var categoryRotation: List<String> = emptyList()
    private var rotationIndex: Int = 0
    private val categoryPages: MutableMap<String, Int> = mutableMapOf()
    private val exhaustedCategories: MutableSet<String> = mutableSetOf()
    private var popularPage: Int = 0

    private fun resetRotation() {
        val config = categoryPreferences.config.value
        val active = config.unsplashActive()
        val starred = config.unsplashStarredActive()
        // Weighted rotation: starred categories appear STARRED_WEIGHT times.
        // See PexelsHomeViewModel.STARRED_WEIGHT for the reasoning.
        categoryRotation = active.flatMap { category ->
            val weight = if (category in starred) STARRED_WEIGHT else 1
            List(weight) { category }
        }.shuffled()
        rotationIndex = 0
        categoryPages.clear()
        exhaustedCategories.clear()
        popularPage = 0
    }

    private companion object {
        const val STARRED_WEIGHT = 3
    }

    private suspend fun fetchNext(forceFresh: Boolean = false): Result<List<Wallpaper>> {
        if (categoryRotation.isEmpty()) {
            val page = popularPage + 1
            return repository.popular(
                page = page, perPage = PAGE_SIZE, forceFresh = forceFresh,
            ).also { result ->
                result.onSuccess { list -> if (list.isNotEmpty()) popularPage = page }
            }
        }

        val pool = categoryRotation
        val total = pool.size
        for (offset in 0 until total) {
            val idx = (rotationIndex + offset) % total
            val candidate = pool[idx]
            if (candidate in exhaustedCategories) continue

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
                        UnsplashHomeUiState(
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

    fun refresh() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            resetRotation()
            fetchNext(forceFresh = true)
                .onSuccess { list ->
                    _uiState.value = UnsplashHomeUiState(
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
