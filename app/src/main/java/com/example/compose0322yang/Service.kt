package com.example.compose0322yang

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.provider.MediaStore.Audio.Media
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
import androidx.glance.appwidget.updateAll
import androidx.media3.common.FlagSet
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
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
import kotlinx.coroutines.flow.isActive
import kotlinx.coroutines.launch
import java.lang.Thread.State
import java.util.concurrent.Executors

data class PlayerState(
    var currentPosition: Long = 0L,
    var duration: Long = 1000L,
    var isPlaying: Boolean = false,
    var volume: Float = 1.0f,
    var currentIndex: Int = 0,
    var repeatMode: Int = Player.REPEAT_MODE_OFF,
    var metadata: MediaMetadata = MediaMetadata.Builder().build(),
)

class PlaybackService : MediaSessionService() {
    private var isForegroundStarted = false
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerJob: Job? = null

    companion object {
        private val _playerState = MutableStateFlow(PlayerState())
        val playerState: StateFlow<PlayerState> = _playerState

        var playerInstance: ExoPlayer? = null

        fun init(items: List<MediaItem>) {
            playerInstance?.apply {
                setMediaItems(items)
                prepare()
                play()
            }
        }

        fun togglePlayPause() {
            playerInstance?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }

        fun seekTo(positionMs: Long) {
            playerInstance?.seekTo(positionMs)
        }

        fun seekToNext() {
            playerInstance?.seekToNextMediaItem()
        }

        fun seekToPrevious() {
            playerInstance?.seekToPreviousMediaItem()
        }

        fun adjustVolume(target: Float) {
            playerInstance?.volume?.coerceIn(0.0f, 1.0f)
            playerInstance?.volume = target
        }

        fun setRepeat(target: Int) {
            playerInstance?.let {
                it.repeatMode = target
            }
        }

        fun setSpeed(target: Float) {
            playerInstance?.let {
                it.playbackParameters = PlaybackParameters(target.coerceIn(0.1f..2.0f))
            }
        }

        fun seekToMediaIndex(index: Int) {
            playerInstance?.let {
                it.seekTo(index, 0)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        playerInstance = player
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                _playerState.value = _playerState.value.copy(
                    currentIndex = player.currentMediaItemIndex,
                    isPlaying = player.isPlaying,
                    duration = player.duration,
                    volume = player.volume,
                    repeatMode = player.repeatMode,
                    metadata = player.mediaMetadata
                )
                serviceScope.launch {
                    Widget().updateAll(applicationContext)
                }
                updatePosition()
                super.onEvents(player, events)
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
        startControllerConnection()
    }

    private fun startControllerConnection() {
        controllerJob?.cancel()
        controllerJob = CoroutineScope(Dispatchers.Main).launch {
            val sessionToken = SessionToken(
                this@PlaybackService,
                ComponentName(this@PlaybackService, PlaybackService::class.java)
            )
            MediaController.Builder(this@PlaybackService, sessionToken).buildAsync()
        }
    }

    private fun updatePosition() {
        serviceScope.launch {
            while (true) {
                _playerState.value =
                    _playerState.value.copy(currentPosition = player.currentPosition)
                if (!_playerState.value.isPlaying) break
                delay(500)
            }
        }
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
        return NotificationCompat.Builder(this, "media_channel")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("title")
            .setContentText("text")
            .setOngoing(true)
            .build()
    }
}