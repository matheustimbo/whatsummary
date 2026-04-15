package com.whatsummary.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent file-based logger so crashes and errors can be reviewed
 * later (shared via "Export logs" in Settings).
 *
 * Log format: `YYYY-MM-DD HH:mm:ss.SSS LEVEL TAG: message`
 *
 * Writes are batched and run on a dedicated single-threaded executor
 * so logging never blocks the UI or callers.
 */
@Singleton
class FileLogger @Inject constructor(
    @ApplicationContext context: Context
) {
    private val logFile = File(context.filesDir, LOG_FILE)
    private val rotatedFile = File(context.filesDir, LOG_FILE_ROTATED)
    private val queue = ConcurrentLinkedQueue<String>()
    private val writer = Executors.newSingleThreadExecutor { r ->
        Thread(r, "FileLogger").apply { isDaemon = true }
    }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        // Log app start
        i(TAG, "=== App started ===")
    }

    fun d(tag: String, message: String) = write("D", tag, message, null)
    fun i(tag: String, message: String) = write("I", tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) =
        write("W", tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        write("E", tag, message, throwable)

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        val timestamp = dateFormat.format(Date())
        val logLine = buildString {
            append(timestamp).append(' ').append(level).append(' ')
            append(tag).append(": ").append(message)
            if (throwable != null) {
                append('\n')
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                append(sw.toString().trimEnd())
            }
        }

        // Also emit to logcat so it's visible when debugging
        when (level) {
            "D" -> Log.d(tag, message, throwable)
            "I" -> Log.i(tag, message, throwable)
            "W" -> Log.w(tag, message, throwable)
            "E" -> Log.e(tag, message, throwable)
        }

        queue.offer(logLine)
        writer.execute { flush() }
    }

    @Synchronized
    private fun flush() {
        try {
            // Rotate if too big
            if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                if (rotatedFile.exists()) rotatedFile.delete()
                logFile.renameTo(rotatedFile)
            }

            FileWriter(logFile, true).use { fw ->
                PrintWriter(fw).use { pw ->
                    var line: String? = queue.poll()
                    while (line != null) {
                        pw.println(line)
                        line = queue.poll()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush logs to file", e)
        }
    }

    /**
     * Returns the combined log file (current + rotated) for sharing.
     * Called from Settings "Export logs".
     */
    @Synchronized
    fun getCombinedLogs(): String {
        val sb = StringBuilder()
        if (rotatedFile.exists()) sb.append(rotatedFile.readText())
        if (logFile.exists()) sb.append(logFile.readText())
        return sb.toString()
    }

    fun clear() {
        writer.execute {
            try {
                logFile.delete()
                rotatedFile.delete()
                queue.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs", e)
            }
        }
    }

    companion object {
        private const val TAG = "FileLogger"
        private const val LOG_FILE = "app.log"
        private const val LOG_FILE_ROTATED = "app.log.1"
        private const val MAX_FILE_SIZE = 512 * 1024L // 512 KB
    }
}
