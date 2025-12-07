package com.anand.prohands.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anand.prohands.ui.screens.EditProfileScreen
import com.anand.prohands.ui.screens.HomeScreen
import com.anand.prohands.ui.screens.JobScreen
import com.anand.prohands.ui.screens.MessagesScreen
import com.anand.prohands.ui.screens.PostJobScreen
import com.anand.prohands.ui.screens.ProfileScreen
import com.anand.prohands.ui.screens.SearchScreen
import com.anand.prohands.viewmodel.AuthViewModel
import com.anand.prohands.viewmodel.AuthState

@Composable
fun NavGraph(
    navController: NavHostController,
    currentUserId: String,
    authState: AuthState,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
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
            JobScreen(currentUserId = currentUserId, onNavigateToCreateJob = { navController.navigate("create_job") })
        }
        composable(BottomNavigation.Messages.route) {
            MessagesScreen()
        }
        composable(BottomNavigation.Profile.route) {
            val shouldRefresh = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("shouldRefresh")
            LaunchedEffect(shouldRefresh) {
                if (shouldRefresh == true) {
                    onRefresh()
                    navController.currentBackStackEntry?.savedStateHandle?.set("shouldRefresh", false)
                }
            }
            ProfileScreen(
                authState = authState,
                onEditProfile = { navController.navigate("edit_profile/$currentUserId") },
                onRefresh = onRefresh,
                onLogout = onLogout
            )
        }
        composable("create_job") {
            PostJobScreen(navController = navController, currentUserId = currentUserId)
        }
        composable(
            "edit_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId") ?: ""
            EditProfileScreen(navController = navController, userId = userId)
        }
    }
}