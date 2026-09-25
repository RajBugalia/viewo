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
    deviceSyncMode: com.example.viewo.model.DeviceSyncMode = com.example.viewo.model.DeviceSyncMode.AUTO,
    onLogProofOfPlay: (String, String, Int, String) -> Unit = { _, _, _, _ -> }
) {
    val campaign = assignment.campaign
    val defaultPlaylist = assignment.playlist

    val isSplit = when (deviceSyncMode) {
        com.example.viewo.model.DeviceSyncMode.SPLIT_1X2,
        com.example.viewo.model.DeviceSyncMode.SPLIT_2X1,
        com.example.viewo.model.DeviceSyncMode.SPLIT_2X2 -> true
        com.example.viewo.model.DeviceSyncMode.SINGLE -> false
        com.example.viewo.model.DeviceSyncMode.AUTO -> campaign?.layoutType == "SPLIT"
    }

    if (isSplit) {
        val (rows, cols) = when (deviceSyncMode) {
            com.example.viewo.model.DeviceSyncMode.SPLIT_1X2 -> 1 to 2
            com.example.viewo.model.DeviceSyncMode.SPLIT_2X1 -> 2 to 1
            com.example.viewo.model.DeviceSyncMode.SPLIT_2X2 -> 2 to 2
            else -> {
                val r = (campaign?.splitRows ?: 1).coerceAtLeast(1)
                val c = (campaign?.splitCols ?: 2).coerceAtLeast(1)
                r to c
            }
        }
        val zones = campaign?.zones ?: emptyList()
        SplitScreenPlaying(
            campaign = campaign,
            defaultPlaylist = defaultPlaylist,
            rows = rows,
            cols = cols,
            zones = zones,
            onLogProofOfPlay = onLogProofOfPlay
        )
        return
    }

    // Default: Single Screen Mode
    val mediaItems = defaultPlaylist?.mediaItems ?: emptyList()
    if (mediaItems.isEmpty()) {
        WaitingScreen()
        return
    }

    ZonePlayer(
        campaignId = campaign?.id ?: "",
        playlist = defaultPlaylist!!,
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
    campaign: Campaign?,
    defaultPlaylist: Playlist?,
    rows: Int,
    cols: Int,
    zones: List<com.example.viewo.model.ZoneAssignment>,
    onLogProofOfPlay: (String, String, Int, String) -> Unit
) {
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
                    val zIdx = r * cols + c
                    val zone = zones.firstOrNull { it.row == r && it.col == c }
                        ?: zones.firstOrNull { it.zoneIndex == zIdx }

                    val activePlaylist = zone?.playlist ?: defaultPlaylist

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        if (activePlaylist != null && activePlaylist.mediaItems.isNotEmpty()) {
                            key(zIdx, activePlaylist.id) {
                                ZonePlayer(
                                    campaignId = campaign?.id ?: "",
                                    playlist = activePlaylist,
                                    initialIndex = zIdx,
                                    modifier = Modifier.fillMaxSize(),
                                    onLogProofOfPlay = onLogProofOfPlay
                                )
                            }
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
    initialIndex: Int = 0,
    modifier: Modifier = Modifier,
    onLogProofOfPlay: (String, String, Int, String) -> Unit
) {
    val mediaItems = playlist.mediaItems
    if (mediaItems.isEmpty()) return

    val startIndex = remember(playlist.id, mediaItems.size, initialIndex) {
        if (mediaItems.isNotEmpty()) initialIndex % mediaItems.size else 0
    }
    var currentIndex by remember(playlist.id, startIndex) { mutableStateOf(startIndex) }
    val currentMedia = mediaItems.getOrNull(currentIndex) ?: mediaItems[0]
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
