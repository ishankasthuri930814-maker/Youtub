package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MediaItem
import com.example.data.Playlist
import com.example.data.StreamRepository
import com.example.media.MediaPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    BROWSER,
    PLAYLISTS,
    OFFLINE_LIBRARY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StreamRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = StreamRepository(db.mediaDao())
        
        // Seed default playlists if empty
        viewModelScope.launch {
            repository.allPlaylists.collect { list ->
                if (list.isEmpty()) {
                    repository.createPlaylist("Favorites", "My favorite tracks & videos", "#FF2E63")
                    repository.createPlaylist("Chill & Lo-Fi", "Relaxing background audio", "#00E5FF")
                    repository.createPlaylist("Workout Beats", "High energy streams", "#FF9F00")
                }
            }
        }
    }

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMediaItems: StateFlow<List<MediaItem>> = repository.allMediaItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedItems: StateFlow<List<MediaItem>> = repository.downloadedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Browser state
    private val _currentUrl = MutableStateFlow("https://m.youtube.com")
    val currentUrl = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("StreamTube")
    val currentTitle = _currentTitle.asStateFlow()

    private val _pageProgress = MutableStateFlow(0)
    val pageProgress = _pageProgress.asStateFlow()

    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode = _isDesktopMode.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(true)
    val isAdBlockEnabled = _isAdBlockEnabled.asStateFlow()

    private val _blockedAdsCount = MutableStateFlow(12)
    val blockedAdsCount = _blockedAdsCount.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentMediaTitle = MutableStateFlow("StreamTube Music")
    val currentMediaTitle = _currentMediaTitle.asStateFlow()

    private val _currentMediaUrl = MutableStateFlow("")
    val currentMediaUrl = _currentMediaUrl.asStateFlow()

    private val _isBackgroundPlayEnabled = MutableStateFlow(true)
    val isBackgroundPlayEnabled = _isBackgroundPlayEnabled.asStateFlow()

    // UI state
    private val _currentTab = MutableStateFlow(AppTab.BROWSER)
    val currentTab = _currentTab.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog = _showCreatePlaylistDialog.asStateFlow()

    private val _showAddToPlaylistDialog = MutableStateFlow(false)
    val showAddToPlaylistDialog = _showAddToPlaylistDialog.asStateFlow()

    var activeWebView: WebView? = null

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleDesktopMode() {
        _isDesktopMode.value = !_isDesktopMode.value
        // Reload current url with new user agent
        val url = _currentUrl.value
        if (url.contains("youtube.com")) {
            _currentUrl.value = if (_isDesktopMode.value) "https://www.youtube.com" else "https://m.youtube.com"
        }
    }

    fun toggleAdBlock() {
        _isAdBlockEnabled.value = !_isAdBlockEnabled.value
    }

    fun incrementAdBlockedCount() {
        _blockedAdsCount.value += 1
    }

    fun updateUrl(url: String) {
        _currentUrl.value = url
    }

    fun updateTitle(title: String) {
        _currentTitle.value = title
    }

    fun updateProgress(progress: Int) {
        _pageProgress.value = progress
    }

    fun navigateToUrl(inputUrl: String) {
        var formatted = inputUrl.trim()
        if (formatted.isBlank()) return

        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            if (formatted.contains(".") && !formatted.contains(" ")) {
                formatted = "https://$formatted"
            } else {
                // Search query on YouTube
                formatted = "https://m.youtube.com/results?search_query=" + java.net.URLEncoder.encode(formatted, "UTF-8")
            }
        }
        _currentUrl.value = formatted
        _currentTab.value = AppTab.BROWSER
    }

    fun onMediaStateChanged(playing: Boolean, title: String, url: String) {
        _isPlaying.value = playing
        if (title.isNotBlank()) _currentMediaTitle.value = title
        if (url.isNotBlank()) _currentMediaUrl.value = url

        if (_isBackgroundPlayEnabled.value) {
            val context = getApplication<Application>()
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = if (playing) MediaPlaybackService.ACTION_PLAY else MediaPlaybackService.ACTION_PAUSE
                putExtra(MediaPlaybackService.EXTRA_TITLE, _currentMediaTitle.value)
                putExtra(MediaPlaybackService.EXTRA_URL, _currentMediaUrl.value)
            }
            if (playing) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun togglePlayPause() {
        val newPlaying = !_isPlaying.value
        _isPlaying.value = newPlaying

        activeWebView?.evaluateJavascript(
            if (newPlaying) "document.querySelector('video')?.play();" else "document.querySelector('video')?.pause();",
            null
        )

        val context = getApplication<Application>()
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = if (newPlaying) MediaPlaybackService.ACTION_PLAY else MediaPlaybackService.ACTION_PAUSE
            putExtra(MediaPlaybackService.EXTRA_TITLE, _currentMediaTitle.value)
            putExtra(MediaPlaybackService.EXTRA_URL, _currentMediaUrl.value)
        }
        context.startService(intent)
    }

    fun toggleBackgroundPlay() {
        _isBackgroundPlayEnabled.value = !_isBackgroundPlayEnabled.value
    }

    // Playlist & Library Operations
    fun openCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = true
    }

    fun closeCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = false
    }

    fun createPlaylist(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            repository.createPlaylist(name, description, colorHex)
            closeCreatePlaylistDialog()
        }
    }

    fun openAddToPlaylistDialog() {
        _showAddToPlaylistDialog.value = true
    }

    fun closeAddToPlaylistDialog() {
        _showAddToPlaylistDialog.value = false
    }

    fun saveCurrentPageToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val item = MediaItem(
                title = _currentTitle.value,
                url = _currentUrl.value,
                thumbnailUrl = extractYouTubeThumbnail(_currentUrl.value),
                playlistId = playlistId,
                isDownloaded = false
            )
            repository.saveMediaItem(item)
            closeAddToPlaylistDialog()
        }
    }

    fun saveMediaItemToOffline(item: MediaItem) {
        viewModelScope.launch {
            val updated = item.copy(isDownloaded = true)
            repository.saveMediaItem(updated)
        }
    }

    // Download state
    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadingTitle = MutableStateFlow("")
    val downloadingTitle = _downloadingTitle.asStateFlow()

    // Offline Player State
    private var mediaPlayer: android.media.MediaPlayer? = null

    private val _offlinePlayingItem = MutableStateFlow<MediaItem?>(null)
    val offlinePlayingItem = _offlinePlayingItem.asStateFlow()

    private val _offlineIsPlaying = MutableStateFlow(false)
    val offlineIsPlaying = _offlineIsPlaying.asStateFlow()

    private val _offlinePositionMs = MutableStateFlow(0L)
    val offlinePositionMs = _offlinePositionMs.asStateFlow()

    private val _offlineDurationMs = MutableStateFlow(0L)
    val offlineDurationMs = _offlineDurationMs.asStateFlow()

    fun saveCurrentPageOffline() {
        viewModelScope.launch {
            val title = _currentTitle.value
            val url = _currentUrl.value
            _downloadingTitle.value = title
            _downloadProgress.value = 5

            val file = com.example.media.AudioDownloader.downloadAudioMp3(
                context = getApplication(),
                url = url,
                title = title,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )

            val item = MediaItem(
                title = title,
                url = url,
                thumbnailUrl = extractYouTubeThumbnail(url),
                isDownloaded = true,
                localFilePath = file?.absolutePath
            )
            repository.saveMediaItem(item)

            kotlinx.coroutines.delay(1000)
            _downloadProgress.value = null
        }
    }

    fun playOfflineMediaItem(item: MediaItem) {
        val path = item.localFilePath
        if (path != null && java.io.File(path).exists()) {
            try {
                mediaPlayer?.release()
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    start()
                }
                _offlinePlayingItem.value = item
                _offlineIsPlaying.value = true
                _offlineDurationMs.value = mediaPlayer?.duration?.toLong() ?: 0L
                startOfflineProgressTimer()
            } catch (e: Exception) {
                e.printStackTrace()
                navigateToUrl(item.url)
            }
        } else {
            navigateToUrl(item.url)
        }
    }

    fun toggleOfflinePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _offlineIsPlaying.value = false
            } else {
                player.start()
                _offlineIsPlaying.value = true
            }
        }
    }

    fun seekOfflineTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _offlinePositionMs.value = positionMs
        } catch (e: Exception) {}
    }

    private fun startOfflineProgressTimer() {
        viewModelScope.launch {
            while (_offlineIsPlaying.value && mediaPlayer != null) {
                try {
                    _offlinePositionMs.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            item.localFilePath?.let { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {}
            }
            if (_offlinePlayingItem.value?.id == item.id) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                _offlinePlayingItem.value = null
                _offlineIsPlaying.value = false
            }
            repository.deleteMediaItem(item)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }

    private fun extractYouTubeThumbnail(url: String): String {
        return try {
            if (url.contains("v=")) {
                val videoId = url.substringAfter("v=").substringBefore("&")
                "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
            } else if (url.contains("youtu.be/")) {
                val videoId = url.substringAfter("youtu.be/").substringBefore("?")
                "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
