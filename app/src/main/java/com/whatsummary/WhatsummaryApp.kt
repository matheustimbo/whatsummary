package com.whatsummary

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.whatsummary.util.CrashHandler
import com.whatsummary.util.FileLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WhatsummaryApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var fileLogger: FileLogger

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Install crash handler as early as possible (Hilt has already injected
        // by the time super.onCreate() returns).
        CrashHandler.install(fileLogger)
        fileLogger.i(TAG, "Application.onCreate — version=${packageInfoVersion()}")

        loadNativeLibraries()
        createNotificationChannels()
    }

    private fun loadNativeLibraries() {
        try {
            System.loadLibrary("sqlcipher")
            fileLogger.i(TAG, "SQLCipher native library loaded")
        } catch (e: UnsatisfiedLinkError) {
            fileLogger.e(TAG, "Failed to load SQLCipher native library", e)
        }
    }

    private fun createNotificationChannels() {
        try {
            val channel = NotificationChannel(
                CHANNEL_SUMMARY,
                getString(R.string.notification_channel_summary),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_summary)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        } catch (e: Exception) {
            fileLogger.e(TAG, "Failed to create notification channel", e)
        }
    }

    private fun packageInfoVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    companion object {
        private const val TAG = "WhatsummaryApp"
        const val CHANNEL_SUMMARY = "summary_channel"
    }
}
