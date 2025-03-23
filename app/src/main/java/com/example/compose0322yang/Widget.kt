package com.example.compose0322yang

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.media3.common.Player

class Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                GlanceModifier.fillMaxSize().background(Color.White),
                horizontalAlignment = androidx.glance.layout.Alignment.CenterHorizontally,
                verticalAlignment = androidx.glance.layout.Alignment.CenterVertically
            ) {
                var player by remember { mutableStateOf<Player?>(null) }
                var list by remember { mutableStateOf<List<Int>>(emptyList()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        val sessionToken =
                            androidx.media3.session.SessionToken(
                                context,
                                android.content.ComponentName(
                                    context,
                                    com.example.compose0322yang.PlaybackService::class.java
                                )
                            )
                        val controllerFuture =
                            androidx.media3.session.MediaController.Builder(context, sessionToken)
                                .buildAsync()
                        controllerFuture.addListener(
                            {
                                player = controllerFuture.get()
                                list = PlaybackService.fav
                            },
                            java.util.concurrent.Executors.newSingleThreadExecutor()
                        )
                        kotlinx.coroutines.delay(500L)
                    }
                }

                Row() {
                    Button(
                        text = "prev",
                        onClick = { player?.seekToPreviousMediaItem() })
                    Button(
                        text = "play/pause",
                        onClick = { if (player?.isPlaying == true) player?.pause() else player?.play() })
                    Button(
                        text = "next",
                        onClick = { player?.seekToNextMediaItem() })
                }
                LazyColumn {
                    items(list) {
                        Text(it.toString())
                    }
                }
            }
        }
    }
}

class WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = Widget()
}