package com.whatsummary.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.whatsummary.data.db.entity.CapturedMessage
import com.whatsummary.data.repository.MessageRepository
import com.whatsummary.util.NotificationParser
import dagger.hilt.android.AndroidEntryPoint
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val parsed = NotificationParser.parse(sbn)
        if (parsed.isEmpty()) return

        scope.launch {
            for (msg in parsed) {
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
            }
        }
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, WhatsAppNotificationListener::class.java))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
