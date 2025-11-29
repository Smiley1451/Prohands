package com.anand.prohands.data

import com.google.gson.annotations.SerializedName


data class AuthResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("username") val username: String?, // Present here
    @SerializedName("role") val role: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("refreshToken") val refreshToken: String?
)


data class LoginResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("userId") val userId: String?,
    // LoginResponse in backend does NOT have 'username' field
    @SerializedName("role") val role: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("mfaRequired") val mfaRequired: Boolean = false,
    @SerializedName("message") val message: String?
)
