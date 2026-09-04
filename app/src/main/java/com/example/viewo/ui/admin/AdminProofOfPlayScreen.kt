package com.example.viewo.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.viewo.model.ProofOfPlayRecord
import com.example.viewo.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProofOfPlayScreen(viewModel: AdminViewModel) {
    val popRecords by viewModel.proofOfPlayRecords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proof of Play Logs") },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (popRecords.isEmpty()) {
                    Text(
                        text = "No proof of play records found. Swipe down to refresh.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    val groupedRecords = remember(popRecords) {
                        popRecords.groupBy { "${it.screenId}_${it.campaignId}_${it.mediaId}" }
                            .map { (_, records) ->
                                val latest = records.maxByOrNull { it.timestamp } ?: records.first()
                                val totalDuration = records.sumOf { it.durationSeconds }
                                val count = records.size
                                Triple(latest, totalDuration, count)
                            }
                            .sortedByDescending { it.first.timestamp }
                    }
                    LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groupedRecords) { (record, totalDuration, count) ->
                        PoPRecordCard(record, totalDuration, count)
                    }
                }
            }
        }
    }
}
}


@Composable
fun PoPRecordCard(record: ProofOfPlayRecord, totalDuration: Int, playCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Screen ID: ${record.screenId}", style = MaterialTheme.typography.titleMedium)
            Text("Campaign ID: ${record.campaignId}", style = MaterialTheme.typography.bodyMedium)
            Text("Media ID: ${record.mediaId}", style = MaterialTheme.typography.bodyMedium)
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val endDate = try { sdf.parse(record.timestamp) } catch(e: Exception) { null }
            if (endDate != null) {
                val startMillis = endDate.time - (record.durationSeconds * 1000L)
                val startStr = sdf.format(java.util.Date(startMillis))
                Text("Last Start: $startStr", style = MaterialTheme.typography.bodySmall)
                Text("Last End: ${record.timestamp}", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Last Time: ${record.timestamp}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Total Plays: $playCount", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Total Duration: $totalDuration seconds", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            
            Text("Status: ${record.status}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
