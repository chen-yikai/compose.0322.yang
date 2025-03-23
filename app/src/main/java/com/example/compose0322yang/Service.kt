package com.example.compose0322yang

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Thread.State
import java.util.concurrent.Executors

class PlaybackService : MediaSessionService() {
    private var isForegroundStarted = false
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying

        private val _currentIndex = MutableStateFlow(0)
        val currentIndex: StateFlow<Int> = _currentIndex

        private val _duration = MutableStateFlow(0f)
        val duration: StateFlow<Float> = _duration

        private val _playerIns = MutableStateFlow<ExoPlayer?>(null)
        val playerIns: StateFlow<ExoPlayer?> = _playerIns

        var fav = mutableListOf<Int>()

        var playerInstance: ExoPlayer? = null

        fun init(items: List<MediaItem>) {
            playerInstance?.apply {
                setMediaItems(items)
                prepare()
                play()
            }
        }

        fun pauseAudio() {
            playerInstance?.pause()
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()
        playerInstance = player
        _playerIns.value = player
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                _currentIndex.value = player.currentMediaItemIndex
                _duration.value = player.duration.toFloat()
                super.onEvents(player, events)
            }

            override fun onIsPlayingChanged(state: Boolean) {
                _isPlaying.value = state
                super.onIsPlayingChanged(state)
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    return Futures.immediateFuture(mediaItems)
                }
            })
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!isForegroundStarted) {
            val notification = createNotification()
            startForeground(1, notification)
            isForegroundStarted = true
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        playerInstance = null
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "media_channel")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("title")
            .setContentText("text")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}