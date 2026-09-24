package com.example.viewo.ui.player

import android.net.Uri
import androidx.compose.foundation.layout.*
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
import com.example.viewo.model.Campaign
import com.example.viewo.model.Media
import com.example.viewo.model.MediaType
import com.example.viewo.model.Playlist
import com.example.viewo.model.PlayerAssignment
import kotlinx.coroutines.delay

@Composable
fun PlayingScreen(
    assignment: PlayerAssignment,
    onLogProofOfPlay: (String, String, Int, String) -> Unit = { _, _, _, _ -> }
) {
    val campaign = assignment.campaign

    // Check if campaign is configured for Multiple Screen (Split Grid)
    if (campaign?.layoutType == "SPLIT" && !campaign.zones.isNullOrEmpty()) {
        SplitScreenPlaying(
            campaign = campaign,
            onLogProofOfPlay = onLogProofOfPlay
        )
        return
    }

    // Default: Single Screen Mode
    val mediaItems = assignment.playlist?.mediaItems ?: emptyList()
    if (mediaItems.isEmpty()) {
        WaitingScreen()
        return
    }

    ZonePlayer(
        campaignId = campaign?.id ?: "",
        playlist = assignment.playlist!!,
        modifier = Modifier.fillMaxSize(),
        onLogProofOfPlay = onLogProofOfPlay
    )
}

/**
 * Split Screen Grid Player
 * Renders multiple concurrent ad zones arranged in rows and columns
 */
@Composable
fun SplitScreenPlaying(
    campaign: Campaign,
    onLogProofOfPlay: (String, String, Int, String) -> Unit
) {
    val rows = (campaign.splitRows ?: 1).coerceAtLeast(1)
    val cols = (campaign.splitCols ?: 1).coerceAtLeast(1)
    val zones = campaign.zones ?: emptyList()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (c in 0 until cols) {
                    val zone = zones.firstOrNull { it.row == r && it.col == c }
                        ?: zones.firstOrNull { it.zoneIndex == (r * cols + c) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        if (zone?.playlist != null && zone.playlist.mediaItems.isNotEmpty()) {
                            ZonePlayer(
                                campaignId = campaign.id,
                                playlist = zone.playlist,
                                modifier = Modifier.fillMaxSize(),
                                onLogProofOfPlay = onLogProofOfPlay
                            )
                        } else {
                            WaitingScreen()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Zone Player
 * Cycles through its playlist independently, playing images or videos
 */
@Composable
fun ZonePlayer(
    campaignId: String,
    playlist: Playlist,
    modifier: Modifier = Modifier,
    onLogProofOfPlay: (String, String, Int, String) -> Unit
) {
    val mediaItems = playlist.mediaItems
    if (mediaItems.isEmpty()) return

    var currentIndex by remember { mutableStateOf(0) }
    val currentMedia = mediaItems[currentIndex]
    val defaultDuration = playlist.defaultDurationSeconds

    fun advanceNext() {
        val duration = currentMedia.durationSeconds ?: defaultDuration
        onLogProofOfPlay(campaignId, currentMedia.id, duration, "COMPLETED")
        currentIndex = (currentIndex + 1) % mediaItems.size
    }

    androidx.compose.animation.Crossfade(
        targetState = currentMedia, 
        modifier = modifier, 
        label = "zone_media_crossfade"
    ) { media ->
        if (media.type == MediaType.IMAGE) {
            ImagePlayer(media = media, defaultDuration = defaultDuration, onFinished = { advanceNext() })
        } else if (media.type == MediaType.VIDEO) {
            VideoPlayer(media = media, onFinished = { advanceNext() })
        }
    }
}

@Composable
fun ImagePlayer(media: Media, defaultDuration: Int = 10, onFinished: () -> Unit) {
    val duration = (media.durationSeconds ?: defaultDuration) * 1000L

    LaunchedEffect(media.id) {
        delay(duration)
        onFinished()
    }

    AsyncImage(
        model = com.example.viewo.Constants.getDynamicUrl(media.url),
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
        val mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(com.example.viewo.Constants.getDynamicUrl(media.url)))
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
                useController = false
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
