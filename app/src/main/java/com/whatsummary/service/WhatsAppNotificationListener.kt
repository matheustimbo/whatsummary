package com.whatsummary.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.whatsummary.data.db.entity.CapturedMessage
import com.whatsummary.data.repository.MessageRepository
import com.whatsummary.util.FileLogger
import com.whatsummary.util.NotificationParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WhatsAppNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var fileLogger: FileLogger

    private val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            fileLogger.e(TAG, "Coroutine error in listener scope", throwable)
        }
    }

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    }

    override fun onCreate() {
        super.onCreate()
        fileLogger.i(TAG, "Service created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        fileLogger.i(TAG, "Listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val parsed = try {
                NotificationParser.parse(sbn)
            } catch (e: Exception) {
                fileLogger.e(TAG, "Error parsing notification from ${sbn.packageName}", e)
                return
            }
            if (parsed.isEmpty()) return

            fileLogger.d(TAG, "Parsed ${parsed.size} message(s) from ${sbn.packageName}")

            scope.launch {
                for (msg in parsed) {
                    try {
                        val hash = NotificationParser.contentHash(
                            msg.groupName, msg.author, msg.text, msg.timestamp
                        )
                        messageRepository.insertMessage(
                            CapturedMessage(
                                groupName = msg.groupName,
                                author = msg.author,
                                text = msg.text,
                                timestamp = msg.timestamp,
                                messageType = msg.messageType,
                                contentHash = hash
                            )
                        )
                    } catch (e: Exception) {
                        fileLogger.e(TAG, "Error inserting message for group=${msg.groupName}", e)
                    }
                }
            }
        } catch (e: Throwable) {
            fileLogger.e(TAG, "Unexpected error in onNotificationPosted", e)
        }
    }

    override fun onListenerDisconnected() {
        fileLogger.w(TAG, "Listener disconnected, requesting rebind")
        try {
            requestRebind(ComponentName(this, WhatsAppNotificationListener::class.java))
        } catch (e: Exception) {
            fileLogger.e(TAG, "Failed to rebind listener", e)
        }
    }

    override fun onDestroy() {
        fileLogger.i(TAG, "Service destroyed")
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WhatsAppListener"
    }
}
