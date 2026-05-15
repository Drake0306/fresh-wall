package com.example.freshwall.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.freshwall.R
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.WallpaperSource

/**
 * Bottom-sheet "details" view for a wallpaper. Photographer-first: big
 * avatar + name + (when available) bio / location / social links, then a
 * 2-column grid of info tiles (Dimensions, Quality, Likes, Source).
 *
 * Unsplash photos carry the full attribution payload (avatar, @handle,
 * bio, Instagram, Twitter, portfolio); Pexels gives us name + photo URL
 * only, so the extras gracefully disappear when their fields are null.
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

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PhotographerAvatar(
                    avatarUrl = wallpaper.authorAvatarUrl,
                    name = wallpaper.author,
                    size = 88.dp,
                )
            }
            Spacer(Modifier.size(14.dp))

            Text(
                text = wallpaper.author ?: "Unknown",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )

            wallpaper.authorUsername?.let { username ->
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }

            SourceBadge(source = wallpaper.source)

            wallpaper.authorBio?.let { bio ->
                Spacer(Modifier.size(12.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                )
            }

            wallpaper.authorLocation?.let { location ->
                Spacer(Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val socialLinks = buildSocialLinks(wallpaper)
            if (socialLinks.isNotEmpty()) {
                Spacer(Modifier.size(16.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    socialLinks.forEach { link ->
                        SocialPill(
                            label = link.label,
                            icon = link.icon,
                            onClick = { openExternal(link.url) },
                        )
                    }
                }
            }

            Spacer(Modifier.size(20.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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

                val likes = wallpaper.likes
                val swatch = wallpaper.dominantColor?.let(::parseHexColor)
                if (likes != null || swatch != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (likes != null) {
                            InfoCard(
                                label = "Likes",
                                value = formatCompact(likes),
                                icon = Icons.Outlined.Favorite,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (swatch != null) {
                            ColorSwatchCard(
                                hex = wallpaper.dominantColor!!,
                                color = swatch,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if ((likes != null) xor (swatch != null)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                wallpaper.sourceUrl?.let { url ->
                    InfoCard(
                        label = sourceLabel(wallpaper.source),
                        value = "View original",
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { openExternal(url) },
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*  Small helpers                                                             */
/* -------------------------------------------------------------------------- */

private data class SocialLink(val label: String, val icon: ImageVector, val url: String)

private fun buildSocialLinks(wallpaper: Wallpaper): List<SocialLink> = buildList {
    wallpaper.authorUrl?.takeIf { it.isNotBlank() }?.let {
        add(SocialLink(sourceLabel(wallpaper.source), Icons.AutoMirrored.Outlined.OpenInNew, it))
    }
    wallpaper.authorInstagram?.let {
        add(
            SocialLink(
                label = "Instagram",
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                url = "https://instagram.com/$it",
            ),
        )
    }
    wallpaper.authorTwitter?.let {
        add(
            SocialLink(
                label = "Twitter",
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                url = "https://twitter.com/$it",
            ),
        )
    }
    wallpaper.authorPortfolioUrl?.let {
        add(SocialLink("Portfolio", Icons.Outlined.Language, it))
    }
}

private fun sourceLabel(source: WallpaperSource): String = when (source) {
    WallpaperSource.PEXELS -> "Pexels"
    WallpaperSource.UNSPLASH -> "Unsplash"
    WallpaperSource.FEATURED -> "FreshWall"
}

@Composable
private fun SourceBadge(source: WallpaperSource) {
    val (iconRes, label) = when (source) {
        WallpaperSource.PEXELS -> R.drawable.ic_pexels to "Powered by Pexels"
        WallpaperSource.UNSPLASH -> R.drawable.ic_unsplash to "Powered by Unsplash"
        WallpaperSource.FEATURED -> return
    }
    Spacer(Modifier.size(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SocialPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ColorSwatchCard(hex: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = hex.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(2.dp))
            Text(
                text = "Dominant color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun parseHexColor(hex: String): Color? = runCatching {
    val cleaned = hex.removePrefix("#").trim()
    when (cleaned.length) {
        6 -> Color(("FF$cleaned").toLong(16))
        8 -> Color(cleaned.toLong(16))
        else -> null
    }
}.getOrNull()

private fun formatCompact(count: Int): String = when {
    count >= 1_000_000 -> "${count / 100_000 / 10.0}M"
    count >= 1_000 -> "${count / 100 / 10.0}K"
    else -> count.toString()
}
