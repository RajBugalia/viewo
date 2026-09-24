package com.example.viewo.ui.player

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.viewo.viewmodel.PlayerState
import com.example.viewo.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.playerState.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val currentState = state) {
                is PlayerState.Unpaired -> PairingScreen(pairingCode)
                is PlayerState.WaitingForCampaign -> WaitingScreen()
                is PlayerState.Downloading -> DownloadingScreen()
                is PlayerState.Playing -> PlayingScreen(
                    assignment = currentState.assignment,
                    onLogProofOfPlay = viewModel::logProofOfPlay
                )
            }

            // Wi-Fi Icon overlay on the top right
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Wi-Fi Settings",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) // Semi-transparent so it's not too intrusive
                )
            }

            // Settings Dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Device & Sync Mode Settings") },
                    text = {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            val activeState = state
                            val syncModeText = if (activeState is PlayerState.Playing) {
                                val camp = activeState.assignment.campaign
                                if (camp?.layoutType == "SPLIT") {
                                    "Multi-Ad Split Screen (${camp.splitRows ?: 1} Rows × ${camp.splitCols ?: 1} Cols)"
                                } else {
                                    "Single Screen (100% Fullscreen)"
                                }
                            } else {
                                "Single Screen (Standard)"
                            }

                            Text("Device Pairing Code: $pairingCode", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Active Sync Mode: $syncModeText", color = MaterialTheme.colorScheme.primary)
                            Text("Open Wi-Fi / Network settings to configure device connection:", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showSettingsDialog = false
                                try {
                                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e3: Exception) {
                                            // Toast removed per user requirements
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Open Wi-Fi Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showSettingsDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
