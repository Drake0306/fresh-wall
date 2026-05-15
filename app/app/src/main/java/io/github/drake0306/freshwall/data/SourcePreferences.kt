package io.github.drake0306.freshwall.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which wallpaper sources the user has enabled. Controls which tabs show
 * up on the Home screen and which sources [io.github.drake0306.freshwall.ui.search.SearchViewModel]
 * queries when the user searches.
 *
 * Invariant: at least one of [featured]/[pexels]/[unsplash] is `true`. The
 * setter ([SourcePreferences.update]) enforces this — if a caller tries to
 * flip every source off, the change is rejected and the previous state is
 * preserved.
 *
 * Defaults: Featured starts **off** because the bundled catalog is still
 * being curated (and the R2 manifest isn't wired up to a real bucket yet),
 * so we don't want users opening the app and seeing an empty Featured tab
 * by default. Pexels + Unsplash both start on.
 */
data class SourceConfig(
    val featured: Boolean = false,
    val pexels: Boolean = true,
    val unsplash: Boolean = true,
) {
    /** Convenience: any of the three on (always true given the invariant). */
    val anyEnabled: Boolean get() = featured || pexels || unsplash

    /** Convenience: how many sources are enabled. Used by the UI to decide
     *  whether to render a tab bar at all (single source = no tab bar). */
    val enabledCount: Int get() =
        (if (featured) 1 else 0) + (if (pexels) 1 else 0) + (if (unsplash) 1 else 0)
}

class SourcePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(read())
    val config: StateFlow<SourceConfig> = _config.asStateFlow()

    /**
     * Apply [transform] to the current config. The new config is only
     * persisted if at least one source remains enabled — otherwise the
     * call is a no-op so we never end up with a home screen that has zero
     * tabs to show.
     */
    fun update(transform: (SourceConfig) -> SourceConfig) {
        val next = transform(_config.value)
        if (!next.anyEnabled) return
        _config.value = next
        prefs.edit()
            .putBoolean(KEY_FEATURED, next.featured)
            .putBoolean(KEY_PEXELS, next.pexels)
            .putBoolean(KEY_UNSPLASH, next.unsplash)
            .apply()
    }

    private fun read(): SourceConfig {
        val default = SourceConfig()
        return SourceConfig(
            featured = prefs.getBoolean(KEY_FEATURED, default.featured),
            pexels = prefs.getBoolean(KEY_PEXELS, default.pexels),
            unsplash = prefs.getBoolean(KEY_UNSPLASH, default.unsplash),
        )
    }

    private companion object {
        const val PREFS_NAME = "freshwall_prefs"
        const val KEY_FEATURED = "source_featured_enabled"
        const val KEY_PEXELS = "source_pexels_enabled"
        const val KEY_UNSPLASH = "source_unsplash_enabled"
    }
}
