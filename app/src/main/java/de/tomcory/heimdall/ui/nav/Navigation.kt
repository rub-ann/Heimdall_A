package de.tomcory.heimdall.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.tomcory.heimdall.ui.apps.AppsScreen
import de.tomcory.heimdall.ui.apps.PermissionsScreen
import de.tomcory.heimdall.ui.database.DatabaseScreen
import de.tomcory.heimdall.ui.home.HomeScreen

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavigationItem.Traffic.route) {
        composable(NavigationItem.Traffic.route) {
            HomeScreen()
        }
        composable(NavigationItem.Apps.route) {
            AppsScreen()
        }
        composable(NavigationItem.Database.route) {
            DatabaseScreen()
        }

        composable("permissions/{packageName}") {
            PermissionsScreen(navController, it.arguments?.getString("packageName"))
        }
    }
}