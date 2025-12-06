package com.anand.prohands.network

import android.content.Context
import com.anand.prohands.BuildConfig
import com.anand.prohands.utils.SessionManager
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://unsufferably-heimish-tashina.ngrok-free.dev/"

    // NOTE: You must replace this with your actual production host and SHA-256 pin
    private const val CERT_HOST = "unsufferably-heimish-tashina.ngrok-free.dev"
    private const val CERT_PIN_PLACEHOLDER = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    private lateinit var sessionManager: SessionManager

    fun init(context: Context) {
        sessionManager = SessionManager(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager))

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = Level.BODY
            }
            clientBuilder.addInterceptor(logging)
        } else {
            val certificatePinner = CertificatePinner.Builder()
                .add(CERT_HOST, CERT_PIN_PLACEHOLDER)
                .build()
            clientBuilder.certificatePinner(certificatePinner)
        }

        clientBuilder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val profileApi: ProfileApi by lazy {
        retrofit.create(ProfileApi::class.java)
    }

    val jobService: JobService by lazy {
        retrofit.create(JobService::class.java)
    }
}
