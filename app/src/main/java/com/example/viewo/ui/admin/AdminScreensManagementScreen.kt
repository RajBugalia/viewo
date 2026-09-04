package com.example.viewo.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewo.model.Screen
import com.example.viewo.model.ScreenStatus
import com.example.viewo.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreensManagementScreen(viewModel: AdminViewModel) {
    val screens by viewModel.screens.collectAsState()
    var showPairDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var pairingCode by remember { mutableStateOf("") }
    var screenName by remember { mutableStateOf("") }
    var screenToDelete by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()

    if (showScanner) {
        ScannerScreen(
            onQrCodeScanned = { code ->
                pairingCode = code
                showScanner = false
                showPairDialog = true
            },
            onCancel = {
                showScanner = false
                showPairDialog = true
            }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showPairDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Pair Screen")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Screens",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && screens.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (screens.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No screens found. Click + to pair one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = screens,
                            key = { it.id }
                        ) { screen ->
                            ScreenCard(screen, onLongClick = { screenToDelete = screen.id })
                        }
                    }
                }
            }
        }
    }

    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            title = { Text("Pair New Screen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it.uppercase() },
                        label = { Text("6-Digit Pairing Code") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = screenName,
                        onValueChange = { screenName = it },
                        label = { Text("Screen Name") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showPairDialog = false
                            showScanner = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR Code")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pairingCode.isNotBlank() && screenName.isNotBlank()) {
                        viewModel.pairScreen(pairingCode, screenName, "Lobby") // Hardcoded location for MVP
                        showPairDialog = false
                        pairingCode = ""
                        screenName = ""
                    }
                }) {
                    Text("Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPairDialog = false
                    pairingCode = ""
                    screenName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (screenToDelete != null) {
        AlertDialog(
            onDismissRequest = { screenToDelete = null },
            title = { Text("Unpair Screen") },
            text = { Text("Are you sure you want to unpair this screen?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteScreen(screenToDelete!!)
                    screenToDelete = null
                }) {
                    Text("Unpair", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { screenToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenCard(screen: Screen, onLongClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongClick)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = screen.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Badge(
                    containerColor = when (screen.status) {
                        ScreenStatus.ONLINE -> com.example.viewo.ui.theme.StatusOnline
                        ScreenStatus.OFFLINE -> com.example.viewo.ui.theme.StatusOffline
                        ScreenStatus.WARNING -> com.example.viewo.ui.theme.StatusWarning
                    }
                ) {
                    Text(
                        text = screen.status.name,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location: ${screen.location}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last Heartbeat: ${screen.lastHeartbeat}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (screen.currentCampaign != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Playing: ${screen.currentCampaign}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
