package com.example.viewo.ui.player

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.viewo.model.Campaign
import com.example.viewo.model.DeviceSyncMode
import com.example.viewo.model.PlayerAssignment
import com.example.viewo.model.Playlist
import com.example.viewo.ui.theme.ViewoTheme
import com.example.viewo.viewmodel.PlayerState
import com.example.viewo.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class GridPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on continuously for digital signage
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive sticky full screen
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
        )

        setContent {
            ViewoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    GridPlayerScreen(
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun GridPlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val state by viewModel.playerState.collectAsState()
    val syncMode by viewModel.deviceSyncMode.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()

    var customRows by remember { mutableIntStateOf(1) }
    var customCols by remember { mutableIntStateOf(2) }
    var userOverrodeGrid by remember { mutableStateOf(false) }

    var showControls by remember { mutableStateOf(true) }

    // Auto-hide controls after 5 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        when (val currentState = state) {
            is PlayerState.Playing -> {
                val campaign = currentState.assignment.campaign
                val defaultPlaylist = currentState.assignment.playlist

                // Determine active rows & cols
                val activeRows = if (userOverrodeGrid) customRows else {
                    when (syncMode) {
                        DeviceSyncMode.SPLIT_1X2 -> 1
                        DeviceSyncMode.SPLIT_2X1 -> 2
                        DeviceSyncMode.SPLIT_2X2 -> 2
                        else -> (campaign?.splitRows ?: 1).coerceAtLeast(1)
                    }
                }

                val activeCols = if (userOverrodeGrid) customCols else {
                    when (syncMode) {
                        DeviceSyncMode.SPLIT_1X2 -> 2
                        DeviceSyncMode.SPLIT_2X1 -> 1
                        DeviceSyncMode.SPLIT_2X2 -> 2
                        else -> (campaign?.splitCols ?: 2).coerceAtLeast(1)
                    }
                }

                val zones = campaign?.zones ?: emptyList()

                DedicatedGridContainer(
                    rows = activeRows,
                    cols = activeCols,
                    campaign = campaign,
                    defaultPlaylist = defaultPlaylist,
                    zones = zones,
                    onLogProofOfPlay = viewModel::logProofOfPlay
                )
            }
            is PlayerState.Downloading -> DownloadingScreen()
            is PlayerState.WaitingForCampaign -> WaitingScreen()
            is PlayerState.Unpaired -> PairingScreen(pairingCode)
        }

        // Overlay top controls for switching grid size and closing
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.85f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid Player",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Grid Multi-Screen Player [Code: $pairingCode]",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                customRows = 1
                                customCols = 2
                                userOverrodeGrid = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (customRows == 1 && customCols == 2 && userOverrodeGrid) MaterialTheme.colorScheme.primary else Color.DarkGray
                            )
                        ) {
                            Text("1x2 (Side-by-Side)", color = Color.White, fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                customRows = 2
                                customCols = 1
                                userOverrodeGrid = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (customRows == 2 && customCols == 1 && userOverrodeGrid) MaterialTheme.colorScheme.primary else Color.DarkGray
                            )
                        ) {
                            Text("2x1 (Stacked)", color = Color.White, fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                customRows = 2
                                customCols = 2
                                userOverrodeGrid = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (customRows == 2 && customCols == 2 && userOverrodeGrid) MaterialTheme.colorScheme.primary else Color.DarkGray
                            )
                        ) {
                            Text("2x2 (4-Grid)", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Single Screen", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders an N x M grid where every cell independently runs an ad player.
 */
@Composable
fun DedicatedGridContainer(
    rows: Int,
    cols: Int,
    campaign: Campaign?,
    defaultPlaylist: Playlist?,
    zones: List<com.example.viewo.model.ZoneAssignment>,
    onLogProofOfPlay: (String, String, Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (c in 0 until cols) {
                    val zIdx = r * cols + c

                    // Try to find matching zone from campaign
                    val matchingZone = zones.firstOrNull { it.row == r && it.col == c }
                        ?: zones.firstOrNull { it.zoneIndex == zIdx }

                    // Fallback to default playlist or zone playlist
                    val playlistForCell = matchingZone?.playlist ?: defaultPlaylist

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        if (playlistForCell != null && playlistForCell.mediaItems.isNotEmpty()) {
                            key(zIdx, playlistForCell.id, playlistForCell.mediaItems.size) {
                                ZonePlayer(
                                    campaignId = campaign?.id ?: "",
                                    playlist = playlistForCell,
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
