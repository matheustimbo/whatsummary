package com.whatsummary.data.api

import com.whatsummary.data.api.model.AnthropicRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnthropicClient @Inject constructor(
    private val api: AnthropicApi
) {

    suspend fun generateSummary(prompt: String, model: String): Result<String> {
        return try {
            val request = AnthropicRequest(
                model = model,
                maxTokens = 1024,
                messages = listOf(AnthropicRequest.Message(content = prompt))
            )
            val response = api.createMessage(request)
            val text = response.content
                .filter { it.type == "text" }
                .joinToString("\n") { it.text }
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testApiKey(): Boolean {
        return try {
            val request = AnthropicRequest(
                model = "claude-haiku-4-5-20251001",
                maxTokens = 10,
                messages = listOf(AnthropicRequest.Message(content = "Say OK"))
            )
            api.createMessage(request)
            true
        } catch (e: Exception) {
            false
        }
    }
}
