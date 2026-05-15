package io.github.drake0306.freshwall.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.drake0306.freshwall.FreshWallApplication
import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.data.WallpaperSource
import io.github.drake0306.freshwall.ui.drawer.AppDrawer
import io.github.drake0306.freshwall.ui.drawer.DrawerItem
import io.github.drake0306.freshwall.ui.featured.FeaturedViewModel
import io.github.drake0306.freshwall.ui.pexels.PexelsHomeViewModel
import io.github.drake0306.freshwall.ui.preview.WallpaperPreviewPopup
import io.github.drake0306.freshwall.ui.unsplash.UnsplashHomeViewModel
import io.github.drake0306.freshwall.ui.components.LoadingMoreIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * The three possible content tabs on the home screen. Which subset is
 * actually shown is controlled by `SourcePreferences`. The enum's
 * declaration order doubles as the display order — Featured first (if
 * enabled), then Pexels, then Unsplash.
 */
private enum class HomeSource(val label: String, val source: WallpaperSource) {
    Featured("Featured", WallpaperSource.FEATURED),
    Pexels("Pexels", WallpaperSource.PEXELS),
    Unsplash("Unsplash", WallpaperSource.UNSPLASH),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: FeaturedViewModel,
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearchClick: () -> Unit,
    onDrawerItemClick: (DrawerItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Tracks the selected tab by SOURCE NAME (not raw index) so toggling
    // sources on/off in Settings doesn't accidentally bounce the user to a
    // different tab. If `null` or the name doesn't resolve to any enabled
    // source, we fall back to the first available source.
    var selectedSourceName by rememberSaveable { mutableStateOf<String?>(null) }
    var showDrawer by remember { mutableStateOf(false) }
    var previewWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FreshWallApplication }
    val favoritesManager = remember(app) { app.favoritesManager }
    val favorites by favoritesManager.favorites.collectAsStateWithLifecycle()
    val favoriteIds = remember(favorites) { favorites.asSequence().map { it.id }.toHashSet() }

    // Which sources the user has enabled (Featured / Pexels / Unsplash).
    // Drives both the bottom tab pills and the content branching below.
    // Defaults: Featured OFF (experimental), Pexels + Unsplash ON.
    val sourceConfig by app.sourcePreferences.config.collectAsStateWithLifecycle()
    val enabledSources = remember(sourceConfig) {
        buildList {
            if (sourceConfig.featured) add(HomeSource.Featured)
            if (sourceConfig.pexels) add(HomeSource.Pexels)
            if (sourceConfig.unsplash) add(HomeSource.Unsplash)
        }
    }
    // SourcePreferences guarantees at least one enabled. Resolve the chosen
    // source from its saved name; fall back to the first enabled source if
    // the name doesn't match any currently-enabled tab (e.g. the user
    // disabled the source they were on).
    val safeSelectedIndex = enabledSources
        .indexOfFirst { it.name == selectedSourceName }
        .takeIf { it >= 0 } ?: 0
    val activeSource: HomeSource? = enabledSources.getOrNull(safeSelectedIndex)

    val density = LocalDensity.current
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val tabHeightDp = 48.dp
    // Visual budget reserved at the bottom of each grid so wallpaper tiles
    // never sit underneath the always-visible bottom pill bar.
    //   navBar inset + lift padding + pill row + breathing gap.
    val barHeightDp = 80.dp
    val barLiftDp = 12.dp
    val triggerPx = with(density) { 80.dp.toPx() }

    var hideProgress by rememberSaveable { mutableFloatStateOf(0f) }
    val nestedScroll = remember(triggerPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                hideProgress = (hideProgress - available.y / triggerPx).coerceIn(0f, 1f)
                return Offset.Zero
            }
        }
    }

    val gridState = rememberLazyStaggeredGridState()
    val pexelsGridState = rememberLazyStaggeredGridState()
    val unsplashGridState = rememberLazyStaggeredGridState()

    var selectedCategory by rememberSaveable { mutableStateOf(WallpaperCategory.ALL) }
    val featuredWallpapers = remember(uiState.wallpapers, selectedCategory) {
        filterByCategory(uiState.wallpapers, selectedCategory)
    }

    val pexelsViewModel: PexelsHomeViewModel = viewModel()
    val pexelsState by pexelsViewModel.uiState.collectAsStateWithLifecycle()
    val unsplashViewModel: UnsplashHomeViewModel = viewModel()
    val unsplashState by unsplashViewModel.uiState.collectAsStateWithLifecycle()

    val activeGridState = when (activeSource) {
        HomeSource.Pexels -> pexelsGridState
        HomeSource.Unsplash -> unsplashGridState
        HomeSource.Featured, null -> gridState
    }

    // The title pill is "raised" (gets its surface + elevation) only when
    // there's actually content scrolled under it. Keyed on activeGridState
    // so switching tabs re-binds to the new grid's scroll state — without
    // the key, the closure would forever read the grid from the first
    // composition.
    val titleRaised by remember(activeGridState) {
        derivedStateOf {
            activeGridState.firstVisibleItemIndex > 0 ||
                activeGridState.firstVisibleItemScrollOffset > 4
        }
    }

    // Tracks the last source the user was actively on (by name, since the
    // index meaning depends on which sources are enabled). Saveable so it
    // survives detail-screen round-trips. Lets us detect a real tab-switch
    // (vs composition just re-entering after a back-nav from Detail) so we
    // can reset the remote grids to the top only when the user actually
    // taps INTO them — not when they return from a wallpaper preview.
    var lastActiveSource by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(activeSource) {
        val current = activeSource
        val justSwitchedTo = current != null && current.name != lastActiveSource
        lastActiveSource = current?.name
        when (current) {
            HomeSource.Pexels -> {
                pexelsViewModel.loadIfNeeded()
                if (justSwitchedTo) pexelsGridState.animateScrollToItem(0)
            }
            HomeSource.Unsplash -> {
                unsplashViewModel.loadIfNeeded()
                if (justSwitchedTo) unsplashGridState.animateScrollToItem(0)
            }
            else -> Unit
        }
    }

    // When the user picks a different category chip, jump back to the top
    // of the Featured grid so they actually see the new filter — not the
    // middle of it.
    LaunchedEffect(selectedCategory) {
        if (activeSource == HomeSource.Featured) {
            hideProgress = 0f
            gridState.animateScrollToItem(0)
        }
    }

    // Trigger loadMore when the active remote grid scrolls near the end.
    //
    // We pair the trigger condition with `total` so the snapshot value
    // changes whenever a page of new items arrives. That way, if a previous
    // loadMore call was deferred (raced with the initial load), the flow
    // re-emits as soon as new items land. Without `total` in the snapshot,
    // distinctUntilChanged would suppress the second emit and the user
    // would be stranded at the end of the list with nothing loading.
    LaunchedEffect(pexelsGridState, activeSource) {
        if (activeSource != HomeSource.Pexels) return@LaunchedEffect
        snapshotFlow {
            val info = pexelsGridState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to (total > 0 && last >= total - 3)
        }
            .distinctUntilChanged()
            .filter { (_, nearEnd) -> nearEnd }
            .collect { pexelsViewModel.loadMore() }
    }
    LaunchedEffect(unsplashGridState, activeSource) {
        if (activeSource != HomeSource.Unsplash) return@LaunchedEffect
        snapshotFlow {
            val info = unsplashGridState.layoutInfo
            val total = info.totalItemsCount
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to (total > 0 && last >= total - 3)
        }
            .distinctUntilChanged()
            .filter { (_, nearEnd) -> nearEnd }
            .collect { unsplashViewModel.loadMore() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScroll),
    ) {
        // Shared content padding for the three source grids so they all
        // tuck neatly behind the title pill at the top and the bottom nav
        // at the bottom.
        val gridPadding = PaddingValues(
            start = 4.dp,
            end = 4.dp,
            top = statusBarTopDp + tabHeightDp + 8.dp,
            bottom = navBarBottomDp + barHeightDp + barLiftDp + 8.dp,
        )

        AnimatedContent(
            targetState = activeSource,
            label = "homeTab",
            transitionSpec = {
                val forward = (targetState?.ordinal ?: 0) > (initialState?.ordinal ?: 0)
                if (forward) {
                    (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)))
                } else {
                    (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280)))
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { source ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (source) {
                    HomeSource.Featured -> {
                        when {
                            uiState.isLoading -> WallpaperGridSkeleton(
                                contentPadding = gridPadding,
                            )
                            uiState.unconfigured -> FeaturedEmptyState(
                                title = "Wallpaper feed coming soon",
                                body = "We're setting up the catalog. Browse the Pexels or Unsplash tab for now.",
                                modifier = Modifier.align(Alignment.Center),
                            )
                            uiState.error != null -> FeaturedEmptyState(
                                title = "Couldn't load wallpapers",
                                body = uiState.error.orEmpty(),
                                actionLabel = "Try again",
                                onAction = { viewModel.refresh() },
                                modifier = Modifier.align(Alignment.Center),
                            )
                            featuredWallpapers.isEmpty() && uiState.heroes.isEmpty() -> FeaturedEmptyState(
                                title = "Nothing in this category yet",
                                body = "Try another chip — or pull down to refresh.",
                                modifier = Modifier.align(Alignment.Center),
                            )
                            else -> PullToRefreshBox(
                                isRefreshing = uiState.isRefreshing,
                                onRefresh = { viewModel.refresh() },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                LazyVerticalStaggeredGrid(
                                    state = gridState,
                                    columns = StaggeredGridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalItemSpacing = 6.dp,
                                    contentPadding = gridPadding,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    item(
                                        span = StaggeredGridItemSpan.FullLine,
                                        key = "categories",
                                    ) {
                                        CategoryChipsRow(
                                            selected = selectedCategory,
                                            onSelect = { selectedCategory = it },
                                        )
                                    }
                                    items(featuredWallpapers, key = { it.id }) { w ->
                                        WallpaperTile(
                                            wallpaper = w,
                                            isFavorite = w.id in favoriteIds,
                                            onClick = { onWallpaperClick(w) },
                                            onFavoriteClick = { favoritesManager.toggle(w) },
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            onLongClick = { previewWallpaper = w },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HomeSource.Pexels -> RemoteSourceGrid(
                        gridState = pexelsGridState,
                        contentPadding = gridPadding,
                        wallpapers = pexelsState.wallpapers,
                        isInitialLoading = pexelsState.isLoading && pexelsState.wallpapers.isEmpty(),
                        initialError = pexelsState.error.takeIf { pexelsState.wallpapers.isEmpty() },
                        allLoaded = pexelsState.allLoaded,
                        isLoadingMore = pexelsState.isLoadingMore,
                        isRefreshing = pexelsState.isRefreshing,
                        onRefresh = { pexelsViewModel.refresh() },
                        onLoadMore = { pexelsViewModel.loadMore() },
                        favoriteIds = favoriteIds,
                        onWallpaperClick = onWallpaperClick,
                        onWallpaperLongClick = { previewWallpaper = it },
                        onFavoriteToggle = { favoritesManager.toggle(it) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    HomeSource.Unsplash -> RemoteSourceGrid(
                        gridState = unsplashGridState,
                        contentPadding = gridPadding,
                        wallpapers = unsplashState.wallpapers,
                        isInitialLoading = unsplashState.isLoading && unsplashState.wallpapers.isEmpty(),
                        initialError = unsplashState.error.takeIf { unsplashState.wallpapers.isEmpty() },
                        allLoaded = unsplashState.allLoaded,
                        isLoadingMore = unsplashState.isLoadingMore,
                        isRefreshing = unsplashState.isRefreshing,
                        onRefresh = { unsplashViewModel.refresh() },
                        onLoadMore = { unsplashViewModel.loadMore() },
                        favoriteIds = favoriteIds,
                        onWallpaperClick = onWallpaperClick,
                        onWallpaperLongClick = { previewWallpaper = it },
                        onFavoriteToggle = { favoritesManager.toggle(it) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    null -> FeaturedEmptyState(
                        title = "No sources enabled",
                        body = "Open Settings → Sources to turn one on.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            Spacer(Modifier.height(statusBarTopDp))
            HomeTitleBar(
                onTitleClick = {
                    scope.launch {
                        hideProgress = 0f
                        activeGridState.animateScrollToItem(0)
                    }
                },
                hideProgress = hideProgress,
                raised = titleRaised,
                sourceBadge = activeSource?.source?.takeIf { it != WallpaperSource.FEATURED },
            )
        }

        // Always-opaque status-bar backdrop. Sits ON TOP of grid items
        // (so scrolling images can never bleed through), the sliding title
        // row, and the collapsing hero — so each disappears cleanly under it.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(statusBarTopDp)
                .background(MaterialTheme.colorScheme.background),
        )

        // Bottom pill bar — absolutely positioned, never auto-hides on scroll.
        // It only goes away when the user navigates off the home screen.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottomDp + barLiftDp),
        ) {
            BottomNavBar(
                tabLabels = enabledSources.map { it.label },
                selectedIndex = safeSelectedIndex,
                onTabSelected = { i ->
                    selectedSourceName = enabledSources.getOrNull(i)?.name
                },
                onMenuClick = { showDrawer = true },
                onSearchClick = onSearchClick,
            )
        }
    }

    if (showDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showDrawer = false },
            sheetState = sheetState,
        ) {
            AppDrawer(
                onItemClick = { item ->
                    showDrawer = false
                    onDrawerItemClick(item)
                },
            )
        }
    }

    previewWallpaper?.let { w ->
        WallpaperPreviewPopup(
            wallpaper = w,
            onOpenFullScreen = {
                previewWallpaper = null
                onWallpaperClick(w)
            },
            onDismiss = { previewWallpaper = null },
        )
    }
}

/**
 * Shared grid renderer for the Pexels and Unsplash tabs — both are paged
 * remote sources with the same loading / error / load-more shape, so this
 * single composable handles them both. Avoids duplicating the LazyGrid
 * setup, the LoadingMoreIndicator wiring, and the empty-state branches.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.RemoteSourceGrid(
    gridState: LazyStaggeredGridState,
    contentPadding: PaddingValues,
    wallpapers: List<Wallpaper>,
    isInitialLoading: Boolean,
    initialError: String?,
    allLoaded: Boolean,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    favoriteIds: Set<String>,
    onWallpaperClick: (Wallpaper) -> Unit,
    onWallpaperLongClick: (Wallpaper) -> Unit,
    onFavoriteToggle: (Wallpaper) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    when {
        isInitialLoading -> WallpaperGridSkeleton(
            contentPadding = contentPadding,
        )
        initialError != null -> Text(
            text = initialError,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
        )
        else -> PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(wallpapers, key = { it.id }) { w ->
                    WallpaperTile(
                        wallpaper = w,
                        isFavorite = w.id in favoriteIds,
                        onClick = { onWallpaperClick(w) },
                        onFavoriteClick = { onFavoriteToggle(w) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onLongClick = { onWallpaperLongClick(w) },
                    )
                }
                if (!allLoaded) {
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
}
