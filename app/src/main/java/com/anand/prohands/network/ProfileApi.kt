package com.anand.prohands.network

import com.anand.prohands.data.ClientProfileDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ProfileApi {
    // Removed @Header("Authorization") because AuthInterceptor handles it now
    @GET("api/clients/{userId}")
    suspend fun getProfile(
        @Path("userId") userId: String
    ): Response<ClientProfileDto>

    @PUT("api/clients/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body profile: ClientProfileDto
    ): Response<ClientProfileDto>

    @Multipart
    @POST("api/clients/{userId}/picture")
    suspend fun uploadProfilePicture(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<ClientProfileDto>
    
    @GET("api/clients")
    suspend fun searchProfiles(
        @Query("skill") skill: String?,
        @Query("minRating") minRating: Double?
    ): Response<List<ClientProfileDto>>
}
