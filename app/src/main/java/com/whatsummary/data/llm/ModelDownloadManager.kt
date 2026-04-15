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

    fun isModelBundled(): Boolean {
        return try {
            context.assets.open("models/$MODEL_FILENAME").close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getModelPath(): String? = if (isModelDownloaded()) modelFile.absolutePath else null

    fun getModelSizeMb(): Long {
        return if (modelFile.exists()) modelFile.length() / (1024 * 1024) else 0
    }

    /**
     * Ensures the model is ready in internal storage.
     * Priority: already extracted > bundled in assets > download from HuggingFace.
     */
    suspend fun ensureModel(): Result<String> = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            _downloadState.value = DownloadState.Completed
            return@withContext Result.success(modelFile.absolutePath)
        }

        modelDir.mkdirs()

        if (isModelBundled()) {
            return@withContext extractFromAssets()
        }

        _downloadState.value = DownloadState.Error("Modelo não encontrado. Reinstale o app.")
        Result.failure(Exception("Model not bundled and no download fallback"))
    }

    private fun extractFromAssets(): Result<String> {
        _downloadState.value = DownloadState.Downloading(0f)

        return try {
            context.assets.open("models/$MODEL_FILENAME").use { input ->
                val totalSize = input.available().toLong()
                val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")

                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(65536)
                    var totalBytesRead = 0L

                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (totalSize > 0) {
                            val progress = totalBytesRead.toFloat() / totalSize
                            _downloadState.value = DownloadState.Downloading(progress)
                        }
                    }
                }

                tempFile.renameTo(modelFile)
            }

            _downloadState.value = DownloadState.Completed
            Result.success(modelFile.absolutePath)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "Erro ao extrair modelo")
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
        const val MODEL_SIZE_MB = 657
    }
}
