package com.anand.prohands.network

import com.anand.prohands.data.chat.MediaSignRequest
import com.anand.prohands.data.chat.MediaSignResponse
import com.anand.prohands.data.chat.MessageDto
import com.anand.prohands.data.chat.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatService {

    @GET("api/chat/sync")
    suspend fun sync(
        @Query("since") since: String
    ): Response<SyncResponse>

    @GET("api/chat/history/{chatId}")
    suspend fun getHistory(
        @Path("chatId") chatId: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<List<MessageDto>>

    @POST("api/chat/media/sign")
    suspend fun signMedia(
        @Body request: MediaSignRequest
    ): Response<MediaSignResponse>
}
