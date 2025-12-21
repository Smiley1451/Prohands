package com.anand.prohands.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anand.prohands.ui.screens.*
import com.anand.prohands.viewmodel.AuthViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    currentUserId: String,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavigation.Home.route
    ) {
        composable(BottomNavigation.Home.route) {
            HomeScreen(
                currentUserId = currentUserId, 
                onProfileClick = { navController.navigate(BottomNavigation.Profile.route) },
                onMessagesClick = { navController.navigate("messages/$currentUserId") },
                onNotificationsClick = { /* Navigate to notifications */ }
            )
        }
        composable(BottomNavigation.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(BottomNavigation.PostJob.route) {
            PostJobScreen(navController = navController, currentUserId = currentUserId)
        }
        composable(BottomNavigation.Jobs.route) {
            ManageJobsScreen(navController = navController, currentUserId = currentUserId)
        }
        composable(BottomNavigation.Profile.route) {
            val shouldRefresh = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("shouldRefresh")
            LaunchedEffect(shouldRefresh) {
                if (shouldRefresh == true) {
                    authViewModel.fetchProfile()
                    navController.currentBackStackEntry?.savedStateHandle?.set("shouldRefresh", false)
                }
            }
            ProfileScreen(
                userId = currentUserId,
                isReadOnly = false,
                onEditProfile = { navController.navigate("edit_profile/$currentUserId") },
                onLogout = onLogout,
                onJobClick = { jobId -> navController.navigate("job_details/$jobId") }
            )
        }
        composable("create_job") {
            PostJobScreen(navController = navController, currentUserId = currentUserId)
        }
        composable(
            "edit_job/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            EditJobScreen(navController = navController, jobId = jobId)
        }
        composable(
            "edit_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId") ?: ""
            EditProfileScreen(navController = navController, userId = userId)
        }
        composable(
            "worker_recommendations/{jobId}/{lat}/{lon}/{title}",
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lon") { type = NavType.FloatType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lon = backStackEntry.arguments?.getFloat("lon")?.toDouble() ?: 0.0
            val title = backStackEntry.arguments?.getString("title") ?: ""
            WorkerRecommendationsScreen(
                navController = navController,
                jobId = jobId,
                latitude = lat,
                longitude = lon,
                jobTitle = title
            )
        }
        composable(
            "worker_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId, 
                isReadOnly = true,
                onMessageClick = { navController.navigate("messages/$userId") }
            )
        }
        composable(
            "messages/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipientId = backStackEntry.arguments?.getString("userId") ?: ""
            MessagesScreen(
                currentUserId = currentUserId,
                recipientId = recipientId,
                navController = navController
            )
        }
        composable(
            "job_details/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            JobDetailsScreen(jobId = jobId, navController = navController)
        }
    }
}
