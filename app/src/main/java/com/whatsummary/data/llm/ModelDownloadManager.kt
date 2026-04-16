package com.whatsummary.data.llm

import android.content.Context
import com.whatsummary.util.FileLogger
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
    @ApplicationContext private val context: Context,
    private val fileLogger: FileLogger
) {
    private val modelDir = File(context.filesDir, "models")
    private val modelFile = File(modelDir, MODEL_FILENAME)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: Flow<DownloadState> = _downloadState.asStateFlow()

    fun isModelDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 0

    /**
     * The APK used to ship the model in assets (which made the APK 600+ MB
     * and frequently stalled download on mobile). Now the model is uploaded
     * as a separate public GitHub Release asset and pulled on first use.
     */
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
     * Priority: already downloaded > bundled in assets (legacy) > download from public URL.
     */
    suspend fun ensureModel(): Result<String> = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            fileLogger.i(TAG, "Model already present at ${modelFile.absolutePath} (${getModelSizeMb()} MB)")
            _downloadState.value = DownloadState.Completed
            return@withContext Result.success(modelFile.absolutePath)
        }

        modelDir.mkdirs()

        if (isModelBundled()) {
            fileLogger.i(TAG, "Extracting bundled model from assets (legacy APK)")
            return@withContext extractFromAssets()
        }

        fileLogger.i(TAG, "Downloading model from public URL: $MODEL_URL")
        return@withContext downloadFromUrl()
    }

    private fun extractFromAssets(): Result<String> {
        _downloadState.value = DownloadState.Downloading(0f)
        val start = System.currentTimeMillis()

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

            val elapsed = System.currentTimeMillis() - start
            fileLogger.i(TAG, "Model extracted in ${elapsed}ms (size=${getModelSizeMb()} MB)")
            _downloadState.value = DownloadState.Completed
            Result.success(modelFile.absolutePath)
        } catch (e: Exception) {
            fileLogger.e(TAG, "Failed to extract bundled model", e)
            _downloadState.value = DownloadState.Error(e.message ?: "Erro ao extrair modelo")
            File(modelDir, "$MODEL_FILENAME.tmp").delete()
            Result.failure(e)
        }
    }

    private fun downloadFromUrl(): Result<String> {
        _downloadState.value = DownloadState.Downloading(0f)
        val start = System.currentTimeMillis()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")

        return try {
            val request = Request.Builder().url(MODEL_URL).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val msg = "Download falhou: HTTP ${response.code}"
                fileLogger.e(TAG, msg)
                _downloadState.value = DownloadState.Error(msg)
                return Result.failure(Exception(msg))
            }

            val body = response.body ?: run {
                fileLogger.e(TAG, "Empty body")
                _downloadState.value = DownloadState.Error("Resposta vazia do servidor")
                return Result.failure(Exception("Empty body"))
            }

            val contentLength = body.contentLength()

            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(65536)
                    var totalBytesRead = 0L
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength
                            _downloadState.value = DownloadState.Downloading(progress)
                        }
                    }
                }
            }

            if (!tempFile.renameTo(modelFile)) {
                throw java.io.IOException("Falha ao finalizar arquivo do modelo")
            }

            val elapsed = System.currentTimeMillis() - start
            fileLogger.i(
                TAG,
                "Model downloaded in ${elapsed}ms (size=${getModelSizeMb()} MB)"
            )
            _downloadState.value = DownloadState.Completed
            Result.success(modelFile.absolutePath)
        } catch (e: Exception) {
            fileLogger.e(TAG, "Failed to download model", e)
            tempFile.delete()
            _downloadState.value = DownloadState.Error(e.message ?: "Erro ao baixar modelo")
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
        private const val TAG = "ModelDownloadMgr"
        const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
        const val MODEL_SIZE_MB = 529

        /** Public GitHub Release asset (no auth, no gating). */
        const val MODEL_URL =
            "https://github.com/matheustimbo/whatsummary/releases/download/latest/gemma3-1b-it-int4.task"
    }
}
