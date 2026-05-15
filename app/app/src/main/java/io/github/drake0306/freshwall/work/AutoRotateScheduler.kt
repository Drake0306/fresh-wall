package io.github.drake0306.freshwall.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.drake0306.freshwall.FreshWallApplication
import io.github.drake0306.freshwall.data.AutoRotateConfig
import java.util.concurrent.TimeUnit

object AutoRotateScheduler {

    private const val WORK_NAME = "freshwall_auto_rotate"

    /** Read the current config and either schedule or cancel the periodic worker. */
    fun applyCurrent(context: Context) {
        val app = context.applicationContext as FreshWallApplication
        apply(context, app.autoRotatePreferences.config.value)
    }

    fun apply(context: Context, config: AutoRotateConfig) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (!config.enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (config.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()
        // WorkManager's minimum period is 15 minutes.
        val interval = config.intervalMinutes.coerceAtLeast(15L)
        val request = PeriodicWorkRequestBuilder<AutoRotateWorker>(
            interval, TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
