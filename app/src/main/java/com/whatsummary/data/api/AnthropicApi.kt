package com.whatsummary.data.api

import com.whatsummary.data.api.model.AnthropicRequest
import com.whatsummary.data.api.model.AnthropicResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AnthropicApi {

    @POST("v1/messages")
    suspend fun createMessage(@Body request: AnthropicRequest): AnthropicResponse
}
