package com.whatsummary.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Triggers a one-off run of the SummaryWorker immediately.
     * Used by the "Resumir agora" buttons in the UI.
     */
    fun runOnce() {
        val request = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_OFF_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val ONE_OFF_WORK_NAME = "one_off_summary"
    }
}
