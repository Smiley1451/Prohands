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
import com.anand.prohands.network.RetrofitClient
import com.anand.prohands.ui.screens.ForgotPasswordScreen
import com.anand.prohands.ui.screens.LoginScreen
import com.anand.prohands.ui.screens.MainScreen
import com.anand.prohands.ui.screens.ResetPasswordScreen
import com.anand.prohands.ui.screens.SignUpScreen
import com.anand.prohands.ui.screens.VerifyAccountScreen
import com.anand.prohands.ui.screens.VerifyMfaScreen
import com.anand.prohands.ui.theme.ProHandsTheme
import com.anand.prohands.utils.SessionManager
import com.anand.prohands.viewmodel.AuthViewModel
import com.anand.prohands.viewmodel.AuthViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize RetrofitClient with Context
        RetrofitClient.init(applicationContext)

        // Create SessionManager
        val sessionManager = SessionManager(applicationContext)

        // Create Factory
        val viewModelFactory = AuthViewModelFactory(sessionManager)

        setContent {
            ProHandsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)

                    val startDestination = if (sessionManager.getAuthToken() != null) "main" else "login"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToSignUp = { navController.navigate("signup") },
                                onNavigateToVerifyMfa = { navController.navigate("verify_mfa") },
                                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                                onLoginSuccess = {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
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
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
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
                        composable("main") {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }
}