package io.github.drake0306.freshwall.ui.preview

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.ui.detail.WallpaperInfoContent
import io.github.drake0306.freshwall.util.rememberHaptics

/**
 * Long-press preview popup for a wallpaper tile. Shows the same details card
 * the user sees behind the info icon on the full-screen detail page —
 * photographer avatar, name, bio, social links, dimensions, source — in a
 * centered modal, with a single "Expand" button to jump into the full
 * detail screen.
 *
 * No image preview here on purpose: the details ARE the value-add, the
 * image is one tap away via Expand.
 */
@Composable
fun WallpaperPreviewPopup(
    wallpaper: Wallpaper,
    onOpenFullScreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberHaptics()

    // Drives a quick zoom-in / fade-in for the card. Starts at 0 on first
    // composition, animates to 1; LaunchedEffect flips the flag right away
    // so the card materialises rather than popping in.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val appearProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "preview-appear",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .heightIn(max = 600.dp)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        val s = 0.9f + 0.1f * appearProgress
                        scaleX = s
                        scaleY = s
                        alpha = appearProgress
                    }
                    // Eat clicks inside the card so taps don't dismiss it.
                    .clickable(
                        onClick = {},
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp,
            ) {
                Column {
                    // The reusable info card. imageSize = null falls back to
                    // the wallpaper's declared width/height so the
                    // Dimensions/Quality tiles still populate.
                    WallpaperInfoContent(
                        wallpaper = wallpaper,
                        imageSize = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    )
                    ExpandActionRow(
                        onExpand = {
                            haptics.click()
                            onOpenFullScreen()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandActionRow(onExpand: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        FilledTonalButton(
            onClick = onExpand,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.OpenInFull,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Expand",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
