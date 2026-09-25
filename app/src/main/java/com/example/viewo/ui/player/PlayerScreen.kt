package com.example.viewo.ui.player

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val syncMode by viewModel.deviceSyncMode.collectAsState()
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
                is PlayerState.Playing -> {
                    // Auto-launch GridPlayerActivity if campaign is SPLIT
                    LaunchedEffect(currentState.assignment.campaign?.id, currentState.assignment.campaign?.layoutType) {
                        if (currentState.assignment.campaign?.layoutType == "SPLIT") {
                            val intent = Intent(context, GridPlayerActivity::class.java)
                            context.startActivity(intent)
                        }
                    }

                    PlayingScreen(
                        assignment = currentState.assignment,
                        deviceSyncMode = syncMode,
                        onLogProofOfPlay = viewModel::logProofOfPlay
                    )
                }
            }

            // Top Center Pill to open Dedicated Grid Player
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clickable {
                        val intent = Intent(context, GridPlayerActivity::class.java)
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Grid Player Activity",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            // Settings Dialog
            if (showSettingsDialog) {
                var codeInput by remember { mutableStateOf(pairingCode) }
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Device & Grid Player Settings") },
                    text = {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    showSettingsDialog = false
                                    val intent = Intent(context, GridPlayerActivity::class.java)
                                    context.startActivity(intent)
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Launch Grid Player Activity", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            androidx.compose.material3.OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it.uppercase() },
                                label = { Text("Device Pairing Code") },
                                trailingIcon = {
                                    TextButton(onClick = { viewModel.setPairingCode(codeInput) }) {
                                        Text("Save")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Sync Mode / Split Screen:", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)

                            val syncModes = listOf(
                                com.example.viewo.model.DeviceSyncMode.AUTO to "Auto (Web Campaign Driven)",
                                com.example.viewo.model.DeviceSyncMode.SPLIT_1X2 to "Split 1 × 2 (2 Columns)",
                                com.example.viewo.model.DeviceSyncMode.SPLIT_2X1 to "Split 2 × 1 (2 Rows)",
                                com.example.viewo.model.DeviceSyncMode.SPLIT_2X2 to "Split 2 × 2 (4 Quadrants)",
                                com.example.viewo.model.DeviceSyncMode.SINGLE to "Single Screen (100% Fullscreen)"
                            )

                            syncModes.forEach { (mode, label) ->
                                androidx.compose.material3.FilterChip(
                                    selected = (syncMode == mode),
                                    onClick = { viewModel.setDeviceSyncMode(mode) },
                                    label = { Text(label) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text("Network / System Settings:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
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
