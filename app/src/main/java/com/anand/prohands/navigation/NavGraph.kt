package com.anand.prohands.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.anand.prohands.ui.screens.HomeScreen
import com.anand.prohands.ui.screens.MessagesScreen
import com.anand.prohands.ui.screens.PostJobScreen
import com.anand.prohands.ui.screens.ProfileScreen
import com.anand.prohands.ui.screens.SearchScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = BottomNavigation.Home.route
    ) {
        composable(BottomNavigation.Home.route) {
            HomeScreen()
        }
        composable(BottomNavigation.Search.route) {
            SearchScreen()
        }
        composable(BottomNavigation.PostJob.route) {
            PostJobScreen()
        }
        composable(BottomNavigation.Messages.route) {
            MessagesScreen()
        }
        composable(BottomNavigation.Profile.route) {
            ProfileScreen()
        }
    }
}