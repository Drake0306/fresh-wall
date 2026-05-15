package com.example.freshwall.ui.detail

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.actions.ApplyTarget
import com.example.freshwall.actions.CropTransform
import com.example.freshwall.actions.WallpaperActions
import com.example.freshwall.ads.RewardedAdManager
import com.example.freshwall.data.FavoritesManager
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.util.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    wallpaper: Wallpaper,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = remember(context) { context.applicationContext as FreshWallApplication }
    val actions = app.wallpaperActions
    val adManager = app.rewardedAdManager
    val favoritesManager: FavoritesManager = app.favoritesManager
    val favorites by favoritesManager.favorites.collectAsStateWithLifecycle()
    val isFavorite = remember(favorites, wallpaper.id) {
        favorites.any { it.id == wallpaper.id }
    }
    val activity = remember(context) { context.findActivity() }

    // Reset zoom/pan/immersive whenever the user opens a new wallpaper.
    var scale by remember(wallpaper.id) { mutableFloatStateOf(1f) }
    var offsetX by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    var viewSize by remember(wallpaper.id) { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(wallpaper.id) { mutableStateOf<IntSize?>(null) }
    var isImmersive by remember(wallpaper.id) { mutableStateOf(false) }
    var hasSetInitialScale by remember(wallpaper.id) { mutableStateOf(false) }

    // Once both image and view are measured, auto-zoom so the image visually
    // fills the screen — the look of ContentScale.Crop, but the underlying
    // image is still rendered with ContentScale.Fit (no pixels discarded).
    // User can pinch out back to scale = 1 to see the whole image with letterbox.
    LaunchedEffect(imageSize, viewSize) {
        if (hasSetInitialScale) return@LaunchedEffect
        val img = imageSize ?: return@LaunchedEffect
        if (viewSize.width <= 0 || viewSize.height <= 0 ||
            img.width <= 0 || img.height <= 0
        ) return@LaunchedEffect
        val fitFit = minOf(
            viewSize.width.toFloat() / img.width,
            viewSize.height.toFloat() / img.height,
        )
        val fitCrop = maxOf(
            viewSize.width.toFloat() / img.width,
            viewSize.height.toFloat() / img.height,
        )
        scale = (fitCrop / fitFit).coerceIn(MIN_SCALE, MAX_SCALE)
        hasSetInitialScale = true
    }

    var showApplySheet by remember { mutableStateOf(false) }
    var showDownloadConfirm by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var burstLike by remember { mutableStateOf(false) }

    LaunchedEffect(burstLike) {
        if (burstLike) {
            delay(700)
            burstLike = false
        }
    }

    val onLikeToggle: () -> Unit = {
        val nowFavorite = favoritesManager.toggle(wallpaper)
        if (nowFavorite) burstLike = true
    }

    val downloadPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            gateAndRun(adManager, activity, context) {
                scope.launch { runDownload(actions, wallpaper, context) }
            }
        } else {
            Toast.makeText(context, "Permission required to save image", Toast.LENGTH_SHORT).show()
        }
    }

    val onDownloadConfirmed: () -> Unit = {
        showDownloadConfirm = false
        val needsRuntimePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val hasPermission = !needsRuntimePermission ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            gateAndRun(adManager, activity, context) {
                scope.launch { runDownload(actions, wallpaper, context) }
            }
        } else {
            downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { viewSize = it },
        ) {
            SubcomposeAsyncImage(
                model = wallpaper.fullUrl,
                contentDescription = null,
                // Fit = the WHOLE image is always rendered, never cropped.
                // For portrait images on portrait phones, this naturally
                // fills the width (with vertical letterbox for the rest).
                // Pinch in for closeups; the image is always recoverable.
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val drawable = state.result.drawable
                    val w = drawable.intrinsicWidth
                    val h = drawable.intrinsicHeight
                    if (w > 0 && h > 0) imageSize = IntSize(w, h)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "image-${wallpaper.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
                    .pointerInput(wallpaper.id) {
                        detectTapGestures(
                            onTap = { isImmersive = !isImmersive },
                            onDoubleTap = { onLikeToggle() },
                        )
                    }
                    .pointerInput(wallpaper.id, viewSize, imageSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            val max = computeMaxOffsets(viewSize, imageSize, newScale)
                            scale = newScale
                            // Pan multiplier: at higher zoom, each finger-pixel
                            // moves the image by more so the perceived speed of
                            // covering image content matches what it feels like
                            // at zoom = 1.
                            val panMul = newScale
                            offsetX = (offsetX + pan.x * panMul).coerceIn(-max.x, max.x)
                            offsetY = (offsetY + pan.y * panMul).coerceIn(-max.y, max.y)
                        }
                    },
                loading = {
                    AsyncImage(
                        model = wallpaper.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(20.dp),
                    )
                },
                error = {
                    AsyncImage(
                        model = wallpaper.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(20.dp),
                    )
                },
            )

            FilledIconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }

            FilledIconButton(
                onClick = onLikeToggle,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .align(Alignment.TopEnd),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    contentColor = if (isFavorite) Color.Red else Color.White,
                ),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                )
            }

            LikeBurst(
                visible = burstLike,
                modifier = Modifier.align(Alignment.Center),
            )

            AnimatedVisibility(
                visible = !isImmersive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                DetailActionSheet(
                    wallpaper = wallpaper,
                    imageSize = imageSize,
                    onApplyClick = { showApplySheet = true },
                    onDownloadClick = { showDownloadConfirm = true },
                    onInfoClick = { showInfoDialog = true },
                )
            }
        }
    }

    if (showApplySheet) {
        ApplyTargetSheet(
            onDismiss = { showApplySheet = false },
            onPick = { target ->
                showApplySheet = false
                val crop = if (viewSize.width > 0 && viewSize.height > 0) {
                    CropTransform(
                        viewWidth = viewSize.width,
                        viewHeight = viewSize.height,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                    )
                } else null
                gateAndRun(adManager, activity, context) {
                    scope.launch { runApply(actions, wallpaper, target, crop, context) }
                }
            },
        )
    }

    if (showDownloadConfirm) {
        DownloadConfirmDialog(
            wallpaper = wallpaper,
            onConfirm = onDownloadConfirmed,
            onDismiss = { showDownloadConfirm = false },
        )
    }

    if (showInfoDialog) {
        WallpaperInfoSheet(
            wallpaper = wallpaper,
            imageSize = imageSize,
            onDismiss = { showInfoDialog = false },
        )
    }
}

// At scale = 1 the image is at its ContentScale.Fit baseline — the whole
// image is visible (width-filled for portrait images, with vertical
// letterbox). Pinch in to look closer; we never go below 1 (no shrinking
// past full visibility).
private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f

@Composable
private fun LikeBurst(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0f, animationSpec = tween(220)) +
            fadeIn(animationSpec = tween(140)),
        exit = scaleOut(targetScale = 1.4f, animationSpec = tween(280)) +
            fadeOut(animationSpec = tween(280)),
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(140.dp),
        )
    }
}

private fun computeMaxOffsets(view: IntSize, image: IntSize?, scale: Float): Offset {
    if (image == null ||
        view.width <= 0 || view.height <= 0 ||
        image.width <= 0 || image.height <= 0
    ) return Offset.Zero
    // Fit math: the image is scaled to fit INSIDE the view, smaller of the
    // two dimensions wins. Pan range only opens up once `scale > 1`.
    val scaleFit = minOf(
        view.width.toFloat() / image.width,
        view.height.toFloat() / image.height,
    )
    val scaledW = image.width * scaleFit * scale
    val scaledH = image.height * scaleFit * scale
    return Offset(
        x = ((scaledW - view.width) / 2f).coerceAtLeast(0f),
        y = ((scaledH - view.height) / 2f).coerceAtLeast(0f),
    )
}

private fun gateAndRun(
    adManager: RewardedAdManager,
    activity: Activity?,
    context: android.content.Context,
    action: () -> Unit,
) {
    if (activity == null) {
        action()
        return
    }
    adManager.showAd(
        activity = activity,
        onRewardEarned = action,
        onDismissedWithoutReward = {
            Toast.makeText(context, "Watch the full ad to continue", Toast.LENGTH_SHORT).show()
        },
        onUnavailable = {
            action()
        },
    )
}


private suspend fun runApply(
    actions: WallpaperActions,
    wallpaper: Wallpaper,
    target: ApplyTarget,
    crop: CropTransform?,
    context: android.content.Context,
) {
    val result = actions.setAsWallpaper(wallpaper, target, crop)
    val msg = if (result.isSuccess) "Wallpaper applied" else "Failed to apply wallpaper"
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

private suspend fun runDownload(
    actions: WallpaperActions,
    wallpaper: Wallpaper,
    context: android.content.Context,
) {
    val result = actions.downloadToGallery(wallpaper, "freshwall-${wallpaper.id}")
    val msg = if (result.isSuccess) "Saved to gallery" else "Download failed"
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
