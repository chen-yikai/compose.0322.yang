package com.example.compose0322yang

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.transition.Slide
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.Executors

const val host = "https://skills-music-api-v2.eliaschen.dev"

data class Music(
    val id: Int,
    val name: String,
    val description: String,
    val tags: List<String>,
    val audio: String,
    val cover: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(context: Context) {
    var data by remember { mutableStateOf<List<Music>>(emptyList()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val isPlaying by PlaybackService.isPlaying.collectAsState()
    val currentIndex by PlaybackService.currentIndex.collectAsState()
    var player by remember { mutableStateOf<Player?>(null) }

    var favList by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showList by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val sessionToken =
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controllerFuture =
                MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener(
                {
                    player = controllerFuture.get()
                },
                Executors.newSingleThreadExecutor()
            )
            delay(500L)
        }
    }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request =
                Request.Builder().url("https://skills-music-api-v2.eliaschen.dev/sounds").build()
            data = client.newCall(request).execute().use {
                Gson().fromJson(it.body?.string(), object : TypeToken<List<Music>>() {}.type)
            }
        }
        val mediaItems: MutableList<MediaItem> = mutableListOf()
        data.forEachIndexed { index, item ->
            mediaItems.add(
                MediaItem.Builder().setUri("$host${item.audio}")
                    .setMediaMetadata(
                        MediaMetadata.Builder().setTitle(item.name).setDescription(item.description)
                            .setArtworkUri(Uri.parse("$host${item.cover}")).build()
                    ).build()
            )
        }
        PlaybackService.init(mediaItems)
    }

    if (showList) {
        ModalBottomSheet(onDismissRequest = { showList = false }) {
            LazyColumn(Modifier.padding(horizontal = 20.dp)) {
                item {
                    Text("My List", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                items(favList) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                        Text(data[it].name)
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { showList = true }) {
                Icon(
                    painter = painterResource(R.drawable.baseline_format_list_bulleted_24),
                    contentDescription = ""
                )
            }
            IconButton(onClick = { if (!favList.contains(currentIndex)) favList += currentIndex }) {
                Icon(
                    painter = painterResource(R.drawable.baseline_playlist_add_24),
                    contentDescription = ""
                )
            }
        }
        if (data.isNotEmpty()) {
            LaunchedEffect(player, Unit) {
                withContext(Dispatchers.IO) {
                    bitmap =
                        URL("$host${data[currentIndex].cover}").openStream()
                            .use {
                                BitmapFactory.decodeStream(it) ?: null
                            }
                }
            }
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "",
                        Modifier
                            .padding(horizontal = 40.dp)
                            .clip(
                                RoundedCornerShape(20.dp)
                            )
                    )

                }
                Spacer(Modifier.height(20.dp))
                Text(
                    data[currentIndex].name,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))
                if (player?.duration != null)
                    player?.currentPosition?.toFloat()?.let {
                        Slider(
                            value = it,
                            onValueChange = {
                                player?.seekTo(it.toLong())
                            },
                            valueRange = 0f..40000f,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                Spacer(Modifier.height(30.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = {
                        player?.seekToPreviousMediaItem()
                    }, modifier = Modifier.size(60.dp)) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.baseline_skip_previous_24),
                            contentDescription = ""
                        )
                    }
                    IconButton(onClick = {
                        if (isPlaying) {
                            player?.pause()
                        } else {
                            player?.play()
                        }
                    }, modifier = Modifier.size(60.dp)) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24),
                            contentDescription = ""
                        )
                    }
                    IconButton(onClick = {
                        player?.seekToNextMediaItem()
                    }, modifier = Modifier.size(60.dp)) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.baseline_skip_next_24),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
}

