package io.github.drake0306.freshwall.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Canonical list of category labels offered during onboarding. Used as
 * search-term hints against both the Pexels and Unsplash APIs, so the
 * entries are kept as photographic everyday words that both libraries
 * return solid wallpaper results for. Order is the order shown in the
 * picker grid.
 */
val WALLPAPER_CATEGORIES: List<String> = listOf(
    // — Landscape & natural —
    "Nature", "Mountains", "Ocean", "Beach", "Forest", "Desert",
    "Sky", "Sunset", "Aurora", "Plants", "Flowers", "Floral",
    // — Cosmic —
    "Space", "Galaxy", "Stars", "Moon", "Nebula",
    // — Graphic / motif —
    "Abstract", "Minimal", "Geometric", "Patterns", "Texture", "Gradient",
    // — Tone —
    "Dark", "Monochrome", "Pastel", "Neon", "Colorful",
    // — Built environment —
    "City", "Architecture", "Streets", "Skyline", "Interior",
    // — Animals —
    "Animals", "Wildlife", "Cats", "Dogs", "Birds",
    // — People & subject —
    "Portrait", "People", "Fashion",
    // — Lifestyle & themes —
    "Cars", "Travel", "Food", "Sports", "Music", "Technology",
    // — Style / aesthetic —
    "Anime", "Aesthetic", "Art", "Vintage", "Retro",
)

/** How the user wants their per-source category preferences applied. */
@Serializable
enum class CustomizationMode {
    /** Same category set drives both Pexels and Unsplash feeds. */
    COMBINED,
    /** Each source has its own category set. */
    SEPARATE,
}

/**
 * Snapshot of the user's category choices. [onboardingComplete] flips to
 * `true` after they finish (or skip) the first-run flow; the home screen
 * uses that flag to decide whether to route into Onboarding.
 *
 * Selection rules enforced in the UI:
 *   - At least 5 categories per active set.
 *   - At most 15 categories per active set.
 *   - At most [MAX_STARRED] starred categories per active set. Starred
 *     categories are higher-priority and surface more often in the feed
 *     rotation; non-starred selections still show up, just less frequently.
 */
@Serializable
data class CategoryConfig(
    val onboardingComplete: Boolean = false,
    val mode: CustomizationMode = CustomizationMode.COMBINED,
    val combinedCategories: List<String> = emptyList(),
    val pexelsCategories: List<String> = emptyList(),
    val unsplashCategories: List<String> = emptyList(),
    val combinedStarred: List<String> = emptyList(),
    val pexelsStarred: List<String> = emptyList(),
    val unsplashStarred: List<String> = emptyList(),
) {
    /** Categories that drive the Pexels feed, respecting [mode]. */
    fun pexelsActive(): List<String> = when (mode) {
        CustomizationMode.COMBINED -> combinedCategories
        CustomizationMode.SEPARATE -> pexelsCategories
    }

    /** Categories that drive the Unsplash feed, respecting [mode]. */
    fun unsplashActive(): List<String> = when (mode) {
        CustomizationMode.COMBINED -> combinedCategories
        CustomizationMode.SEPARATE -> unsplashCategories
    }

    /** Starred Pexels categories, filtered to the currently-selected pool. */
    fun pexelsStarredActive(): Set<String> {
        val pool = pexelsActive().toSet()
        val source = if (mode == CustomizationMode.COMBINED) combinedStarred else pexelsStarred
        return source.filter { it in pool }.toSet()
    }

    /** Starred Unsplash categories, filtered to the currently-selected pool. */
    fun unsplashStarredActive(): Set<String> {
        val pool = unsplashActive().toSet()
        val source = if (mode == CustomizationMode.COMBINED) combinedStarred else unsplashStarred
        return source.filter { it in pool }.toSet()
    }

    companion object {
        const val MAX_STARRED = 3
    }
}

class CategoryPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _config = MutableStateFlow(read())
    val config: StateFlow<CategoryConfig> = _config.asStateFlow()

    fun update(transform: (CategoryConfig) -> CategoryConfig) {
        val next = transform(_config.value)
        _config.value = next
        prefs.edit().putString(KEY_BLOB, json.encodeToString(next)).apply()
    }

    /** Convenience: mark onboarding complete with the final selections. */
    fun finishOnboarding(
        mode: CustomizationMode,
        combined: List<String>,
        pexels: List<String>,
        unsplash: List<String>,
        combinedStarred: List<String> = emptyList(),
        pexelsStarred: List<String> = emptyList(),
        unsplashStarred: List<String> = emptyList(),
    ) = update {
        it.copy(
            onboardingComplete = true,
            mode = mode,
            combinedCategories = combined,
            pexelsCategories = pexels,
            unsplashCategories = unsplash,
            combinedStarred = combinedStarred,
            pexelsStarred = pexelsStarred,
            unsplashStarred = unsplashStarred,
        )
    }

    private fun read(): CategoryConfig {
        val blob = prefs.getString(KEY_BLOB, null) ?: return CategoryConfig()
        return runCatching { json.decodeFromString<CategoryConfig>(blob) }
            .getOrDefault(CategoryConfig())
    }

    private companion object {
        const val PREFS_NAME = "freshwall_prefs"
        const val KEY_BLOB = "category_config_v1"
    }
}
