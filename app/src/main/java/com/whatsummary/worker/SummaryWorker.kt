package com.whatsummary.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whatsummary.R
import com.whatsummary.WhatsummaryApp
import com.whatsummary.data.db.entity.Summary
import com.whatsummary.data.llm.ApiSummaryGenerator
import com.whatsummary.data.llm.LocalSummaryGenerator
import com.whatsummary.data.llm.SummaryGenerator
import com.whatsummary.data.preferences.UserPreferences
import com.whatsummary.data.repository.GroupRepository
import com.whatsummary.data.repository.MessageRepository
import com.whatsummary.data.repository.SummaryRepository
import com.whatsummary.util.PromptBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val summaryRepository: SummaryRepository,
    private val groupRepository: GroupRepository,
    private val apiGenerator: ApiSummaryGenerator,
    private val localGenerator: LocalSummaryGenerator,
    private val preferences: UserPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val generator: SummaryGenerator = when (preferences.inferenceMode) {
            UserPreferences.MODE_API -> {
                if (!apiGenerator.isAvailable()) return Result.failure()
                apiGenerator
            }
            else -> {
                if (!localGenerator.isAvailable()) {
                    // Fallback to API if local model not downloaded
                    if (apiGenerator.isAvailable()) apiGenerator
                    else return Result.failure()
                } else {
                    localGenerator
                }
            }
        }

        val today = LocalDate.now()
        val dateStr = today.toString()
        val modelLabel = if (generator is ApiSummaryGenerator) preferences.llmModel else "gemma-3-1b-local"

        return try {
            val enabledGroups = groupRepository.getEnabledGroups().take(MAX_GROUPS_PER_RUN)
            var summarizedCount = 0

            for (group in enabledGroups) {
                val messages = messageRepository.getUnsummarizedMessages(group.groupName, today)
                if (messages.isEmpty()) continue

                val prompt = PromptBuilder.buildSummaryPrompt(
                    groupName = group.groupName,
                    date = dateStr,
                    messages = messages
                )

                val result = generator.generateSummary(prompt)
                if (result.isFailure) continue
                val summaryText = result.getOrThrow()

                summaryRepository.saveSummary(
                    Summary(
                        groupName = group.groupName,
                        date = dateStr,
                        content = summaryText,
                        messageCount = messages.size,
                        modelUsed = modelLabel
                    )
                )

                messageRepository.markAsSummarized(group.groupName, today)
                summarizedCount++
            }

            // Cleanup old data
            messageRepository.deleteOlderThan(preferences.retentionDays)
            val cutoffDate = today.minusDays(preferences.retentionDays.toLong()).toString()
            summaryRepository.deleteOlderThan(cutoffDate)

            if (summarizedCount > 0) {
                sendNotification(summarizedCount)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun sendNotification(groupCount: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, WhatsummaryApp.CHANNEL_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(
                applicationContext.getString(R.string.notification_summary_ready, groupCount)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val MAX_GROUPS_PER_RUN = 20
        private const val NOTIFICATION_ID = 1001
    }
}
