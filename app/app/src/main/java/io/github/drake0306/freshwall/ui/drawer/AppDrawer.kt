package io.github.drake0306.freshwall.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.drake0306.freshwall.util.rememberHaptics

enum class DrawerItem {
    FAVORITES,
    AUTO_ROTATE,
    DONATE,
    SETTINGS,
    ABOUT,
}

@Composable
fun AppDrawer(
    onItemClick: (DrawerItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "FreshWall",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Spacer(Modifier.height(8.dp))

        DrawerRow(Icons.Outlined.FavoriteBorder, "Favorites") { onItemClick(DrawerItem.FAVORITES) }
        DrawerRow(Icons.Outlined.AutoMode, "Auto-rotate") { onItemClick(DrawerItem.AUTO_ROTATE) }
        DrawerRow(Icons.Outlined.VolunteerActivism, "Donate") { onItemClick(DrawerItem.DONATE) }
        DrawerRow(Icons.Outlined.Settings, "Settings") { onItemClick(DrawerItem.SETTINGS) }
        DrawerRow(Icons.Outlined.Info, "About") { onItemClick(DrawerItem.ABOUT) }

        Spacer(
            Modifier.height(
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        )
    }
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    ListItem(
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.click()
                onClick()
            },
    )
}
