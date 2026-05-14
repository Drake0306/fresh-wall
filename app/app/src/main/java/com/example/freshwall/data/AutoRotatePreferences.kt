package com.example.freshwall.data

import android.content.Context
import com.example.freshwall.actions.ApplyTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AutoRotateSource { FAVORITES, FEATURED }

data class AutoRotateConfig(
    val enabled: Boolean = false,
    val intervalMinutes: Long = 360, // 6 hours default
    val source: AutoRotateSource = AutoRotateSource.FEATURED,
    val target: ApplyTarget = ApplyTarget.HOME,
    val wifiOnly: Boolean = true,
)

class AutoRotatePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(read())
    val config: StateFlow<AutoRotateConfig> = _config.asStateFlow()

    fun update(transform: (AutoRotateConfig) -> AutoRotateConfig) {
        val next = transform(_config.value)
        _config.value = next
        prefs.edit()
            .putBoolean(KEY_ENABLED, next.enabled)
            .putLong(KEY_INTERVAL, next.intervalMinutes)
            .putString(KEY_SOURCE, next.source.name)
            .putString(KEY_TARGET, next.target.name)
            .putBoolean(KEY_WIFI_ONLY, next.wifiOnly)
            .apply()
    }

    private fun read(): AutoRotateConfig = AutoRotateConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        intervalMinutes = prefs.getLong(KEY_INTERVAL, 360L),
        source = runCatching {
            AutoRotateSource.valueOf(
                prefs.getString(KEY_SOURCE, AutoRotateSource.FEATURED.name)
                    ?: AutoRotateSource.FEATURED.name
            )
        }.getOrDefault(AutoRotateSource.FEATURED),
        target = runCatching {
            ApplyTarget.valueOf(
                prefs.getString(KEY_TARGET, ApplyTarget.HOME.name)
                    ?: ApplyTarget.HOME.name
            )
        }.getOrDefault(ApplyTarget.HOME),
        wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, true),
    )

    private companion object {
        const val PREFS_NAME = "freshwall_autorotate"
        const val KEY_ENABLED = "enabled"
        const val KEY_INTERVAL = "interval_minutes"
        const val KEY_SOURCE = "source"
        const val KEY_TARGET = "target"
        const val KEY_WIFI_ONLY = "wifi_only"
    }
}
