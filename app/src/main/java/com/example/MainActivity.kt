package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.BottomNavBar
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.OfflineLibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.StreamTubeTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            StreamTubeTheme(darkTheme = isDarkMode) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val pageProgress by viewModel.pageProgress.collectAsState()
    val isDesktopMode by viewModel.isDesktopMode.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()
    val blockedCount by viewModel.blockedAdsCount.collectAsState()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMediaTitle by viewModel.currentMediaTitle.collectAsState()

    val playlists by viewModel.playlists.collectAsState()
    val showCreateDialog by viewModel.showCreatePlaylistDialog.collectAsState()
    val showAddDialog by viewModel.showAddToPlaylistDialog.collectAsState()

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.closeCreatePlaylistDialog() },
            onCreate = { name, desc, color ->
                viewModel.createPlaylist(name, desc, color)
            }
        )
    }

    if (showAddDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { viewModel.closeAddToPlaylistDialog() },
            onSelectPlaylist = { playlistId ->
                viewModel.saveCurrentPageToPlaylist(playlistId)
            },
            onCreateNewPlaylist = {
                viewModel.openCreatePlaylistDialog()
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Persistent Mini Audio Player
                MiniPlayerBar(
                    title = currentMediaTitle,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                    onAddToPlaylist = { viewModel.openAddToPlaylistDialog() },
                    onSaveOffline = { viewModel.saveCurrentPageOffline() }
                )

                // Navigation Bar
                BottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.BROWSER -> BrowserScreen(
                    viewModel = viewModel,
                    url = currentUrl,
                    title = currentTitle,
                    progress = pageProgress,
                    isDesktopMode = isDesktopMode,
                    isAdBlockEnabled = isAdBlockEnabled,
                    blockedCount = blockedCount,
                    isPlaying = isPlaying,
                    onNavigate = { viewModel.navigateToUrl(it) }
                )

                AppTab.PLAYLISTS -> PlaylistsScreen(
                    viewModel = viewModel,
                    onPlayTrackUrl = { viewModel.navigateToUrl(it) }
                )

                AppTab.OFFLINE_LIBRARY -> OfflineLibraryScreen(
                    viewModel = viewModel,
                    onPlayTrackUrl = { viewModel.navigateToUrl(it) }
                )

                AppTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
