package com.whatsummary.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.whatsummary.data.preferences.UserPreferences
import com.whatsummary.worker.SummaryScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var summaryScheduler: SummaryScheduler

    @Inject
    lateinit var preferences: UserPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && preferences.onboardingCompleted) {
            summaryScheduler.schedule()
        }
    }
}
