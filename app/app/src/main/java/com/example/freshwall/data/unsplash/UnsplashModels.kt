package com.example.freshwall.data.unsplash

import com.example.freshwall.data.Wallpaper
import com.example.freshwall.data.WallpaperSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire schema for the Unsplash API responses. Shape mirrors what
 * [com.example.freshwall.data.pexels.PexelsModels] does for Pexels so the
 * UI layer can stay source-agnostic.
 */
@Serializable
internal data class UnsplashSearchResponse(
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    val results: List<UnsplashPhoto> = emptyList(),
)

@Serializable
internal data class UnsplashPhoto(
    val id: String,
    val width: Int = 0,
    val height: Int = 0,
    val description: String? = null,
    @SerialName("alt_description") val altDescription: String? = null,
    val urls: UnsplashUrls,
    val user: UnsplashUser,
    val links: UnsplashLinks,
) {
    fun toWallpaper(): Wallpaper = Wallpaper(
        id = "unsplash:$id",
        // Unsplash's "regular" is ~1080w — fine for grid thumbnails.
        // We could prefer "small" (400w) for the thumb to save bandwidth.
        thumbnailUrl = urls.small.ifBlank { urls.regular },
        // "raw" preserves the original; "full" is JPEG-encoded for fast
        // delivery. We use "full" so users get a sensibly sized download.
        fullUrl = urls.full.ifBlank { urls.regular },
        name = (description ?: altDescription)
            ?.takeIf { it.isNotBlank() }
            ?: "Unsplash photo",
        description = description?.takeIf { it.isNotBlank() },
        author = user.name.takeIf { it.isNotBlank() },
        authorUrl = user.links.html.takeIf { it.isNotBlank() },
        sourceUrl = links.html.takeIf { it.isNotBlank() },
        source = WallpaperSource.UNSPLASH,
        width = width.takeIf { it > 0 },
        height = height.takeIf { it > 0 },
    )
}

@Serializable
internal data class UnsplashUrls(
    val raw: String = "",
    val full: String = "",
    val regular: String = "",
    val small: String = "",
    val thumb: String = "",
)

@Serializable
internal data class UnsplashUser(
    val name: String = "",
    val username: String = "",
    val links: UnsplashUserLinks = UnsplashUserLinks(),
)

@Serializable
internal data class UnsplashUserLinks(
    val html: String = "",
)

@Serializable
internal data class UnsplashLinks(
    val html: String = "",
)
