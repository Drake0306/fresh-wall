package com.example.freshwall.ui.search

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
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
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.searchSuggestions
import com.example.freshwall.ui.components.LoadingMoreIndicator
import com.example.freshwall.ui.featured.FeaturedViewModel
import com.example.freshwall.ui.home.WallpaperTile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val PEXELS_CATEGORIES = listOf(
    "Nature", "Mountains", "Ocean", "Sunset", "Forest",
    "Abstract", "Minimal", "Dark", "Neon", "Geometric",
    "City", "Space", "Flowers", "Art", "Macro",
)

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
    val submittedQuery by searchViewModel.submittedQuery.collectAsStateWithLifecycle()
    val searchPexels by searchViewModel.searchPexels.collectAsStateWithLifecycle()

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

    val resultsGridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun submit() {
        val q = query.text.trim()
        if (q.isEmpty()) return
        searchViewModel.submit(q)
        keyboard?.hide()
    }

    // Trigger pagination when the results grid scrolls near the end.
    LaunchedEffect(resultsGridState, submittedQuery, searchPexels) {
        if (!searchPexels || submittedQuery.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val info = resultsGridState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
            .distinctUntilChanged()
            .filter { it }
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
                        text = "Search Pexels",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = if (searchPexels) {
                            "Searches the Pexels library"
                        } else {
                            "Searches your local collection"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = searchPexels,
                    onCheckedChange = { searchViewModel.setSearchPexels(it) },
                )
            }

            // Category chips + filter indicator — only when Pexels search is active.
            AnimatedVisibility(visible = searchPexels) {
                Column {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(PEXELS_CATEGORIES, key = { it }) { category ->
                            val isSelected = submittedQuery.equals(category, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AspectRatio,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Showing portrait wallpapers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    isPexels = searchPexels,
                    isLoading = searchPexels && pexelsState.isLoading,
                    isLoadingMore = searchPexels && pexelsState.isLoadingMore,
                    allLoaded = pexelsState.allLoaded,
                    error = if (searchPexels) pexelsState.error else null,
                    results = if (searchPexels) pexelsState.results else localResults,
                    onWallpaperClick = onWallpaperClick,
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
    isPexels: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    allLoaded: Boolean,
    error: String?,
    results: List<Wallpaper>,
    favoriteIds: Set<String>,
    onFavoriteClick: (Wallpaper) -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
) {
    when {
        isLoading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
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
                text = if (isPexels) "No Pexels results" else "Nothing in your library matches that",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
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
                )
            }
            if (isPexels && !allLoaded) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "loading_more",
                ) {
                    LoadingMoreIndicator(visible = isLoadingMore)
                }
            }
        }
    }
}
