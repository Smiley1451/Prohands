package com.anand.prohands.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.navigation.BottomNavigation
import com.anand.prohands.navigation.NavGraph
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.AuthViewModel

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // Use the singleton instance from Application to avoid multiple EncryptedSharedPreferences instances
    val sessionManager = (context.applicationContext as ProHandsApplication).sessionManager
    val currentUserId = sessionManager.getUserId() ?: ""

    Scaffold(
        containerColor = ProColors.Background,
        bottomBar = {
            BottomBar(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) { // Apply the padding here
            NavGraph(
                navController = navController,
                currentUserId = currentUserId,
                authViewModel = authViewModel,
                onLogout = onLogout
            )
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController) {
    val screens = listOf(
        BottomNavigation.Home,
        BottomNavigation.Search,
        BottomNavigation.PostJob,
        BottomNavigation.Jobs,
        BottomNavigation.Profile
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = ProColors.Surface,
        contentColor = ProColors.Secondary
    ) {
        screens.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                label = { Text(text = screen.title) },
                icon = { Icon(imageVector = screen.icon, contentDescription = "Navigation icon") },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ProColors.OnPrimary,
                    selectedTextColor = ProColors.PrimaryVariant,
                    indicatorColor = ProColors.Primary,
                    unselectedIconColor = ProColors.TextTertiary,
                    unselectedTextColor = ProColors.TextTertiary
                )
            )
        }
    }
}
