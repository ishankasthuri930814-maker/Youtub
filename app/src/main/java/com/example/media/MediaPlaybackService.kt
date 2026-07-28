package com.example.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class MediaPlaybackService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    var isPlaying: Boolean = false
        private set
    var currentTitle: String = "StreamTube Audio"
        private set
    var currentUrl: String = ""
        private set

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StreamTube:BackgroundAudioWakeLock")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initMediaSession()
    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSession(this, "StreamTubeMediaSession").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        isPlaying = true
                        startForeground(NOTIFICATION_ID, buildNotification(true, currentTitle))
                    }
                    override fun onPause() {
                        isPlaying = false
                        startForeground(NOTIFICATION_ID, buildNotification(false, currentTitle))
                    }
                    override fun onStop() {
                        isPlaying = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                })
                isActive = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { }
                    .build()
                audioFocusRequest = focusRequest
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMediaSessionState(playing: Boolean, title: String) {
        mediaSession?.let { session ->
            val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val playbackState = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP or PlaybackState.ACTION_PLAY_PAUSE)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
            session.setPlaybackState(playbackState)

            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "StreamTube Background Audio")
                .build()
            session.setMetadata(metadata)
            session.isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY -> {
                isPlaying = true
                currentTitle = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                currentUrl = intent.getStringExtra(EXTRA_URL) ?: currentUrl
                requestAudioFocus()
                acquireWakeLock()
                updateMediaSessionState(true, currentTitle)
                startForeground(NOTIFICATION_ID, buildNotification(isPlaying, currentTitle))
            }
            ACTION_PAUSE -> {
                isPlaying = false
                releaseWakeLock()
                updateMediaSessionState(false, currentTitle)
                startForeground(NOTIFICATION_ID, buildNotification(isPlaying, currentTitle))
            }
            ACTION_STOP -> {
                isPlaying = false
                abandonAudioFocus()
                releaseWakeLock()
                mediaSession?.isActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TOGGLE -> {
                isPlaying = !isPlaying
                if (isPlaying) {
                    requestAudioFocus()
                    acquireWakeLock()
                } else {
                    releaseWakeLock()
                }
                updateMediaSessionState(isPlaying, currentTitle)
                startForeground(NOTIFICATION_ID, buildNotification(isPlaying, currentTitle))
            }
        }
        return START_STICKY
    }

    fun updateState(playing: Boolean, title: String, url: String) {
        isPlaying = playing
        currentTitle = if (title.isBlank()) "StreamTube Audio" else title
        currentUrl = url
        if (playing) {
            requestAudioFocus()
            acquireWakeLock()
            updateMediaSessionState(true, currentTitle)
            startForeground(NOTIFICATION_ID, buildNotification(true, currentTitle))
        } else {
            releaseWakeLock()
            updateMediaSessionState(false, currentTitle)
            startForeground(NOTIFICATION_ID, buildNotification(false, currentTitle))
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(30 * 60 * 1000L /*30 mins max per acquisition*/)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StreamTube Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls background audio playback for YouTube & web media"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(playing: Boolean, title: String): Notification {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingActivityIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingToggleIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (playing) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Ad-Free Background Audio Active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingActivityIntent)
            .setOngoing(playing)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseIcon, playPauseText, pendingToggleIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStopIntent)
            .build()
    }

    override fun onDestroy() {
        abandonAudioFocus()
        releaseWakeLock()
        mediaSession?.release()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "stream_tube_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.media.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.media.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.media.ACTION_STOP"
        const val ACTION_TOGGLE = "com.example.media.ACTION_TOGGLE"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_URL = "extra_url"
    }
}
