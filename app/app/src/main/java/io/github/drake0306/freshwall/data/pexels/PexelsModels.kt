package io.github.drake0306.freshwall.data.pexels

import io.github.drake0306.freshwall.data.Wallpaper
import io.github.drake0306.freshwall.data.WallpaperSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PexelsSearchResponse(
    val photos: List<PexelsPhoto> = emptyList(),
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
internal data class PexelsCuratedResponse(
    val photos: List<PexelsPhoto> = emptyList(),
)

@Serializable
internal data class PexelsPhoto(
    val id: Long,
    val width: Int = 0,
    val height: Int = 0,
    val url: String = "",
    val photographer: String = "",
    @SerialName("photographer_url") val photographerUrl: String = "",
    val alt: String = "",
    val src: PexelsSrc,
) {
    fun toWallpaper(): Wallpaper = Wallpaper(
        id = "pexels:$id",
        name = alt.takeIf { it.isNotBlank() } ?: "Pexels photo",
        description = null,
        // `large` is an aspect-preserving resize (long edge ≤ 940px). The
        // staggered grid needs the original aspect so the tile's
        // .aspectRatio(width/height) frames the whole image without a second
        // centre-crop. `portrait` would force a 2:3 crop and break that.
        thumbnailUrl = src.large.ifEmpty { src.medium.ifEmpty { src.portrait } },
        // Full-resolution original — no resize, no crop. UI handles zoom-to-fit.
        fullUrl = src.original.ifEmpty { src.large2x },
        author = photographer.takeIf { it.isNotBlank() },
        authorUrl = photographerUrl.takeIf { it.isNotBlank() },
        sourceUrl = url.takeIf { it.isNotBlank() },
        source = WallpaperSource.PEXELS,
        width = width.takeIf { it > 0 },
        height = height.takeIf { it > 0 },
    )
}

@Serializable
internal data class PexelsSrc(
    val original: String = "",
    val large2x: String = "",
    val large: String = "",
    val medium: String = "",
    val small: String = "",
    val portrait: String = "",
    val landscape: String = "",
    val tiny: String = "",
)
