package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.AppTab

@Composable
fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.BROWSER,
            onClick = { onTabSelected(AppTab.BROWSER) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.BROWSER) Icons.Filled.Language else Icons.Outlined.Language,
                    contentDescription = "Browser"
                )
            },
            label = { Text("Browser") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_browser")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.PLAYLISTS,
            onClick = { onTabSelected(AppTab.PLAYLISTS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.PLAYLISTS) Icons.Filled.PlaylistPlay else Icons.Outlined.PlaylistPlay,
                    contentDescription = "Playlists"
                )
            },
            label = { Text("Playlists") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_playlists")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.OFFLINE_LIBRARY,
            onClick = { onTabSelected(AppTab.OFFLINE_LIBRARY) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.OFFLINE_LIBRARY) Icons.Filled.Download else Icons.Outlined.Download,
                    contentDescription = "Offline"
                )
            },
            label = { Text("Offline") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_offline")
        )

        NavigationBarItem(
            selected = currentTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag("tab_settings")
        )
    }
}
