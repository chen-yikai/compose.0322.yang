package com.example.compose0322yang

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text

class Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                GlanceModifier.fillMaxSize().background(Color.White),
                horizontalAlignment = androidx.glance.layout.Alignment.CenterHorizontally,
                verticalAlignment = androidx.glance.layout.Alignment.CenterVertically
            ) {
                var list by remember { mutableStateOf<List<Int>>(emptyList()) }
                val playerState by PlaybackService.playerState.collectAsState()
                Text(playerState.metadata.title.toString())
                Row {
                    Button(
                        text = "prev",
                        onClick = { PlaybackService.seekToPrevious() })
                    CircleIconButton(
                        imageProvider = ImageProvider(if (playerState.isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24),
                        contentDescription = "",
                        onClick = {
                            PlaybackService.togglePlayPause()
                        })
                    Button(
                        text = "next",
                        onClick = { PlaybackService.seekToNext() })
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