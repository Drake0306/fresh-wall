package io.github.drake0306.freshwall.data

/**
 * Suggestion list shown under the search field while the user is typing.
 * Mixes their own recent searches (highest priority) with a small curated
 * set of popular wallpaper search terms.
 */
private val CANNED_SUGGESTIONS = listOf(
    "mountains", "minimal", "nature", "neon", "ocean",
    "sunset", "city", "space", "abstract", "dark",
    "forest", "geometric", "flowers", "blue", "purple",
    "art", "skyline", "macro", "desert", "rain",
)

fun searchSuggestions(
    query: String,
    history: List<String>,
    limit: Int = 4,
): List<String> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val fromHistory = history.filter { it.lowercase().contains(q) }
    val fromCanned = CANNED_SUGGESTIONS.filter { it.startsWith(q) }
    return (fromHistory + fromCanned)
        .map { it.trim() }
        .filter { it.lowercase() != q } // don't suggest exactly what's typed
        .distinctBy { it.lowercase() }
        .take(limit)
}
