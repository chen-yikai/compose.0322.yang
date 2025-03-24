package com.example.compose0322yang

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.transition.Slide
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import kotlinx.coroutines.launch
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

@Preview
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(context: Context = LocalContext.current) {
    var data by remember { mutableStateOf<List<Music>>(emptyList()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val playerState by PlaybackService.playerState.collectAsState()

    var favList by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showList by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }

    var scope = rememberCoroutineScope()

    var pagerState: PagerState = rememberPagerState(initialPage = 0) { data.size }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request =
                    Request.Builder().url("https://skills-music-api-v2.eliaschen.dev/sounds")
                        .build()
                data = client.newCall(request).execute().use {
                    Gson().fromJson(
                        it.body?.string(), object : TypeToken<List<Music>>() {}.type
                    )
                }
            }
            val mediaItems: MutableList<MediaItem> = mutableListOf()
            data.forEachIndexed { index, item ->
                mediaItems.add(
                    MediaItem.Builder().setUri("$host${item.audio}").setMediaMetadata(
                        MediaMetadata.Builder().setTitle(item.name)
                            .setDescription(item.description)
                            .setArtworkUri(Uri.parse("$host${item.cover}")).build()
                    ).build()
                )
            }
            PlaybackService.init(mediaItems)
        } catch (e: Exception) {

        }
    }

    if (showConfig) {
        var selectedItem by remember { mutableStateOf(playerState.repeatMode) }
        var itemList = listOf("Off", "Once", "All")
        LaunchedEffect(selectedItem) {
            PlaybackService.setRepeat(
                when (selectedItem) {
                    0 -> Player.REPEAT_MODE_OFF
                    1 -> Player.REPEAT_MODE_ONE
                    2 -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
            )
        }
        Dialog(onDismissRequest = { showConfig = false }) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Slider(value = playerState.volume, onValueChange = {
                    PlaybackService.adjustVolume(it)
                }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    itemList.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = index == selectedItem, onClick = {
                                selectedItem = index
                            }, shape = SegmentedButtonDefaults.itemShape(
                                count = itemList.size,
                                index = index,
                            )
                        ) {
                            Text(item)
                        }
                    }
                }
                FilledTonalButton(onClick = {
                    PlaybackService.setSpeed(2.0f)
                }) { Text("Speed up") }
            }
        }
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

    LaunchedEffect(pagerState.currentPage) {
        PlaybackService.seekToMediaIndex(pagerState.currentPage)
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
            IconButton(onClick = {
                showConfig = true
            }) { Icon(Icons.Default.Settings, contentDescription = "") }
            IconButton(onClick = { if (!favList.contains(playerState.currentIndex)) favList += playerState.currentIndex }) {
                Icon(
                    painter = painterResource(R.drawable.baseline_playlist_add_24),
                    contentDescription = ""
                )
            }
        }
        if (data.isNotEmpty()) {
            LaunchedEffect(playerState.currentIndex) {
                bitmap = null
                withContext(Dispatchers.IO) {
                    bitmap = URL("$host${data[playerState.currentIndex].cover}").openStream().use {
                        BitmapFactory.decodeStream(it) ?: null
                    }
                }
            }
            Column(
                Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(state = pagerState) { currentPage ->
                    Column(
                        Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (bitmap != null) {
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "",
                                    Modifier
                                        .size(350.dp)
                                        .clip(
                                            RoundedCornerShape(20.dp)
                                        )
                                )
                            }
                        } else {
                            Box(Modifier.size(350.dp)) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    data[playerState.currentIndex].name,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))
                if (playerState.duration.toFloat() > 0f) Slider(
                    value = playerState.currentPosition.toFloat(),
                    onValueChange = {
                        PlaybackService.seekTo(it.toLong())
                    },
                    valueRange = 0f..playerState.duration.toFloat(),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(timeFormat(playerState.currentPosition))
                    Text(timeFormat(playerState.duration))
                }
                Spacer(Modifier.height(30.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }, modifier = Modifier.size(60.dp)) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.baseline_skip_previous_24),
                            contentDescription = ""
                        )
                    }
                    IconButton(onClick = {
                        PlaybackService.togglePlayPause()
                    }, modifier = Modifier.size(60.dp)) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(if (playerState.isPlaying) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24),
                            contentDescription = ""
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }, modifier = Modifier.size(60.dp)) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.baseline_skip_next_24),
                            contentDescription = ""
                        )
                    }
                }
            }
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

fun timeFormat(input: Long): String {
    val second = input / 1000
    val mm = second / 60
    val ss = second % 60
    return "${mm.toString().padStart(2, '0')}:${ss.toString().padStart(2, '0')}"
}
