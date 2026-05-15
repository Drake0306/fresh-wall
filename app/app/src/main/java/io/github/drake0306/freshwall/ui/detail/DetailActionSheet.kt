package io.github.drake0306.freshwall.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.drake0306.freshwall.actions.ApplyTarget
import io.github.drake0306.freshwall.data.Wallpaper

/**
 * Vertical drag distance, in dp, that opens the info sheet when the user
 * flicks the drag-handle pill upward. Chosen to feel intentional without
 * making the gesture finicky — anything past this and the info sheet opens
 * on release.
 */
private const val DRAG_OPEN_INFO_DP = 40

/**
 * Bottom action sheet for the detail screen.
 * Header: [avatar][photographer + dimensions][info icon]. Body: Apply/Download.
 * Bonus: the drag-handle pill at the top accepts an upward flick to open the
 * info sheet, mirroring the standard "pull up to expand" bottom-sheet idiom.
 */
@Composable
internal fun DetailActionSheet(
    wallpaper: Wallpaper,
    imageSize: IntSize?,
    onApplyClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Drag-handle pill. The visible pill stays the same small chip
            // it always was, but the gesture target around it is enlarged
            // so an upward flick from the user's thumb reliably catches.
            // Dragging it up past DRAG_OPEN_INFO_DP and releasing opens the
            // info sheet — same destination as tapping the explicit info
            // icon, just a more natural "pull up to expand" gesture.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 80.dp, height = 24.dp)
                    .pointerInput(Unit) {
                        val thresholdPx = DRAG_OPEN_INFO_DP.dp.toPx()
                        var dragAmount = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragAmount = 0f },
                            onDragEnd = {
                                if (dragAmount < -thresholdPx) onInfoClick()
                                dragAmount = 0f
                            },
                            onDragCancel = { dragAmount = 0f },
                            onVerticalDrag = { _, delta -> dragAmount += delta },
                        )
                    }
                    // TalkBack users can't do the vertical-drag gesture, so
                    // surface the same action as a semantic click. The
                    // explicit Info icon button still works too — this just
                    // gives the handle itself an accessible affordance.
                    .semantics {
                        role = Role.Button
                        onClick(label = "Open photo details") {
                            onInfoClick()
                            true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PhotographerAvatar(
                    avatarUrl = wallpaper.authorAvatarUrl,
                    name = wallpaper.author,
                    size = 36.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wallpaper.author ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val dims = imageSize?.takeIf { it.width > 0 && it.height > 0 }
                    if (dims != null) {
                        Text(
                            text = "${dims.width} × ${dims.height}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Image details",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.size(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilledTonalButton(
                    onClick = onApplyClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Wallpaper, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply")
                }
                FilledTonalButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download")
                }
            }
        }
    }
}

@Composable
internal fun DownloadConfirmDialog(
    wallpaper: Wallpaper,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to gallery?") },
        text = {
            Text(
                "This wallpaper will be saved to your Photos in the FreshWall folder.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Photographer avatar. Renders the real profile photo from [avatarUrl] when
 * present (Unsplash exposes one per photo); falls back to a Material-style
 * "no photo" placeholder otherwise — a neutral surfaceVariant circle with a
 * Person silhouette inside.
 */
@Composable
internal fun PhotographerAvatar(
    avatarUrl: String?,
    name: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val avatarModifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name?.let { "$it's profile picture" },
            contentScale = ContentScale.Crop,
            modifier = avatarModifier,
        )
    } else {
        Box(
            modifier = avatarModifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.7f),
            )
        }
    }
}

/**
 * Square info card used in the WallpaperInfoSheet grid.
 * Headline value on top (large), small label underneath.
 */
@Composable
internal fun InfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (icon != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
