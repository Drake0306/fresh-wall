package io.github.drake0306.freshwall.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.drake0306.freshwall.R
import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.data.WallpaperSource
import io.github.drake0306.freshwall.util.rememberHaptics

// Tile-shape bounds for the staggered grid. Min keeps super-tall panoramas
// from blowing out a column's height; max keeps ultra-wide panoramas from
// collapsing to ribbons. Range is generous on purpose — common landscapes
// (16:9 ≈ 1.78, 3:2 = 1.5) and portraits (9:16 ≈ 0.56, 2:3 ≈ 0.67) all
// pass through unmodified, so the staggered effect actually reads as
// "respects each photo's shape." Outside the range, the tile uses the
// clamp value and ContentScale.Crop trims the source bitmap.
private const val MIN_TILE_RATIO = 0.5f      // 1:2  — tallest portrait allowed
private const val MAX_TILE_RATIO = 2.0f      // 2:1  — widest landscape allowed
private const val DEFAULT_TILE_RATIO = 9f / 16f  // fallback when width/height missing

/**
 * Centered "FreshWall" title shown at the top of the home screen.
 *
 * [hideProgress] (0..1) fades + shrinks the pill so it dissolves before
 * reaching the opaque status-bar backdrop. [raised] toggles the pill's
 * surface treatment: at the very top of the feed we want the title to
 * read as plain text on the background (no chip behind it); once content
 * scrolls under it, the pill gains its surface + elevation so it stands
 * out from the moving content. [sourceBadge] adds the source's brand mark
 * next to the title — Pexels mark when on the Pexels tab, Unsplash mark
 * when on the Unsplash tab, nothing for Featured. Signals what the user
 * is currently browsing without crowding the bottom tab pills.
 */
@Composable
fun HomeTitleBar(
    onTitleClick: () -> Unit,
    hideProgress: Float,
    raised: Boolean,
    sourceBadge: WallpaperSource?,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (raised) MaterialTheme.colorScheme.surfaceContainerHigh
                      else Color.Transparent,
        label = "titleBg",
    )
    val shadow by animateDpAsState(
        targetValue = if (raised) 4.dp else 0.dp,
        label = "titleShadow",
    )
    val tonal by animateDpAsState(
        targetValue = if (raised) 3.dp else 0.dp,
        label = "titleTonal",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Top padding zeroed so the pill sits flush against the
            // status-bar baseline (the parent Column already spaces the
            // status bar with statusBarTopDp). Bottom padding stays
            // generous to keep the gap between the pill and the first
            // row of grid tiles below.
            .padding(top = 0.dp, bottom = 58.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onTitleClick,
            shape = CircleShape,
            color = bg,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = tonal,
            shadowElevation = shadow,
            modifier = Modifier.graphicsLayer {
                val p = hideProgress.coerceIn(0f, 1f)
                alpha = 1f - p
                val s = 1f - 0.08f * p
                scaleX = s
                scaleY = s
            },
        ) {
            // Swap the title content with a sequential fade — the old
            // version fades out completely first, then the layout snaps to
            // the new size while the pill is invisible, then the new
            // version fades in. The size change is hidden inside the
            // invisible midpoint, so the user never sees "FreshWall" jump
            // sideways when a source badge appears, disappears, or swaps.
            AnimatedContent(
                targetState = sourceBadge,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 160, delayMillis = 160)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 160)) using
                        SizeTransform(clip = false) { _, _ -> snap(delayMillis = 160) }
                },
                label = "titleSourceBadge",
            ) { source ->
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "FreshWall",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val badgeRes = when (source) {
                        WallpaperSource.PEXELS -> R.drawable.ic_pexels
                        WallpaperSource.UNSPLASH -> R.drawable.ic_unsplash
                        else -> null
                    }
                    if (badgeRes != null) {
                        Icon(
                            imageVector = ImageVector.vectorResource(badgeRes),
                            contentDescription = "Showing ${source?.name?.lowercase()} photos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating bottom navigation. Three separate pills, centered, not full-width.
 * - Menu (left): standalone circular pill
 * - Tabs (middle): pill with two segments; selected segment gets a filled
 *   `secondaryContainer` background (Material 3 Expressive style)
 * - Search (right): standalone circular pill
 */
@Composable
fun BottomNavBar(
    tabLabels: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularNavPill(
            icon = Icons.Outlined.Menu,
            contentDescription = "Menu",
            // Menu uses the open() double-bump so the drawer reveal *feels*
            // like a panel sliding in, not a plain button press.
            onClick = { haptics.open(); onMenuClick() },
        )
        if (tabLabels.size > 1) {
            TabsPill(
                labels = tabLabels,
                selectedIndex = selectedIndex,
                onTabSelected = { i -> haptics.tabSwitch(); onTabSelected(i) },
            )
        }
        CircularNavPill(
            icon = Icons.Outlined.Search,
            contentDescription = "Search",
            // Search uses the standard click() — distinct from the lighter
            // tabSwitch() so the user can feel which pill they hit.
            onClick = { haptics.click(); onSearchClick() },
        )
    }
}

@Composable
private fun CircularNavPill(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // Pill + icon both scaled up ~10% from the original 48dp / 24dp pair.
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.size(53.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun TabsPill(
    labels: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEachIndexed { i, label ->
                TabSegment(
                    label = label,
                    selected = i == selectedIndex,
                    onClick = { onTabSelected(i) },
                )
            }
        }
    }
}

@Composable
private fun TabSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
                      else Color.Transparent,
        label = "tabSegmentBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabSegmentText",
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            // Touch padding bumped together with the font size so the
            // tab pills keep a balanced shape after the 10% type increase.
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            // labelLarge ×1.1 — matches the CircularNavPill icon bump so the
            // bottom rack reads at one consistent size.
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = MaterialTheme.typography.labelLarge.fontSize * 1.1f,
            ),
            color = textColor,
        )
    }
}

/**
 * Filter categories shown above the Featured grid. Each maps to a deterministic
 * slice of the bundled wallpaper list — no server roundtrip, no Pexels.
 * "Daily" reshuffles once per day so the picks feel fresh without us having
 * to ship new content.
 */
enum class WallpaperCategory(val label: String) {
    ALL("All"),
    DAILY("Daily"),
    TRENDING("Trending"),
    CURATED("Curated"),
    CLASSICS("Classics"),
}

/**
 * Apply [category] to the bundled [wallpapers]. Pure, deterministic — the
 * same input on the same day yields the same output, so back-navigation
 * doesn't reshuffle results under the user.
 */
fun filterByCategory(
    wallpapers: List<Wallpaper>,
    category: WallpaperCategory,
): List<Wallpaper> {
    if (wallpapers.isEmpty()) return wallpapers
    return when (category) {
        WallpaperCategory.ALL -> wallpapers
        WallpaperCategory.DAILY -> {
            val seed = java.time.LocalDate.now().toEpochDay()
            wallpapers.shuffled(kotlin.random.Random(seed))
                .take(6.coerceAtMost(wallpapers.size))
        }
        WallpaperCategory.TRENDING -> wallpapers.take(8.coerceAtMost(wallpapers.size))
        WallpaperCategory.CURATED -> {
            val mid = wallpapers.size / 2
            wallpapers.drop(mid).take(8.coerceAtMost(wallpapers.size - mid))
        }
        WallpaperCategory.CLASSICS -> wallpapers.takeLast(8.coerceAtMost(wallpapers.size))
    }
}

/**
 * Horizontal row of selectable category pills. Mirrors the bottom-nav tabs:
 * selected chip gets [ColorScheme.secondaryContainer]; the rest sit on
 * [ColorScheme.surfaceContainerHigh] so they read as a row of buttons.
 */
@Composable
fun CategoryChipsRow(
    selected: WallpaperCategory,
    onSelect: (WallpaperCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WallpaperCategory.entries.forEach { category ->
            CategoryChip(
                label = category.label,
                selected = category == selected,
                onClick = {
                    haptics.click()
                    onSelect(category)
                },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
                      else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "categoryChipBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                      else MaterialTheme.colorScheme.onSurface,
        label = "categoryChipText",
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        contentColor = textColor,
        tonalElevation = if (selected) 0.dp else 2.dp,
        shadowElevation = if (selected) 0.dp else 1.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

// Cycled across skeleton tiles so the loading state has the same varied-
// height rhythm as the real staggered grid, instead of a uniform block of
// 9:16 placeholders that would jolt into staggered layout when real tiles
// arrive.
private val SKELETON_RATIOS = listOf(
    0.66f, 0.75f, 0.56f, 1.0f, 0.6f, 0.85f, 0.7f, 1.2f,
)

/**
 * Greyed-out placeholder grid shown while the first page of a feed is in
 * flight. Mirrors the real staggered layout (varied tile heights) so the
 * page doesn't jolt when real results replace it. Each tile breathes
 * between two alpha values to signal "loading" without the heavyweight
 * shimmer animation.
 */
@Composable
fun WallpaperGridSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    tileCount: Int = 8,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalItemSpacing = 6.dp,
        contentPadding = contentPadding,
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        items(tileCount) { index ->
            SkeletonTile(ratio = SKELETON_RATIOS[index % SKELETON_RATIOS.size])
        }
    }
}

@Composable
private fun SkeletonTile(ratio: Float) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun WallpaperTile(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    showSourceBadge: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val haptics = rememberHaptics()
    // Viewport-entry pop-in. Each tile's `hasAppeared` flips to true
    // exactly once per composition — which happens when the tile first
    // becomes visible (scrolled into view) or when a new page of items
    // is paginated in. The animateFloatAsState drives a 280ms decelerating
    // fade-from-zero + slide-up-by-24dp. Subtle but it's the effect
    // people perceive as "smooth scrolling" in apps like Crisper —
    // tiles materialise instead of snapping into place, which masks the
    // brief beat where the Coil image is still decoding.
    var hasAppeared by remember(wallpaper.id) { mutableStateOf(false) }
    val entryProgress by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
        label = "tile-entry",
    )
    LaunchedEffect(wallpaper.id) { hasAppeared = true }

    // Tile aspect ratio = the source photo's actual ratio (width / height),
    // clamped to a sensible range so a stray panorama or skyscraper-portrait
    // doesn't break the staggered layout. Falls back to 9:16 when the API
    // didn't return dimensions (older manifest-served wallpapers).
    val tileRatio = remember(wallpaper.width, wallpaper.height) {
        val w = wallpaper.width
        val h = wallpaper.height
        if (w != null && h != null && w > 0 && h > 0) {
            (w.toFloat() / h.toFloat()).coerceIn(MIN_TILE_RATIO, MAX_TILE_RATIO)
        } else {
            DEFAULT_TILE_RATIO
        }
    }

    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    alpha = entryProgress
                    translationY = (1f - entryProgress) * 24.dp.toPx()
                }
                .aspectRatio(tileRatio)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick?.let { lc ->
                        {
                            haptics.longPress()
                            lc()
                        }
                    },
                ),
        ) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "image-${wallpaper.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
            )
            val badgeRes = if (showSourceBadge) {
                when (wallpaper.source) {
                    WallpaperSource.PEXELS -> R.drawable.ic_pexels
                    WallpaperSource.UNSPLASH -> R.drawable.ic_unsplash
                    WallpaperSource.FEATURED -> null
                }
            } else null
            if (badgeRes != null) {
                SourceBadge(
                    iconRes = badgeRes,
                    contentDescription = "From ${wallpaper.source.name.lowercase()}",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                )
            }
            FavoriteOverlayIcon(
                isFavorite = isFavorite,
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
        }
    }
}

/**
 * Source badge overlay for a wallpaper tile. Small circular chip with
 * the source's brand mark inside. The Box uses a fixed square `size()`
 * so the `CircleShape` clip renders as a real circle (not a pill /
 * oblong) regardless of the glyph's natural width.
 */
@Composable
private fun SourceBadge(
    @androidx.annotation.DrawableRes iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun FavoriteOverlayIcon(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    IconButton(
        onClick = {
            haptics.like()
            onClick()
        },
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.25f),
            contentColor = if (isFavorite) Color.Red else Color.White,
        ),
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
        )
    }
}

/**
 * Centered placeholder for the Featured grid — used when there's no
 * manifest configured yet, when a fetch failed, or when the active
 * category filter has zero results.
 */
@Composable
fun FeaturedEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
