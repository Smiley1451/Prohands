package com.anand.prohands.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.*
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.IOException
import java.util.regex.Pattern

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val signupSuccess: Boolean = false,
    val verifySuccess: Boolean = false,
    val mfaRequired: Boolean = false,
    val passwordResetRequestSuccess: Boolean = false,
    val passwordResetSuccess: Boolean = false,
    val emailForVerification: String? = null,
    val profile: ClientProfileDto? = null,
    val userId: String? = null,
    val updateSuccess: Boolean = false,
    val uploadSuccess: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val _state = mutableStateOf(AuthState())
    val state: State<AuthState> = _state

    // Regex patterns based on backend validation
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )
    
    private val PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$"
    )

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, loginSuccess = false, mfaRequired = false)
            try {
                val response = RetrofitClient.authService.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.mfaRequired) {
                        _state.value = _state.value.copy(isLoading = false, mfaRequired = true, emailForVerification = email)
                    } else {
                        // Store tokens and userId
                        _state.value = _state.value.copy(
                            isLoading = false, 
                            loginSuccess = true,
                            userId = body.userId
                        )
                    }
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun verifyMfa(otp: String) {
        val email = _state.value.emailForVerification ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, loginSuccess = false)
            try {
                val response = RetrofitClient.authService.verifyMfa(MfaRequest(email, otp))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    _state.value = _state.value.copy(
                        isLoading = false, 
                        loginSuccess = true,
                        userId = body.userId
                    )
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun signup(username: String, email: String, password: String) {
        // Client-side Validation
        val validationError = validateSignupInput(username, email, password)
        if (validationError != null) {
            _state.value = _state.value.copy(isLoading = false, error = validationError)
            return
        }

        // Normalization
        val normalizedEmail = email.trim().lowercase()
        val normalizedPassword = password.trim()
        val normalizedUsername = username.trim()

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, signupSuccess = false)
            try {
                val request = SignupRequest(
                    username = normalizedUsername,
                    email = normalizedEmail,
                    password = normalizedPassword,
                    role = "STUDENT",
                    mfaEnabled = false
                )
                val response = RetrofitClient.authService.signup(request)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, signupSuccess = true, emailForVerification = normalizedEmail)
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun verifyAccount(otp: String) {
         val email = _state.value.emailForVerification ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, verifySuccess = false)
            try {
                val response = RetrofitClient.authService.verifyAccount(VerifyAccountRequest(email, otp))
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, verifySuccess = true)
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
    
    fun requestPasswordReset(email: String) {
         viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, passwordResetRequestSuccess = false, emailForVerification = email)
            try {
                val response = RetrofitClient.authService.requestPasswordReset(email)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, passwordResetRequestSuccess = true)
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
    
    fun resetPassword(otp: String, newPassword: String) {
        val email = _state.value.emailForVerification ?: return
        
        // Validate new password
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
             _state.value = _state.value.copy(isLoading = false, error = "Password must be 8-128 chars with 1 upper, 1 lower, 1 digit, 1 special char")
             return
        }

        viewModelScope.launch {
             _state.value = _state.value.copy(isLoading = true, error = null, passwordResetSuccess = false)
            try {
                val response = RetrofitClient.authService.resetPassword(ResetPasswordRequest(email, otp, newPassword))
                 if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, passwordResetSuccess = true)
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
    
    fun fetchProfile() {
        val userId = _state.value.userId
        if (userId == null) {
            _state.value = _state.value.copy(error = "User ID is missing. Please login again.")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Pass userId to getProfile
                val response = RetrofitClient.profileApi.getProfile("Bearer ", userId) 
                if (response.isSuccessful && response.body() != null) {
                    _state.value = _state.value.copy(isLoading = false, profile = response.body())
                } else {
                     val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun updateProfile(updatedProfile: ClientProfileDto) {
        val userId = _state.value.userId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, updateSuccess = false)
            try {
                val response = RetrofitClient.profileApi.updateProfile("Bearer ", userId, updatedProfile)
                if (response.isSuccessful && response.body() != null) {
                    _state.value = _state.value.copy(isLoading = false, profile = response.body(), updateSuccess = true)
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun uploadProfilePicture(file: MultipartBody.Part) {
        val userId = _state.value.userId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, uploadSuccess = false)
            try {
                val response = RetrofitClient.profileApi.uploadProfilePicture("Bearer ", userId, file)
                if (response.isSuccessful && response.body() != null) {
                    _state.value = _state.value.copy(isLoading = false, profile = response.body(), uploadSuccess = true)
                } else {
                    val errorMsg = parseError(response)
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    fun clearStatusFlags() {
        _state.value = _state.value.copy(updateSuccess = false, uploadSuccess = false, error = null)
    }

    fun resetState() {
        _state.value = AuthState()
    }
    
    private fun validateSignupInput(username: String, email: String, password: String): String? {
        if (username.isBlank() || username.length < 3 || username.length > 50) {
            return "Username must be between 3 and 50 characters"
        }
        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches() || email.length > 255) {
            return "Invalid email address"
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "Password must be 8-128 chars with 1 upper, 1 lower, 1 digit, 1 special char"
        }
        return null
    }
    
    private fun parseError(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                errorBody
            } else {
                "Error: ${response.code()}"
            }
        } catch (e: Exception) {
            "Error: ${response.code()}"
        }
    }

    private fun handleException(e: Exception) {
        val errorMessage = when (e) {
            is IOException -> "Network error: Please check your internet connection"
            is HttpException -> "Server error: ${e.code()}"
            else -> "Unknown error: ${e.localizedMessage}"
        }
        _state.value = _state.value.copy(isLoading = false, error = errorMessage)
    }
}
