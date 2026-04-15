package com.whatsummary.data.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSummaryGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager
) : SummaryGenerator {

    private var llmInference: LlmInference? = null

    override suspend fun generateSummary(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inference = getOrCreateInference()
                    ?: return@withContext Result.failure(
                        IllegalStateException("Modelo local não disponível. Faça o download primeiro.")
                    )
                val response = inference.generateResponse(prompt)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun isAvailable(): Boolean {
        return modelDownloadManager.isModelDownloaded()
    }

    private fun getOrCreateInference(): LlmInference? {
        llmInference?.let { return it }

        val modelPath = modelDownloadManager.getModelPath()
        if (modelPath == null || !File(modelPath).exists()) return null

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(2048)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build()

        return LlmInference.createFromOptions(context, options).also {
            llmInference = it
        }
    }

    fun release() {
        llmInference?.close()
        llmInference = null
    }
}
