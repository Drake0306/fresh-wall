package com.example.freshwall.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.freshwall.FreshWallApplication
import com.example.freshwall.data.ThemeMode

@Composable
fun ThemeScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val themePreferences = remember(context) {
        (context.applicationContext as FreshWallApplication).themePreferences
    }
    val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(title = "Theme", onBack = onBack)
            Spacer(Modifier.height(16.dp))

            ThemePreviewCard()

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Mode",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            ThemeModeRow(
                label = "Light",
                description = "Always light",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { themePreferences.setThemeMode(ThemeMode.LIGHT) },
            )
            ThemeModeRow(
                label = "Dark",
                description = "Always pitch-black dark",
                selected = themeMode == ThemeMode.DARK,
                onClick = { themePreferences.setThemeMode(ThemeMode.DARK) },
            )
            ThemeModeRow(
                label = "System default",
                description = "Follow your phone's theme",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { themePreferences.setThemeMode(ThemeMode.SYSTEM) },
            )

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            )
        }
    }
}

@Composable
private fun ThemePreviewCard() {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorSwatch(MaterialTheme.colorScheme.primary)
                ColorSwatch(MaterialTheme.colorScheme.secondary)
                ColorSwatch(MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Aurora",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Soft, drifting tones — a calm backdrop.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun ThemeModeRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            RadioButton(selected = selected, onClick = onClick)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
