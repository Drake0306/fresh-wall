package com.example.freshwall.data

interface WallpaperRepository {
    suspend fun getWallpapers(): List<Wallpaper>
}
