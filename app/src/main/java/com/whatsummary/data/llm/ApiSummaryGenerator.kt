package com.whatsummary.data.llm

import com.whatsummary.data.api.AnthropicClient
import com.whatsummary.data.preferences.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSummaryGenerator @Inject constructor(
    private val anthropicClient: AnthropicClient,
    private val preferences: UserPreferences
) : SummaryGenerator {

    override suspend fun generateSummary(prompt: String): Result<String> {
        return anthropicClient.generateSummary(prompt, preferences.llmModel)
    }

    override suspend fun isAvailable(): Boolean {
        return !preferences.apiKey.isNullOrBlank()
    }
}
