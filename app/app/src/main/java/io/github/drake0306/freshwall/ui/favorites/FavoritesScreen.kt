package io.github.drake0306.freshwall.ui.favorites

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.drake0306.freshwall.FreshWallApplication
import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.ui.home.WallpaperTile
import io.github.drake0306.freshwall.ui.settings.SettingsTopBar

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onWallpaperClick: (Wallpaper) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val favoritesManager = remember(context) {
        (context.applicationContext as FreshWallApplication).favoritesManager
    }
    val favorites by favoritesManager.favorites.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(title = "Favorites", onBack = onBack)

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No favorites yet.\nTap the heart on any wallpaper to add it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val navBarBottomDp = WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = navBarBottomDp + 16.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(favorites, key = { it.id }) { w ->
                        WallpaperTile(
                            wallpaper = w,
                            isFavorite = true,
                            onClick = { onWallpaperClick(w) },
                            onFavoriteClick = { favoritesManager.toggle(w) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            showSourceBadge = true,
                        )
                    }
                }
            }
        }
    }
}
