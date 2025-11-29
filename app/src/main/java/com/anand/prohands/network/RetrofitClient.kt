package com.anand.prohands.network

import android.content.Context
import com.anand.prohands.utils.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val AUTH_BASE_URL = "https://unsufferably-heimish-tashina.ngrok-free.dev/"
    private const val PROFILE_BASE_URL = "https://picks-situation-images-undefined.trycloudflare.com/"

    // Hold a reference to SessionManager
    private lateinit var sessionManager: SessionManager

    // Initialize this in MainActivity
    fun init(context: Context) {
        sessionManager = SessionManager(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager)) // Add the interceptor here
            .build()
    }

    private val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .client(okHttpClient) // Use the custom client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val profileRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(PROFILE_BASE_URL)
            .client(okHttpClient) // Use the custom client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthApi by lazy {
        authRetrofit.create(AuthApi::class.java)
    }

    val profileApi: ProfileApi by lazy {
        profileRetrofit.create(ProfileApi::class.java)
    }
}
