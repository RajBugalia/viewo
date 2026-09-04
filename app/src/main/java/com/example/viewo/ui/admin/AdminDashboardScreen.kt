package com.example.viewo.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.viewo.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel = hiltViewModel()) {
    val screens by viewModel.screens.collectAsState()
    val campaigns by viewModel.campaigns.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val onlineScreens = screens.count { it.status == com.example.viewo.model.ScreenStatus.ONLINE }
    val offlineScreens = screens.count { it.status == com.example.viewo.model.ScreenStatus.OFFLINE }
    val activeCampaigns = campaigns.count { it.status == com.example.viewo.model.CampaignStatus.ACTIVE }

    @OptIn(ExperimentalMaterial3Api::class)
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refreshData() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && screens.isEmpty() && campaigns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null && screens.isEmpty() && campaigns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(title = "Total Screens", value = screens.size.toString(), modifier = Modifier.weight(1f))
                        StatCard(title = "Active Campaigns", value = activeCampaigns.toString(), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(title = "Online", value = onlineScreens.toString(), modifier = Modifier.weight(1f), color = com.example.viewo.ui.theme.StatusOnline)
                        StatCard(title = "Offline", value = offlineScreens.toString(), modifier = Modifier.weight(1f), color = com.example.viewo.ui.theme.StatusOffline)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recent Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ActivityItem("Screen Main Entrance came online")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ActivityItem("Summer Sale campaign published")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ActivityItem("Advertisement completed on Screen 02")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(text = value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ActivityItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.DesktopMac, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
