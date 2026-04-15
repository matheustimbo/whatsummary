package com.whatsummary.data.llm

interface SummaryGenerator {
    suspend fun generateSummary(prompt: String): Result<String>
    suspend fun isAvailable(): Boolean
}
