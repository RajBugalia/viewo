package com.example.viewo.ui.admin

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewo.viewmodel.AdminViewModel

sealed class AdminRoute(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : AdminRoute("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Campaigns : AdminRoute("campaigns", "Campaigns", Icons.Default.Campaign)
    object Media : AdminRoute("media", "Media", Icons.Default.PermMedia)
    object Playlists : AdminRoute("playlists", "Playlists", Icons.AutoMirrored.Filled.List)
    object Screens : AdminRoute("screens", "Screens", Icons.Default.Tv)
    object PoP : AdminRoute("pop", "PoP", Icons.AutoMirrored.Filled.FactCheck)
}

val adminScreens = listOf(
    AdminRoute.Dashboard,
    AdminRoute.Campaigns,
    AdminRoute.Media,
    AdminRoute.Playlists,
    AdminRoute.Screens,
    AdminRoute.PoP
)

@Composable
fun AdminScaffold() {
    val navController = rememberNavController()
    val adminViewModel: AdminViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val error by adminViewModel.error.collectAsState()

    androidx.compose.runtime.LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AdminBottomBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AdminRoute.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AdminRoute.Dashboard.route) { AdminDashboardScreen(adminViewModel) }
            composable(AdminRoute.Campaigns.route) { AdminCampaignsScreen(adminViewModel) }
            composable(AdminRoute.Media.route) { AdminMediaScreen(adminViewModel) }
            composable(AdminRoute.Playlists.route) { AdminPlaylistsScreen(adminViewModel) }
            composable(AdminRoute.Screens.route) { AdminScreensManagementScreen(adminViewModel) }
            composable(AdminRoute.PoP.route) { AdminProofOfPlayScreen(adminViewModel) }
        }
    }
}

@Composable
fun AdminBottomBar(navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        adminScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
