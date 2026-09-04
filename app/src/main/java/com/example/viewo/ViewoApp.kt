package com.example.viewo

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.viewo.ui.admin.AdminScaffold
import com.example.viewo.ui.auth.RoleSelectionScreen
import com.example.viewo.ui.player.PlayerScreen

import com.example.viewo.ui.splash.SplashScreen

@Composable
fun ViewoApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("role_selection") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("role_selection") {
            RoleSelectionScreen(
                onAdminSelected = { navController.navigate("admin") },
                onPlayerSelected = { navController.navigate("player") }
            )
        }
        composable("admin") {
            AdminScaffold()
        }
        composable("player") {
            PlayerScreen()
        }
    }
}
