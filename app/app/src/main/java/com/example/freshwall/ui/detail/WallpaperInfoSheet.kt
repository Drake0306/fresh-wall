package com.example.freshwall.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.freshwall.R
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.WallpaperSource

/**
 * Bottom-sheet "details" view for a wallpaper. Photographer-first:
 * big avatar + name on top, then a 2-column grid of info tiles
 * (Dimensions, Quality MP, Source link, Photographer profile).
 *
 * Limitations: Pexels API doesn't expose photographer avatar URLs,
 * Instagram/X handles, or profile descriptions. When that data isn't
 * available, the corresponding UI is simply omitted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WallpaperInfoSheet(
    wallpaper: Wallpaper,
    imageSize: IntSize?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openExternal(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    val dims = imageSize?.takeIf { it.width > 0 && it.height > 0 }
    val megapixels = dims?.let {
        val mp = it.width.toLong() * it.height / 1_000_000L
        if (mp >= 1) "$mp MP" else "<1 MP"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.size(8.dp))

            // Centered photographer avatar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PhotographerAvatar(name = wallpaper.author, size = 88.dp)
            }
            Spacer(Modifier.size(16.dp))

            // Centered photographer name
            Text(
                text = wallpaper.author ?: "Unknown",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )

            if (wallpaper.source == WallpaperSource.PEXELS) {
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_pexels),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "Powered by Pexels",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.size(24.dp))

            // Info grid (2-column)
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Row 1 — Dimensions + Quality
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(
                        label = "Dimensions",
                        value = dims?.let { "${it.width} × ${it.height}" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    InfoCard(
                        label = "Quality",
                        value = megapixels ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }

                // Row 2 — Source + Photographer (only the ones we have URLs for)
                val hasSource = wallpaper.sourceUrl != null
                val hasAuthor = wallpaper.authorUrl != null
                if (hasSource || hasAuthor) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (hasSource) {
                            InfoCard(
                                label = "Source",
                                value = "Pexels",
                                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                                onClick = { openExternal(wallpaper.sourceUrl!!) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (hasAuthor) {
                            InfoCard(
                                label = "Photographer",
                                value = "Profile",
                                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                                onClick = { openExternal(wallpaper.authorUrl!!) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // If only one card, give it visual symmetry by adding
                        // an invisible spacer with weight 1.
                        if (hasSource xor hasAuthor) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
