package com.anand.prohands.network

import com.anand.prohands.data.chat.ChatMessage
import com.anand.prohands.data.chat.MediaSignature
import com.anand.prohands.data.chat.UserPresence
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatService {

    @GET("/api/chat/history/{chatId}")
    suspend fun getChatHistory(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<List<ChatMessage>>

    @GET("/api/chat/presence/{userId}")
    suspend fun getUserPresence(
        @Path("userId") userId: String
    ): Response<UserPresence>

    @POST("/api/chat/media/sign")
    suspend fun getMediaSignature(
        @Header("X-User-Id") userId: String
    ): Response<MediaSignature>
}
