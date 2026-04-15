package com.whatsummary.data.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.whatsummary.util.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSummaryGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager,
    private val fileLogger: FileLogger
) : SummaryGenerator {

    private var llmInference: LlmInference? = null

    override suspend fun generateSummary(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                fileLogger.i(TAG, "Generating summary (prompt length=${prompt.length})")
                val inference = getOrCreateInference()
                    ?: return@withContext Result.failure(
                        IllegalStateException("Modelo local não disponível. Faça o download primeiro.")
                    )
                val start = System.currentTimeMillis()
                val response = inference.generateResponse(prompt)
                val elapsed = System.currentTimeMillis() - start
                fileLogger.i(
                    TAG,
                    "Summary generated in ${elapsed}ms (response length=${response.length})"
                )
                Result.success(response)
            } catch (e: Exception) {
                fileLogger.e(TAG, "Failed to generate summary", e)
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
        if (modelPath == null || !File(modelPath).exists()) {
            fileLogger.w(TAG, "Model file not found at path=$modelPath")
            return null
        }

        fileLogger.i(TAG, "Initializing LlmInference with model=$modelPath")
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(2048)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build()

        return try {
            val start = System.currentTimeMillis()
            val inference = LlmInference.createFromOptions(context, options)
            val elapsed = System.currentTimeMillis() - start
            fileLogger.i(TAG, "LlmInference initialized in ${elapsed}ms")
            llmInference = inference
            inference
        } catch (e: Throwable) {
            fileLogger.e(TAG, "Failed to initialize LlmInference", e)
            null
        }
    }

    fun release() {
        llmInference?.close()
        llmInference = null
    }

    companion object {
        private const val TAG = "LocalSummaryGen"
    }
}
