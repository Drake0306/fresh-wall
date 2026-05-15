package com.example.freshwall.ui

import com.example.freshwall.data.Wallpaper

sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data object Settings : Screen
    data object About : Screen
    data object Theme : Screen
    data object Favorites : Screen
    data object AutoRotate : Screen
    data object Donate : Screen
    data object Feedback : Screen
    /** First-launch onboarding flow. Drops into Home on completion. */
    data object Onboarding : Screen
    /** Settings → re-edit category preferences (no welcome / purpose steps). */
    data object CategoryEditor : Screen
    data class Detail(val wallpaper: Wallpaper) : Screen
}
