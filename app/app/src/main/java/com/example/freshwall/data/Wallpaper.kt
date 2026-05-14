package com.example.freshwall.data

import kotlinx.serialization.Serializable

@Serializable
enum class WallpaperSource { FEATURED, PEXELS }

@Serializable
data class Wallpaper(
    val id: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    val name: String? = null,
    val description: String? = null,
    val author: String? = null,
    val authorUrl: String? = null,
    /** URL to the source page for this wallpaper (e.g. its Pexels listing). */
    val sourceUrl: String? = null,
    val source: WallpaperSource = WallpaperSource.FEATURED,
) {
    val displayName: String get() = name ?: "Wallpaper $id"
}

@Serializable
internal data class WallpaperManifest(
    val wallpapers: List<Wallpaper>,
)
