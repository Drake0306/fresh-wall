package io.github.drake0306.freshwall.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.drake0306.freshwall.FreshWallApplication
import io.github.drake0306.freshwall.data.WALLPAPER_CATEGORIES
import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.data.WallpaperSource
import io.github.drake0306.freshwall.data.searchSuggestions
import io.github.drake0306.freshwall.ui.components.LoadingMoreIndicator
import io.github.drake0306.freshwall.ui.featured.FeaturedViewModel
import io.github.drake0306.freshwall.ui.home.WallpaperGridSkeleton
import io.github.drake0306.freshwall.ui.home.WallpaperTile
import io.github.drake0306.freshwall.util.rememberHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Custom Saver so the user's typed text + cursor position survive navigation. */
private val TextFieldValueSaver: Saver<TextFieldValue, Any> = listSaver(
    save = { listOf(it.text, it.selection.start, it.selection.end) },
    restore = { saved ->
        TextFieldValue(
            text = saved[0] as String,
            selection = TextRange(saved[1] as Int, saved[2] as Int),
        )
    },
)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    featuredViewModel: FeaturedViewModel,
    onClose: () -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FreshWallApplication }
    val featuredState by featuredViewModel.uiState.collectAsStateWithLifecycle()
    val history by app.searchHistoryManager.history.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    val searchViewModel: SearchViewModel = viewModel()
    val pexelsState by searchViewModel.pexelsState.collectAsStateWithLifecycle()
    val unsplashState by searchViewModel.unsplashState.collectAsStateWithLifecycle()
    val submittedQuery by searchViewModel.submittedQuery.collectAsStateWithLifecycle()
    val searchSource by searchViewModel.searchSource.collectAsStateWithLifecycle()
    val sourceConfig by app.sourcePreferences.config.collectAsStateWithLifecycle()

    // If the user disables a source in settings while it's the active
    // search source, snap back to a still-enabled one (prefer Pexels, then
    // Unsplash, then Featured). The SourcePreferences invariant guarantees
    // at least one of the three is enabled.
    LaunchedEffect(sourceConfig) {
        val active = searchSource
        val stillAvailable = when (active) {
            WallpaperSource.FEATURED -> sourceConfig.featured
            WallpaperSource.PEXELS -> sourceConfig.pexels
            WallpaperSource.UNSPLASH -> sourceConfig.unsplash
        }
        if (!stillAvailable) {
            val fallback = when {
                sourceConfig.pexels -> WallpaperSource.PEXELS
                sourceConfig.unsplash -> WallpaperSource.UNSPLASH
                else -> WallpaperSource.FEATURED
            }
            searchViewModel.setSearchSource(fallback)
        }
    }

    // The currently-active remote state (Pexels or Unsplash). Featured
    // search uses [localResults] instead and never hits this.
    val remoteState = when (searchSource) {
        WallpaperSource.PEXELS -> pexelsState
        WallpaperSource.UNSPLASH -> unsplashState
        else -> RemoteSearchState() // unused for Featured branch
    }
    val isRemoteSearch = searchSource != WallpaperSource.FEATURED

    var query by rememberSaveable(stateSaver = TextFieldValueSaver) {
        mutableStateOf(TextFieldValue(""))
    }

    // First time SearchScreen mounts after a back-nav, sync the field to
    // whatever was last submitted so the user sees their query in the box.
    LaunchedEffect(Unit) {
        if (query.text.isEmpty() && submittedQuery.isNotEmpty()) {
            query = TextFieldValue(submittedQuery, selection = TextRange(submittedQuery.length))
        }
    }

    val resultsGridState = rememberLazyStaggeredGridState()
    val haptics = rememberHaptics()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun submit() {
        val q = query.text.trim()
        if (q.isEmpty()) return
        searchViewModel.submit(q)
        keyboard?.hide()
    }

    // Trigger pagination when the results grid scrolls near the end. The
    // `total` is part of the snapshot value so that a new page's arrival
    // re-emits the flow — without it, distinctUntilChanged would swallow
    // the re-trigger when the user is already at the bottom.
    LaunchedEffect(resultsGridState, submittedQuery, searchSource) {
        if (!isRemoteSearch || submittedQuery.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val info = resultsGridState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to (total > 0 && last >= total - 3)
        }
            .distinctUntilChanged()
            .filter { (_, nearEnd) -> nearEnd }
            .collect { searchViewModel.loadMore() }
    }

    // Local-collection search filter (runs in the composable because it reads featuredViewModel).
    val localResults: List<Wallpaper> = remember(submittedQuery, featuredState.heroes, featuredState.wallpapers) {
        if (submittedQuery.isEmpty()) emptyList()
        else {
            val q = submittedQuery.lowercase()
            (featuredState.heroes + featuredState.wallpapers).filter { w ->
                w.name?.lowercase()?.contains(q) == true ||
                    w.description?.lowercase()?.contains(q) == true ||
                    w.author?.lowercase()?.contains(q) == true
            }
        }
    }

    val suggestions = remember(query.text, history) {
        searchSuggestions(query.text, history)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search wallpapers") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                )
            }

            // Source toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Search source",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = when (searchSource) {
                            WallpaperSource.PEXELS -> "Searches the Pexels library"
                            WallpaperSource.UNSPLASH -> "Searches the Unsplash library"
                            WallpaperSource.FEATURED -> "Searches your FreshWall collection"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Source picker — one chip per enabled source. Pexels comes
            // first (the default), Unsplash next, Featured last if the
            // user has it turned on in Settings.
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val sources = buildList {
                    if (sourceConfig.pexels) add(WallpaperSource.PEXELS to "Pexels")
                    if (sourceConfig.unsplash) add(WallpaperSource.UNSPLASH to "Unsplash")
                    if (sourceConfig.featured) add(WallpaperSource.FEATURED to "Featured")
                }
                items(sources, key = { it.first.name }) { (source, label) ->
                    FilterChip(
                        selected = searchSource == source,
                        onClick = {
                            haptics.tabSwitch()
                            searchViewModel.setSearchSource(source)
                        },
                        label = { Text(label) },
                    )
                }
            }

            // Category quick-picks — only when a remote source is active
            // (Featured search is a local filter, so the category-shortcut
            // row doesn't apply). Uses the full WALLPAPER_CATEGORIES list so
            // it matches what the user saw during onboarding.
            AnimatedVisibility(visible = isRemoteSearch) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(WALLPAPER_CATEGORIES, key = { it }) { category ->
                        val isSelected = submittedQuery.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptics.click()
                                val q = category.lowercase()
                                query = TextFieldValue(q, selection = TextRange(q.length))
                                searchViewModel.submit(q)
                                keyboard?.hide()
                            },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        )
                    }
                }
            }
            HorizontalDivider()

            when {
                query.text.isEmpty() -> RecentSearches(
                    history = history,
                    onTap = { q ->
                        query = TextFieldValue(q, selection = TextRange(q.length))
                        searchViewModel.submit(q)
                        keyboard?.hide()
                    },
                    onClear = { app.searchHistoryManager.clear() },
                )
                submittedQuery.isEmpty() || submittedQuery != query.text.trim() -> SuggestionsList(
                    suggestions = suggestions,
                    onTap = { s ->
                        query = TextFieldValue(s, selection = TextRange(s.length))
                        searchViewModel.submit(s)
                        keyboard?.hide()
                    },
                )
                else -> SearchResults(
                    isRemote = isRemoteSearch,
                    isLoading = isRemoteSearch && remoteState.isLoading,
                    isLoadingMore = isRemoteSearch && remoteState.isLoadingMore,
                    allLoaded = remoteState.allLoaded,
                    error = if (isRemoteSearch) remoteState.error else null,
                    results = if (isRemoteSearch) remoteState.results else localResults,
                    onWallpaperClick = onWallpaperClick,
                    onLoadMore = { searchViewModel.loadMore() },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    favoriteIds = app.favoritesManager.favorites.collectAsStateWithLifecycle()
                        .value.asSequence().map { it.id }.toHashSet(),
                    onFavoriteClick = { app.favoritesManager.toggle(it) },
                    gridState = resultsGridState,
                )
            }
        }
    }
}

@Composable
private fun RecentSearches(
    history: List<String>,
    onTap: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Recent searches will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "recent_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
        items(history, key = { it }) { q ->
            ListItem(
                headlineContent = { Text(q, style = MaterialTheme.typography.bodyLarge) },
                leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTap(q) },
            )
        }
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<String>,
    onTap: (String) -> Unit,
) {
    if (suggestions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Press search to look up wallpapers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(suggestions, key = { it }) { s ->
            ListItem(
                headlineContent = { Text(s, style = MaterialTheme.typography.bodyLarge) },
                leadingContent = { Icon(Icons.Outlined.NorthWest, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTap(s) },
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchResults(
    isRemote: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    allLoaded: Boolean,
    error: String?,
    results: List<Wallpaper>,
    favoriteIds: Set<String>,
    onFavoriteClick: (Wallpaper) -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    onLoadMore: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: LazyStaggeredGridState,
) {
    when {
        isLoading -> WallpaperGridSkeleton(
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 24.dp),
        )
        error != null -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        results.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isRemote) "No results" else "Nothing in your library matches that",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalItemSpacing = 6.dp,
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(results, key = { it.id }) { w ->
                WallpaperTile(
                    wallpaper = w,
                    isFavorite = w.id in favoriteIds,
                    onClick = { onWallpaperClick(w) },
                    onFavoriteClick = { onFavoriteClick(w) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    // Search results mix sources — every tile shows its
                    // origin badge so users can tell at a glance whether
                    // a result is from Pexels or Unsplash.
                    showSourceBadge = true,
                )
            }
            if (isRemote && !allLoaded) {
                item(
                    span = StaggeredGridItemSpan.FullLine,
                    key = "loading_more",
                ) {
                    LoadingMoreIndicator(
                        isLoading = isLoadingMore,
                        onLoadMore = onLoadMore,
                    )
                }
            }
        }
    }
}
