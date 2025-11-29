package com.anand.prohands.network

import com.anand.prohands.data.ClientProfileDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ProfileApi {
    @GET("api/clients/{userId}")
    suspend fun getProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<ClientProfileDto>

    @PUT("api/clients/{userId}")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Body profile: ClientProfileDto
    ): Response<ClientProfileDto>

    @Multipart
    @POST("api/clients/{userId}/picture")
    suspend fun uploadProfilePicture(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<ClientProfileDto>

    @GET("api/clients")
    suspend fun searchProfiles(
        @Header("Authorization") token: String,
        @Query("skill") skill: String?,
        @Query("minRating") minRating: Double?
    ): Response<List<ClientProfileDto>>
}
