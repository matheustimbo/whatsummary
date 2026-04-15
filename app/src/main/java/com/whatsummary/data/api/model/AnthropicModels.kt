package com.whatsummary.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnthropicRequest(
    val model: String,
    @Json(name = "max_tokens") val maxTokens: Int = 1024,
    val messages: List<Message>
) {
    @JsonClass(generateAdapter = true)
    data class Message(
        val role: String = "user",
        val content: String
    )
}

@JsonClass(generateAdapter = true)
data class AnthropicResponse(
    val id: String,
    val content: List<ContentBlock>,
    val model: String,
    val usage: Usage
) {
    @JsonClass(generateAdapter = true)
    data class ContentBlock(
        val type: String,
        val text: String
    )

    @JsonClass(generateAdapter = true)
    data class Usage(
        @Json(name = "input_tokens") val inputTokens: Int,
        @Json(name = "output_tokens") val outputTokens: Int
    )
}

@JsonClass(generateAdapter = true)
data class AnthropicError(
    val type: String,
    val error: ErrorDetail
) {
    @JsonClass(generateAdapter = true)
    data class ErrorDetail(
        val type: String,
        val message: String
    )
}
