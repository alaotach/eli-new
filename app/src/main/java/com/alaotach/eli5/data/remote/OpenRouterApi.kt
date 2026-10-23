package com.alaotach.eli5.data.remote

import com.alaotach.eli5.data.model.ChatRequest
import com.alaotach.eli5.data.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun getCompletion(
        @Body request: ChatRequest
    ): ChatResponse

    @retrofit2.http.Streaming
    @POST("chat/completions")
    suspend fun getCompletionStream(
        @Body request: ChatRequest
    ): okhttp3.ResponseBody
}
