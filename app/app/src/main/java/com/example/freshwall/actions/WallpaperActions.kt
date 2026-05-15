package com.example.freshwall.actions

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.unsplash.UnsplashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ApplyTarget(val flag: Int) {
    HOME(WallpaperManager.FLAG_SYSTEM),
    LOCK(WallpaperManager.FLAG_LOCK),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

/**
 * User's pinch/pan adjustment, captured from the detail screen.
 * Used to bake the visible region into the wallpaper bitmap.
 */
data class CropTransform(
    val viewWidth: Int,
    val viewHeight: Int,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

class WallpaperActions(
    private val context: Context,
    private val unsplashRepository: UnsplashRepository,
) {

    // Reuse the app-wide Coil singleton so this work shares the memory+disk cache
    // with the grid's AsyncImage loads (and doesn't double-allocate caches).
    private val imageLoader get() = context.imageLoader

    suspend fun setAsWallpaper(
        wallpaper: Wallpaper,
        target: ApplyTarget,
        crop: CropTransform? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val source = loadBitmap(wallpaper.fullUrl)
            val finalBitmap = crop?.takeIf { it.viewWidth > 0 && it.viewHeight > 0 }
                ?.let { renderTransformedBitmap(source, it) }
                ?: source
            // WallpaperManager returns the new wallpaper id; 0 means "nothing
            // was applied" — happens silently on some OEM skins (older MIUI,
            // some ColorOS variants) that restrict wallpaper changes from apps.
            val newId = WallpaperManager.getInstance(context)
                .setBitmap(finalBitmap, null, true, target.flag)
            if (newId == 0) {
                error(
                    "Couldn't apply wallpaper. Your device may have restricted " +
                        "wallpaper changes from third-party apps."
                )
            }
            Unit
        }.also { result ->
            if (result.isSuccess) fireTrackingHit(wallpaper)
        }
    }

    suspend fun downloadToGallery(wallpaper: Wallpaper, fileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = loadBitmap(wallpaper.fullUrl)
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/FreshWall",
                        )
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Failed to create MediaStore entry")
                resolver.openOutputStream(uri).use { out ->
                    requireNotNull(out) { "Failed to open output stream" }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                Unit
            }.also { result ->
                if (result.isSuccess) fireTrackingHit(wallpaper)
            }
        }

    /**
     * Sends the per-source "user used this photo" event. Currently only
     * Unsplash exposes such an endpoint (and requires it). Fire-and-forget —
     * never blocks or surfaces an error to the caller.
     */
    private suspend fun fireTrackingHit(wallpaper: Wallpaper) {
        val url = wallpaper.trackingDownloadUrl ?: return
        unsplashRepository.trackDownload(url)
    }

    private suspend fun loadBitmap(url: String): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable
            ?: error("Failed to download image")
        return (drawable as? BitmapDrawable)?.bitmap
            ?: error("Image is not a bitmap")
    }

    /**
     * Recreate what the user sees on the detail screen as a bitmap:
     * 1. Fit source via ContentScale.Crop
     * 2. Apply user's pinch-zoom (origin = view center)
     * 3. Apply user's pan offset
     */
    private fun renderTransformedBitmap(source: Bitmap, crop: CropTransform): Bitmap {
        val out = Bitmap.createBitmap(crop.viewWidth, crop.viewHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val matrix = Matrix()

        val scaleFit = maxOf(
            crop.viewWidth.toFloat() / source.width,
            crop.viewHeight.toFloat() / source.height,
        )
        matrix.postScale(scaleFit, scaleFit)
        matrix.postTranslate(
            (crop.viewWidth - source.width * scaleFit) / 2f,
            (crop.viewHeight - source.height * scaleFit) / 2f,
        )
        matrix.postScale(crop.scale, crop.scale, crop.viewWidth / 2f, crop.viewHeight / 2f)
        matrix.postTranslate(crop.offsetX, crop.offsetY)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        canvas.drawBitmap(source, matrix, paint)
        return out
    }
}
