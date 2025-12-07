package com.anand.prohands

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anand.prohands.network.RetrofitClient
import com.anand.prohands.services.LocationForegroundService
import com.anand.prohands.ui.screens.*
import com.anand.prohands.ui.theme.ProHandsTheme
import com.anand.prohands.utils.SessionManager
import com.anand.prohands.viewmodel.AuthViewModel
import com.anand.prohands.viewmodel.AuthViewModelFactory

class MainActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        ) {
            startLocationService()
        } else {
            // Handle the case where the user denies the permission.
            // You might want to show a message to the user.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize RetrofitClient with Context
        RetrofitClient.init(applicationContext)

        val sessionManager = SessionManager(applicationContext)

        if (sessionManager.getAuthToken() != null) {
            requestLocationPermissions()
        }

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
                                    authViewModel.fetchProfile()
                                    requestLocationPermissions()
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
                                    authViewModel.fetchProfile()
                                    requestLocationPermissions()
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
                            MainScreen(
                                authViewModel = authViewModel,
                                onLogout = {
                                    authViewModel.logout()
                                    stopLocationService()
                                    navController.navigate("login") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startLocationService()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun startLocationService() {
        Intent(this, LocationForegroundService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun stopLocationService() {
        Intent(this, LocationForegroundService::class.java).also { intent ->
            stopService(intent)
        }
    }
}
