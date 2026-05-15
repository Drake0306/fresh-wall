@file:OptIn(coil.annotation.ExperimentalCoilApi::class)

package io.github.drake0306.freshwall.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import io.github.drake0306.freshwall.FreshWallApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onCategoriesClick: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FreshWallApplication }
    val sourceConfig by app.sourcePreferences.config.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var cacheSizeLabel by remember { mutableStateOf("…") }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    suspend fun refreshCacheSize() {
        val bytes = withContext(Dispatchers.IO) {
            context.imageLoader.diskCache?.size ?: 0L
        }
        cacheSizeLabel = formatBytes(bytes)
    }

    LaunchedEffect(Unit) { refreshCacheSize() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar(title = "Settings", onBack = onBack)
            Spacer(Modifier.height(4.dp))

            SettingsSectionHeader("Appearance")
            SettingsRow(
                icon = Icons.Outlined.Palette,
                label = "Theme",
                description = "Light, Dark, or System default",
                onClick = onThemeClick,
            )

            SettingsSectionHeader("Content")
            SettingsRow(
                icon = Icons.Outlined.Category,
                label = "Wallpaper preferences",
                description = "Edit the categories that drive your Pexels and Unsplash feeds",
                onClick = onCategoriesClick,
            )
            SettingsToggleRow(
                icon = Icons.Outlined.AutoAwesome,
                label = "Featured tab (experimental)",
                description = "Show the curated FreshWall collection alongside Pexels and Unsplash. " +
                    "Still being built — leave off for now if you'd rather stick to the partner sources.",
                checked = sourceConfig.featured,
                onCheckedChange = { checked ->
                    app.sourcePreferences.update { it.copy(featured = checked) }
                },
            )

            SettingsSectionHeader("Storage")
            SettingsRow(
                icon = Icons.Outlined.DeleteSweep,
                label = "Clear image cache",
                description = "Currently using $cacheSizeLabel",
                onClick = { showClearCacheConfirm = true },
            )

            SettingsSectionHeader("Feedback")
            SettingsRow(
                icon = Icons.Outlined.Forum,
                label = "Send feedback",
                description = "Report a bug or suggest an improvement",
                onClick = onFeedbackClick,
            )

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear image cache?") },
            text = {
                Text(
                    "This frees up $cacheSizeLabel. Wallpapers you've already seen will be " +
                        "re-downloaded the next time you open them.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheConfirm = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val loader = context.imageLoader
                                loader.memoryCache?.clear()
                                loader.diskCache?.clear()
                            }
                            refreshCacheSize()
                            Toast.makeText(
                                context,
                                "Cache cleared",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/**
 * Small section label that groups related rows in the Settings list. Matches
 * Material's category-header pattern: uppercase-ish small label in the
 * primary tint, sitting above its rows with consistent padding.
 */
@Composable
private fun SettingsSectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
