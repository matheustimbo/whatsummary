package com.whatsummary.util

/**
 * Global uncaught-exception handler that writes crash stack traces
 * to the file log before delegating to the previous handler
 * (so Android still shows "app stopped" and restarts the process).
 */
class CrashHandler(
    private val fileLogger: FileLogger,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            fileLogger.e(
                TAG,
                "UNCAUGHT EXCEPTION on thread '${thread.name}' (id=${thread.id})",
                throwable
            )
            // Give the logger a moment to flush
            Thread.sleep(150)
        } catch (_: Throwable) {
            // Never block the crash path
        }
        previousHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        private const val TAG = "CrashHandler"

        fun install(fileLogger: FileLogger) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(fileLogger, previous))
        }
    }
}
