package io.github.drake0306.freshwall.data

interface WallpaperRepository {
    suspend fun getWallpapers(): List<Wallpaper>
}
