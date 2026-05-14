package com.example.freshwall.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps the user's last [MAX_ITEMS] search queries, most-recent first. */
class SearchHistoryManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(read())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val updated = (listOf(trimmed) +
            _history.value.filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_ITEMS)
        _history.value = updated
        prefs.edit().putString(KEY_HISTORY, updated.joinToString(SEPARATOR)).apply()
    }

    fun clear() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun read(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.take(MAX_ITEMS)
    }

    private companion object {
        const val PREFS_NAME = "freshwall_search"
        const val KEY_HISTORY = "history"
        const val SEPARATOR = "\n"
        const val MAX_ITEMS = 10
    }
}
