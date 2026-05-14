package com.example.freshwall.ui.autorotate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.actions.ApplyTarget
import com.example.freshwall.data.AutoRotateSource
import com.example.freshwall.ui.settings.SettingsTopBar
import com.example.freshwall.work.AutoRotateScheduler

@Composable
fun AutoRotateScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FreshWallApplication }
    val prefs = app.autoRotatePreferences
    val config by prefs.config.collectAsStateWithLifecycle()

    fun update(transform: (com.example.freshwall.data.AutoRotateConfig) -> com.example.freshwall.data.AutoRotateConfig) {
        prefs.update(transform)
        AutoRotateScheduler.apply(context, prefs.config.value)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsTopBar(title = "Auto-rotate", onBack = onBack)
            Spacer(Modifier.height(8.dp))

            // Status / enable card
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (config.enabled) "Auto-rotate is on" else "Auto-rotate is off",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (config.enabled) {
                                "${humanInterval(config.intervalMinutes)} · ${humanSource(config.source)} → ${humanTarget(config.target)}"
                            } else {
                                "Pick a source and interval, then turn it on."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { on -> update { it.copy(enabled = on) } },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionHeader("Source")
            SingleChoiceRow(
                label = "Featured",
                description = "Pick at random from the FreshWall collection",
                selected = config.source == AutoRotateSource.FEATURED,
                onClick = { update { it.copy(source = AutoRotateSource.FEATURED) } },
            )
            SingleChoiceRow(
                label = "Favorites only",
                description = "Only rotate among wallpapers you've hearted",
                selected = config.source == AutoRotateSource.FAVORITES,
                onClick = { update { it.copy(source = AutoRotateSource.FAVORITES) } },
            )

            Spacer(Modifier.height(16.dp))
            SectionHeader("Interval")
            IntervalChoices.forEach { (minutes, label) ->
                SingleChoiceRow(
                    label = label,
                    description = null,
                    selected = config.intervalMinutes == minutes,
                    onClick = { update { it.copy(intervalMinutes = minutes) } },
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("Apply to")
            SingleChoiceRow(
                label = "Home screen",
                description = null,
                selected = config.target == ApplyTarget.HOME,
                onClick = { update { it.copy(target = ApplyTarget.HOME) } },
            )
            SingleChoiceRow(
                label = "Lock screen",
                description = null,
                selected = config.target == ApplyTarget.LOCK,
                onClick = { update { it.copy(target = ApplyTarget.LOCK) } },
            )
            SingleChoiceRow(
                label = "Both",
                description = null,
                selected = config.target == ApplyTarget.BOTH,
                onClick = { update { it.copy(target = ApplyTarget.BOTH) } },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wi-Fi only",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "Don't burn mobile data downloading wallpapers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = config.wifiOnly,
                    onCheckedChange = { v -> update { it.copy(wifiOnly = v) } },
                )
            }

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            )
        }
    }
}

private val IntervalChoices = listOf(
    15L to "Every 15 minutes",
    30L to "Every 30 minutes",
    60L to "Every hour",
    360L to "Every 6 hours",
    720L to "Every 12 hours",
    1440L to "Every day",
)

private fun humanInterval(minutes: Long): String = when (minutes) {
    15L -> "Every 15 min"
    30L -> "Every 30 min"
    60L -> "Every hour"
    360L -> "Every 6h"
    720L -> "Every 12h"
    1440L -> "Every day"
    else -> "Every ${minutes} min"
}

private fun humanSource(source: AutoRotateSource): String = when (source) {
    AutoRotateSource.FAVORITES -> "Favorites"
    AutoRotateSource.FEATURED -> "Featured"
}

private fun humanTarget(target: ApplyTarget): String = when (target) {
    ApplyTarget.HOME -> "Home"
    ApplyTarget.LOCK -> "Lock"
    ApplyTarget.BOTH -> "Both"
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SingleChoiceRow(
    label: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
