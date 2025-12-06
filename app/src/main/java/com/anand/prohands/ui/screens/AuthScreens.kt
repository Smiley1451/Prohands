package com.anand.prohands.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anand.prohands.viewmodel.AuthViewModel

// Colors based on description
val LightBlueBg = Color(0xFFE0F7FA) // Cyan-ish light blue
val GoldYellow = Color(0xFFFFD700)
val OrangeGold = Color(0xFFFFB300)

@Composable
fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBlueBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top Wave
            val pathTop = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, height * 0.2f)
                cubicTo(
                    width * 0.2f, height * 0.25f,
                    width * 0.5f, height * 0.15f,
                    width, height * 0.3f
                )
                lineTo(width, 0f)
                close()
            }
            drawPath(
                path = pathTop,
                brush = Brush.verticalGradient(
                    colors = listOf(GoldYellow, OrangeGold)
                )
            )

            // Bottom Wave
            val pathBottom = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.85f)
                cubicTo(
                    width * 0.4f, height * 0.75f,
                    width * 0.7f, height * 0.9f,
                    width, height * 0.8f
                )
                lineTo(width, height)
                close()
            }
            drawPath(
                path = pathBottom,
                brush = Brush.verticalGradient(
                    colors = listOf(OrangeGold, GoldYellow)
                )
            )
        }
        content()
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToSignUp: () -> Unit,
    onNavigateToVerifyMfa: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState() // **Fix: Use collectAsState for StateFlow**
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    LaunchedEffect(state.mfaRequired) {
        if (state.mfaRequired) {
            onNavigateToVerifyMfa()
            // State is preserved in ViewModel for verification
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusFlags() // Clear error after showing it
        }
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Login",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = OrangeGold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeGold) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onNavigateToForgotPassword() }) {
                    Text("Forgot password?", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    // STABILITY FIX: Validate input before calling VM
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(email, password)
                    } else {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues() 
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(colors = listOf(OrangeGold, GoldYellow))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("LOGIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = "Sign Up",
                    fontSize = 14.sp,
                    color = OrangeGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
        }

        // Removed the confusing loginSuccess && isLoading overlay logic. 
        // The main content's CircularProgressIndicator (inside the button) is sufficient.
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onNavigateToSignUp = {},
        onNavigateToVerifyMfa = {},
        onNavigateToForgotPassword = {},
        onLoginSuccess = {}
    )
}

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToVerify: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState() // **Fix: Use collectAsState for StateFlow**

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.signupSuccess) {
        if (state.signupSuccess) {
            Toast.makeText(context, "Sign Up Successful! Please Verify Account.", Toast.LENGTH_SHORT).show()
            onNavigateToVerify()
            // Do not reset state immediately so email is preserved
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusFlags() // Clear error after showing it
        }
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Create an account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Name") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangeGold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = OrangeGold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeGold) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeGold) },
                 trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // STABILITY FIX: Validate all fields are non-empty and passwords match
                    when {
                        username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                            Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                        }
                        password != confirmPassword -> {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            viewModel.signup(username, email, password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(colors = listOf(OrangeGold, GoldYellow))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SIGN UP", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = "Login",
                    fontSize = 14.sp,
                    color = OrangeGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(
        onNavigateToLogin = {},
        onNavigateToVerify = {}
    )
}

@Composable
fun VerifyAccountScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState() // **Fix: Use collectAsState for StateFlow**
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state.verifySuccess) {
        if (state.verifySuccess) {
            Toast.makeText(context, "Account Verified! Please Login.", Toast.LENGTH_SHORT).show()
            onNavigateToLogin()
            viewModel.resetState()
        }
    }
    
    LaunchedEffect(state.error) {
        state.error?.let {
             Toast.makeText(context, it, Toast.LENGTH_LONG).show()
             viewModel.clearStatusFlags() // Clear error after showing it
        }
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verify Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "Enter OTP sent to ${state.emailForVerification ?: "your email"}",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("OTP") },
                textStyle = TextStyle(color = Color.Black),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
             Button(
                onClick = { 
                    // STABILITY FIX: Validate OTP length
                    if (otp.length == 6) {
                        viewModel.verifyAccount(otp)
                    } else {
                        Toast.makeText(context, "OTP must be 6 digits", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues() 
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(colors = listOf(OrangeGold, GoldYellow))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("VERIFY", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerifyAccountScreenPreview() {
    VerifyAccountScreen(
        onNavigateToLogin = {}
    )
}

@Composable
fun VerifyMfaScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState() // **Fix: Use collectAsState for StateFlow**
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            Toast.makeText(context, "MFA Verified! Login Successful.", Toast.LENGTH_SHORT).show()
            onLoginSuccess()
            viewModel.resetState()
        }
    }
    
    LaunchedEffect(state.error) {
        state.error?.let {
             Toast.makeText(context, it, Toast.LENGTH_LONG).show()
             viewModel.clearStatusFlags() // Clear error after showing it
        }
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MFA Verification",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "Enter OTP sent to your email for MFA",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("OTP") },
                textStyle = TextStyle(color = Color.Black),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
             Button(
                onClick = { 
                    // STABILITY FIX: Validate OTP length
                    if (otp.length == 6) {
                        viewModel.verifyMfa(otp)
                    } else {
                        Toast.makeText(context, "OTP must be 6 digits", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues() 
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(colors = listOf(OrangeGold, GoldYellow))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("VERIFY MFA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToResetPassword: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState() // **Fix: Use collectAsState for StateFlow**
    var email by remember { mutableStateOf("") }

    LaunchedEffect(state.passwordResetRequestSuccess) {
        if (state.passwordResetRequestSuccess) {
             Toast.makeText(context, "OTP sent to email", Toast.LENGTH_SHORT).show()
            onNavigateToResetPassword()
        }
    }
    
    LaunchedEffect(state.error) {
        state.error?.let {
             Toast.makeText(context, it, Toast.LENGTH_LONG).show()
             viewModel.clearStatusFlags() // Clear error after showing it
        }
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Forgot Password",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = OrangeGold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
             Button(
                onClick = { 
                    // STABILITY FIX: Validate email is not empty
                    if (email.isNotBlank()) {
                         viewModel.requestPasswordReset(email)
                    } else {
                        Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues() 
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(colors = listOf(OrangeGold, GoldYellow))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SEND OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen(
        onNavigateToResetPassword = {}
    )
}

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState() // **Fix: Use collectAsState for StateFlow**
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }


    LaunchedEffect(state.passwordResetSuccess) {
        if (state.passwordResetSuccess) {
            Toast.makeText(context, "Password Reset Successful! Please Login.", Toast.LENGTH_SHORT).show()
            onNavigateToLogin()
            viewModel.resetState()
        }
    }
    
    LaunchedEffect(state.error) {
        state.error?.let {
             Toast.makeText(context, it, Toast.LENGTH_LONG).show()
             viewModel.clearStatusFlags() // Clear error after showing it
        }
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reset Password",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "Enter OTP and New Password",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("OTP") },
                textStyle = TextStyle(color = Color.Black),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                textStyle = TextStyle(color = Color.Black),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeGold) },
                 trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeGold,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = OrangeGold,
                    cursorColor = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
             Button(
                onClick = { 
                    // STABILITY FIX: Validate input is not empty and OTP length
                    when {
                        otp.length != 6 -> {
                            Toast.makeText(context, "OTP must be 6 digits", Toast.LENGTH_SHORT).show()
                        }
                        newPassword.isBlank() -> {
                            Toast.makeText(context, "New password cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            viewModel.resetPassword(otp, newPassword)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues() 
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(colors = listOf(OrangeGold, GoldYellow))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("RESET PASSWORD", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResetPasswordScreenPreview() {
    ResetPasswordScreen(
        onNavigateToLogin = {}
    )
}
