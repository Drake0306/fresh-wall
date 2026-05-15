package io.github.drake0306.freshwall.data.unsplash

import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.data.WallpaperSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire schema for the Unsplash API responses. Shape mirrors what
 * [io.github.drake0306.freshwall.data.pexels.PexelsModels] does for Pexels so the
 * UI layer can stay source-agnostic. All optional fields default so the
 * deserializer (with `ignoreUnknownKeys = true`) keeps working when the
 * API drops or adds keys.
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
    /** Dominant hex color, e.g. `"#A1B2C3"` — handy as a load-time placeholder. */
    val color: String? = null,
    val likes: Int = 0,
    val description: String? = null,
    @SerialName("alt_description") val altDescription: String? = null,
    val urls: UnsplashUrls,
    val user: UnsplashUser,
    val links: UnsplashLinks,
) {
    fun toWallpaper(): Wallpaper = Wallpaper(
        id = "unsplash:$id",
        thumbnailUrl = urls.small.ifBlank { urls.regular },
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
        authorUsername = user.username.takeIf { it.isNotBlank() },
        authorBio = user.bio?.takeIf { it.isNotBlank() },
        authorLocation = user.location?.takeIf { it.isNotBlank() },
        authorAvatarUrl = user.profileImage?.bestAvatar(),
        authorPortfolioUrl = user.portfolioUrl?.takeIf { it.isNotBlank() },
        authorInstagram = user.instagramUsername?.takeIf { it.isNotBlank() },
        authorTwitter = user.twitterUsername?.takeIf { it.isNotBlank() },
        likes = likes.takeIf { it > 0 },
        dominantColor = color?.takeIf { it.isNotBlank() },
        trackingDownloadUrl = links.downloadLocation.takeIf { it.isNotBlank() },
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
    val bio: String? = null,
    val location: String? = null,
    @SerialName("portfolio_url") val portfolioUrl: String? = null,
    @SerialName("instagram_username") val instagramUsername: String? = null,
    @SerialName("twitter_username") val twitterUsername: String? = null,
    @SerialName("profile_image") val profileImage: UnsplashProfileImage? = null,
    val links: UnsplashUserLinks = UnsplashUserLinks(),
)

@Serializable
internal data class UnsplashProfileImage(
    val small: String = "",
    val medium: String = "",
    val large: String = "",
) {
    /** Prefer the largest size that's actually populated. */
    fun bestAvatar(): String? = large.ifBlank { medium.ifBlank { small } }
        .takeIf { it.isNotBlank() }
}

@Serializable
internal data class UnsplashUserLinks(
    val html: String = "",
)

@Serializable
internal data class UnsplashLinks(
    val html: String = "",
    /**
     * URL to hit when a user actually uses the photo (apply / save). Required
     * by Unsplash's API Guidelines — see the [trackDownload] call in
     * [UnsplashRepository].
     */
    @SerialName("download_location") val downloadLocation: String = "",
)
