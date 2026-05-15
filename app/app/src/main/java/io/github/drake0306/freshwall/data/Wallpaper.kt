package io.github.drake0306.freshwall.data

import kotlinx.serialization.Serializable

@Serializable
enum class WallpaperSource { FEATURED, PEXELS, UNSPLASH }

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

    // ── Manifest-driven metadata (R2). All optional so older serialised
    // ── blobs (e.g. persisted favorites) keep deserialising cleanly.
    /** Tagged category strings, e.g. `["Nature", "Minimal"]`. */
    val categories: List<String> = emptyList(),
    /** Free-form tags for search / sort, e.g. `["aurora", "sky"]`. */
    val tags: List<String> = emptyList(),
    /** Native pixel width of [fullUrl]. */
    val width: Int? = null,
    /** Native pixel height of [fullUrl]. */
    val height: Int? = null,
    /** Size of the full image in bytes. */
    val fileSize: Long? = null,
    /** ISO-8601 timestamp the wallpaper was added to the catalog. */
    val uploadedAt: String? = null,

    // ── Rich attribution (Unsplash carries all of these; Pexels only
    // ── populates author/authorUrl above; manifest leaves them null).
    /** Photographer handle (no `@` prefix), e.g. `"johndoe"`. */
    val authorUsername: String? = null,
    /** Short photographer bio. */
    val authorBio: String? = null,
    /** Photographer free-form location string. */
    val authorLocation: String? = null,
    /** Photographer avatar/profile-picture URL. */
    val authorAvatarUrl: String? = null,
    /** Photographer's portfolio site. */
    val authorPortfolioUrl: String? = null,
    /** Photographer's Instagram handle (no `@`). */
    val authorInstagram: String? = null,
    /** Photographer's Twitter / X handle (no `@`). */
    val authorTwitter: String? = null,
    /** Like count on the source platform, when exposed. */
    val likes: Int? = null,
    /** Dominant hex color (e.g. `"#A1B2C3"`) — used as a load-time placeholder. */
    val dominantColor: String? = null,

    /**
     * Source-specific URL the app must hit to register an apply/download
     * event (currently only Unsplash — its API Guidelines require it). The
     * client fires a GET against this URL once per successful user action.
     */
    val trackingDownloadUrl: String? = null,
) {
    val displayName: String get() = name ?: "Wallpaper $id"
}

/**
 * Top-level shape of the wallpapers manifest hosted on R2 (or wherever
 * [io.github.drake0306.freshwall.data.manifest.RemoteWallpaperRepository] points).
 * [version] lets us evolve the format without breaking older app builds.
 */
@Serializable
data class WallpaperManifest(
    val version: Int = 1,
    val generatedAt: String? = null,
    val wallpapers: List<Wallpaper> = emptyList(),
)
