package com.example.freshwall.ui.home

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.ui.drawer.AppDrawer
import com.example.freshwall.ui.drawer.DrawerItem
import com.example.freshwall.ui.featured.FeaturedViewModel
import com.example.freshwall.ui.pexels.PexelsHomeViewModel
import com.example.freshwall.ui.components.LoadingMoreIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val favoritesManager = remember(context) {
        (context.applicationContext as FreshWallApplication).favoritesManager
    }
    val favorites by favoritesManager.favorites.collectAsStateWithLifecycle()
    val favoriteIds = remember(favorites) { favorites.asSequence().map { it.id }.toHashSet() }

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

    val gridState = rememberLazyGridState()
    val pexelsGridState = rememberLazyGridState()

    var selectedCategory by rememberSaveable { mutableStateOf(WallpaperCategory.ALL) }
    val featuredWallpapers = remember(uiState.wallpapers, selectedCategory) {
        filterByCategory(uiState.wallpapers, selectedCategory)
    }

    // The title pill is "raised" (gets its surface + elevation) only when
    // there's actually content scrolled under it. At the very top of the
    // feed it stays flat so it reads as plain text on the page background.
    val titleRaised by remember {
        derivedStateOf {
            val active = if (selectedTab == 0) gridState else pexelsGridState
            active.firstVisibleItemIndex > 0 ||
                active.firstVisibleItemScrollOffset > 4
        }
    }

    val pexelsViewModel: PexelsHomeViewModel = viewModel()
    val pexelsState by pexelsViewModel.uiState.collectAsStateWithLifecycle()

    // Tracks the last tab the user was actively on. Saveable so it survives
    // detail-screen round-trips. Used to detect a real tab-switch (vs the
    // composition simply re-entering after a back-nav from Detail), so we
    // can reset the Pexels grid to the top only when the user actually taps
    // INTO the Pexels tab — not when they return from a wallpaper preview.
    var lastActiveTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        val justSwitchedToPexels = selectedTab == 1 && lastActiveTab != 1
        lastActiveTab = selectedTab
        if (selectedTab == 1) {
            pexelsViewModel.loadIfNeeded()
            if (justSwitchedToPexels) {
                pexelsGridState.animateScrollToItem(0)
            }
        }
    }

    // When the user picks a different category chip, jump back to the top
    // of the Featured grid so they actually see the new filter — not the
    // middle of it.
    LaunchedEffect(selectedCategory) {
        if (selectedTab == 0) {
            hideProgress = 0f
            gridState.animateScrollToItem(0)
        }
    }

    // Trigger loadMore when the Pexels grid scrolls near the end.
    //
    // We pair the trigger condition with `total` so the snapshot value
    // changes whenever a page of new items arrives. That way, if a previous
    // loadMore call was deferred (e.g. raced with the initial load that
    // hadn't finished yet), the flow re-emits as soon as the new items
    // land and another loadMore fires. Without `total` in the snapshot,
    // distinctUntilChanged would suppress the second emit and the user
    // would be stranded at the end of the list with nothing loading.
    LaunchedEffect(pexelsGridState, selectedTab) {
        if (selectedTab != 1) return@LaunchedEffect
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScroll),
    ) {
        AnimatedContent(
            targetState = selectedTab,
            label = "homeTab",
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(280)) { it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)))
                } else {
                    (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(280)))
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { tab ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    1 -> {
                        when {
                            pexelsState.isLoading && pexelsState.wallpapers.isEmpty() -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            pexelsState.error != null && pexelsState.wallpapers.isEmpty() -> {
                                Text(
                                    text = pexelsState.error.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 32.dp),
                                )
                            }
                            else -> {
                                LazyVerticalGrid(
                                    state = pexelsGridState,
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = statusBarTopDp + tabHeightDp + 8.dp,
                                        bottom = navBarBottomDp + barHeightDp + barLiftDp + 8.dp,
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    items(pexelsState.wallpapers, key = { it.id }) { w ->
                                        WallpaperTile(
                                            wallpaper = w,
                                            isFavorite = w.id in favoriteIds,
                                            onClick = { onWallpaperClick(w) },
                                            onFavoriteClick = { favoritesManager.toggle(w) },
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                        )
                                    }
                                    if (!pexelsState.allLoaded) {
                                        item(
                                            span = { GridItemSpan(maxLineSpan) },
                                            key = "loading_more",
                                        ) {
                                            LoadingMoreIndicator(
                                                isLoading = pexelsState.isLoadingMore,
                                                onLoadMore = { pexelsViewModel.loadMore() },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        when {
                            uiState.isLoading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            uiState.unconfigured -> {
                                FeaturedEmptyState(
                                    title = "Wallpaper feed coming soon",
                                    body = "We're setting up the catalog. Browse the Pexels tab for now.",
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            uiState.error != null -> {
                                FeaturedEmptyState(
                                    title = "Couldn't load wallpapers",
                                    body = uiState.error.orEmpty(),
                                    actionLabel = "Try again",
                                    onAction = { viewModel.refresh() },
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            featuredWallpapers.isEmpty() && uiState.heroes.isEmpty() -> {
                                FeaturedEmptyState(
                                    title = "Nothing in this category yet",
                                    body = "Try another chip — or pull down to refresh.",
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                            else -> {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = statusBarTopDp + tabHeightDp + 8.dp,
                                        bottom = navBarBottomDp + barHeightDp + barLiftDp + 8.dp,
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    item(
                                        span = { GridItemSpan(maxLineSpan) },
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
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                        if (selectedTab == 0) {
                            gridState.animateScrollToItem(0)
                        } else {
                            pexelsGridState.animateScrollToItem(0)
                        }
                    }
                },
                hideProgress = hideProgress,
                raised = titleRaised,
                showPexelsBadge = selectedTab == 1,
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
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
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
}
