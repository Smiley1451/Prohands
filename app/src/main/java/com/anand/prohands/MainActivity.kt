package com.anand.prohands

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anand.prohands.ui.screens.EditProfileScreen
import com.anand.prohands.ui.screens.ForgotPasswordScreen
import com.anand.prohands.ui.screens.LoginScreen
import com.anand.prohands.ui.screens.ProfileScreen
import com.anand.prohands.ui.screens.ResetPasswordScreen
import com.anand.prohands.ui.screens.SignUpScreen
import com.anand.prohands.ui.screens.VerifyAccountScreen
import com.anand.prohands.ui.screens.VerifyMfaScreen
import com.anand.prohands.ui.theme.ProHandsTheme
import com.anand.prohands.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProHandsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // Create the ViewModel at the Activity level to share it across screens
                    val authViewModel: AuthViewModel = viewModel()
                    
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToSignUp = { navController.navigate("signup") },
                                onNavigateToVerifyMfa = { navController.navigate("verify_mfa") },
                                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                                onLoginSuccess = {
                                    // Fetch profile immediately after login
                                    authViewModel.fetchProfile()
                                    navController.navigate("profile")
                                }
                            )
                        }
                        composable("signup") {
                            SignUpScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { navController.navigate("login") },
                                onNavigateToVerify = { navController.navigate("verify_account") }
                            )
                        }
                        composable("verify_account") {
                             VerifyAccountScreen(
                                 viewModel = authViewModel,
                                 onNavigateToLogin = { navController.navigate("login") }
                             )
                        }
                        composable("verify_mfa") {
                             VerifyMfaScreen(
                                 viewModel = authViewModel,
                                 onLoginSuccess = {
                                     // Fetch profile immediately after MFA
                                     authViewModel.fetchProfile()
                                     navController.navigate("profile")
                                 }
                             )
                        }
                        composable("forgot_password") {
                             ForgotPasswordScreen(
                                 viewModel = authViewModel,
                                 onNavigateToResetPassword = { navController.navigate("reset_password") }
                             )
                        }
                        composable("reset_password") {
                             ResetPasswordScreen(
                                 viewModel = authViewModel,
                                 onNavigateToLogin = { navController.navigate("login") }
                             )
                        }
                        composable("profile") {
                            // Pass the profile from the view model state
                            authViewModel.state.value.profile?.let { profile ->
                                ProfileScreen(
                                    profile = profile,
                                    onEditProfile = { navController.navigate("edit_profile") },
                                    onRefresh = { authViewModel.fetchProfile() }
                                )
                            }
                        }
                        composable("edit_profile") {
                            EditProfileScreen(
                                viewModel = authViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
