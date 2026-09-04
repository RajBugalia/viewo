package com.example.viewo.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewo.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCampaignsScreen(viewModel: AdminViewModel) {
    val campaigns by viewModel.campaigns.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val screensList by viewModel.screens.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var campaignToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Campaign")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Campaigns", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && campaigns.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (campaigns.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No campaigns found. Click + to create one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = campaigns,
                            key = { it.id }
                        ) { campaign ->
                            CampaignCard(campaign, onLongClick = { campaignToDelete = campaign.id })
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var campaignName by remember { mutableStateOf("") }
        var expandedPlaylist by remember { mutableStateOf(false) }
        var selectedPlaylist by remember { mutableStateOf<com.example.viewo.model.Playlist?>(null) }
        val selectedScreens = remember { mutableStateListOf<String>() }
        val sdfDate = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
        val sdfTime = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
        var startDate by remember { mutableStateOf(sdfDate.format(java.util.Date())) }
        var startTime by remember { mutableStateOf(sdfTime.format(java.util.Date())) }
        var endDate by remember { mutableStateOf("2026-12-31") }
        var endTime by remember { mutableStateOf("23:59") }
        var orientation by remember { mutableStateOf("LANDSCAPE") }

        var showStartDatePicker by remember { mutableStateOf(false) }
        var showStartTimePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }
        var showEndTimePicker by remember { mutableStateOf(false) }

        if (showStartDatePicker) AppDatePicker(onDismissRequest = { showStartDatePicker = false }) { startDate = it; showStartDatePicker = false }
        if (showStartTimePicker) AppTimePicker(onDismissRequest = { showStartTimePicker = false }) { startTime = it; showStartTimePicker = false }
        if (showEndDatePicker) AppDatePicker(onDismissRequest = { showEndDatePicker = false }) { endDate = it; showEndDatePicker = false }
        if (showEndTimePicker) AppTimePicker(onDismissRequest = { showEndTimePicker = false }) { endTime = it; showEndTimePicker = false }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Campaign") },
            text = {
                Column {
                    OutlinedTextField(
                        value = campaignName,
                        onValueChange = { campaignName = it },
                        label = { Text("Campaign Name") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Playlist Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedPlaylist,
                        onExpandedChange = { expandedPlaylist = !expandedPlaylist }
                    ) {
                        OutlinedTextField(
                            value = selectedPlaylist?.name ?: "Select Playlist",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlaylist) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPlaylist,
                            onDismissRequest = { expandedPlaylist = false }
                        ) {
                            playlists.forEach { playlist ->
                                DropdownMenuItem(
                                    text = { Text(playlist.name) },
                                    onClick = {
                                        selectedPlaylist = playlist
                                        expandedPlaylist = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = startDate, onValueChange = {}, readOnly = true, label = { Text("Start Date") }, modifier = Modifier.fillMaxWidth())
                            Box(modifier = Modifier.matchParentSize().clickable { showStartDatePicker = true })
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = startTime, onValueChange = {}, readOnly = true, label = { Text("Start Time") }, modifier = Modifier.fillMaxWidth())
                            Box(modifier = Modifier.matchParentSize().clickable { showStartTimePicker = true })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = endDate, onValueChange = {}, readOnly = true, label = { Text("End Date") }, modifier = Modifier.fillMaxWidth())
                            Box(modifier = Modifier.matchParentSize().clickable { showEndDatePicker = true })
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = endTime, onValueChange = {}, readOnly = true, label = { Text("End Time") }, modifier = Modifier.fillMaxWidth())
                            Box(modifier = Modifier.matchParentSize().clickable { showEndTimePicker = true })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Orientation:")
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = orientation == "LANDSCAPE", onClick = { orientation = "LANDSCAPE" })
                        Text("Landscape")
                        RadioButton(selected = orientation == "PORTRAIT", onClick = { orientation = "PORTRAIT" })
                        Text("Portrait")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Target Screens:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Multi-select Screens
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(screensList) { screen ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedScreens.contains(screen.id),
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedScreens.add(screen.id)
                                        else selectedScreens.remove(screen.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(screen.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (campaignName.isNotBlank() && selectedPlaylist != null && selectedScreens.isNotEmpty()) {
                        val start = "${startDate}T${startTime}"
                        val end = "${endDate}T${endTime}"
                        viewModel.createCampaign(campaignName, "Created from app", selectedPlaylist!!.id, selectedScreens, start, end, orientation)
                        showCreateDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (campaignToDelete != null) {
        AlertDialog(
            onDismissRequest = { campaignToDelete = null },
            title = { Text("Delete Campaign") },
            text = { Text("Are you sure you want to delete this campaign?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCampaign(campaignToDelete!!)
                    campaignToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { campaignToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CampaignCard(campaign: com.example.viewo.model.Campaign, onLongClick: () -> Unit) {
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
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = campaign.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Badge(
                    containerColor = when (campaign.status) {
                        com.example.viewo.model.CampaignStatus.ACTIVE -> com.example.viewo.ui.theme.StatusOnline
                        com.example.viewo.model.CampaignStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
                        com.example.viewo.model.CampaignStatus.SCHEDULED -> com.example.viewo.ui.theme.StatusWarning
                        com.example.viewo.model.CampaignStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
                    }
                ) {
                    Text(
                        text = campaign.status.name,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${campaign.startDate} - ${campaign.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Screens: ${campaign.selectedScreens.size}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePicker(
    onDismissRequest: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    onDateSelected(sdf.format(java.util.Date(millis)))
                } else {
                    onDismissRequest()
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePicker(
    onDismissRequest: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                val hour = timePickerState.hour.toString().padStart(2, '0')
                val minute = timePickerState.minute.toString().padStart(2, '0')
                onTimeSelected("$hour:$minute")
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
