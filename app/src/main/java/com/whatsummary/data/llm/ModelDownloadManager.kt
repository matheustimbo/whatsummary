package com.whatsummary.data.llm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(modelDir, MODEL_FILENAME)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: Flow<DownloadState> = _downloadState.asStateFlow()

    fun isModelDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 0

    fun getModelPath(): String? = if (isModelDownloaded()) modelFile.absolutePath else null

    fun getModelSizeMb(): Long {
        return if (modelFile.exists()) modelFile.length() / (1024 * 1024) else 0
    }

    suspend fun downloadModel(
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            _downloadState.value = DownloadState.Completed
            return@withContext Result.success(modelFile.absolutePath)
        }

        _downloadState.value = DownloadState.Downloading(0f)
        modelDir.mkdirs()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .build()

        try {
            val request = Request.Builder()
                .url(MODEL_URL)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _downloadState.value = DownloadState.Error("Download falhou: HTTP ${response.code}")
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }

            val body = response.body ?: run {
                _downloadState.value = DownloadState.Error("Resposta vazia do servidor")
                return@withContext Result.failure(Exception("Empty response body"))
            }

            val contentLength = body.contentLength()
            val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")

            FileOutputStream(tempFile).use { output ->
                body.source().use { source ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L

                    while (true) {
                        val bytesRead = source.inputStream().read(buffer)
                        if (bytesRead == -1) break

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength
                            _downloadState.value = DownloadState.Downloading(progress)
                            onProgress?.invoke(progress)
                        }
                    }
                }
            }

            tempFile.renameTo(modelFile)
            _downloadState.value = DownloadState.Completed
            Result.success(modelFile.absolutePath)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "Erro desconhecido")
            // Clean up partial download
            File(modelDir, "$MODEL_FILENAME.tmp").delete()
            Result.failure(e)
        }
    }

    fun deleteModel() {
        modelFile.delete()
        File(modelDir, "$MODEL_FILENAME.tmp").delete()
        _downloadState.value = DownloadState.Idle
    }

    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(val progress: Float) : DownloadState()
        data object Completed : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    companion object {
        const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
        const val MODEL_URL = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task"
        const val MODEL_SIZE_MB = 657
    }
}
