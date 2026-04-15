package com.whatsummary

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WhatsummaryApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_SUMMARY,
            getString(R.string.notification_channel_summary),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_summary)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_SUMMARY = "summary_channel"
    }
}
