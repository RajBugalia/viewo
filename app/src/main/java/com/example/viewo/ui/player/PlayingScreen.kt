package com.example.viewo.ui.player

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.viewo.model.Media
import com.example.viewo.model.MediaType
import com.example.viewo.model.PlayerAssignment
import kotlinx.coroutines.delay

@Composable
fun PlayingScreen(
    assignment: PlayerAssignment,
    onLogProofOfPlay: (String, String, Int, String) -> Unit = { _, _, _, _ -> }
) {
    val mediaItems = assignment.playlist?.mediaItems ?: emptyList()
    if (mediaItems.isEmpty()) {
        WaitingScreen()
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    val currentMedia = mediaItems[currentIndex]
    val defaultDuration = assignment.playlist?.defaultDurationSeconds ?: 10

    fun advanceNext() {
        val duration = currentMedia.durationSeconds ?: defaultDuration
        onLogProofOfPlay(assignment.campaign?.id ?: "", currentMedia.id, duration, "COMPLETED")
        currentIndex = (currentIndex + 1) % mediaItems.size
    }

    androidx.compose.animation.Crossfade(targetState = currentMedia, modifier = Modifier.fillMaxSize(), label = "media_crossfade") { media ->
        if (media.type == MediaType.IMAGE) {
            ImagePlayer(media = media, defaultDuration = defaultDuration, onFinished = { advanceNext() })
        } else if (media.type == MediaType.VIDEO) {
            VideoPlayer(media = media, onFinished = { advanceNext() })
        }
    }
}

@Composable
fun ImagePlayer(media: Media, defaultDuration: Int = 10, onFinished: () -> Unit) {
    val duration = (media.durationSeconds ?: defaultDuration) * 1000L // default if null

    LaunchedEffect(media.id) {
        delay(duration)
        onFinished()
    }

    AsyncImage(
        model = media.url,
        contentDescription = media.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun VideoPlayer(media: Media, onFinished: () -> Unit) {
    val context = LocalContext.current
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onFinished()
                    }
                }
            })
        }
    }

    LaunchedEffect(media.url) {
        val mediaItem = MediaItem.fromUri(Uri.parse(media.url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false // Hide controls for digital signage
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
