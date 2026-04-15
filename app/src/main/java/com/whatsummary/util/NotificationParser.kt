package com.whatsummary.util

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import java.security.MessageDigest

data class ParsedMessage(
    val groupName: String,
    val author: String,
    val text: String,
    val timestamp: Long,
    val messageType: String = "text"
)

object NotificationParser {

    private val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

    fun parse(sbn: StatusBarNotification): List<ParsedMessage> {
        if (sbn.packageName !in WHATSAPP_PACKAGES) return emptyList()

        val extras = sbn.notification.extras ?: return emptyList()
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        // If no conversationTitle, it's likely a DM — skip
        if (conversationTitle == null) return emptyList()

        val groupName = conversationTitle

        // Try stacked messages first (EXTRA_TEXT_LINES)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null && textLines.isNotEmpty()) {
            return textLines.mapNotNull { line ->
                parseMessageLine(groupName, line.toString(), sbn.postTime)
            }
        }

        // Single message
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return emptyList()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return emptyList()

        // In group notifications, the title is the author name
        val author = title
        val messageType = detectMediaType(text)

        return listOf(
            ParsedMessage(
                groupName = groupName,
                author = author,
                text = text,
                timestamp = sbn.postTime,
                messageType = messageType
            )
        )
    }

    private fun parseMessageLine(groupName: String, line: String, timestamp: Long): ParsedMessage? {
        // Stacked lines have format "Author: message text"
        val colonIndex = line.indexOf(": ")
        if (colonIndex == -1) return null

        val author = line.substring(0, colonIndex)
        val text = line.substring(colonIndex + 2)
        val messageType = detectMediaType(text)

        return ParsedMessage(
            groupName = groupName,
            author = author,
            text = text,
            timestamp = timestamp,
            messageType = messageType
        )
    }

    fun detectMediaType(text: String): String {
        val lower = text.lowercase()
        return when {
            text.contains("\uD83D\uDCF7") || lower.startsWith("foto") || lower.startsWith("photo") || lower == "image" -> "image"
            text.contains("\uD83C\uDFA5") || text.contains("\uD83D\uDCF9") || lower.startsWith("v\u00eddeo") || lower.startsWith("video") -> "video"
            text.contains("\uD83C\uDFA4") || text.contains("\uD83D\uDD0A") || lower.startsWith("\u00e1udio") || lower.startsWith("audio") -> "audio"
            text.contains("\uD83D\uDCC4") || lower.startsWith("documento") || lower.startsWith("document") -> "document"
            lower.contains("sticker") || lower.contains("figurinha") -> "sticker"
            lower == "gif" -> "gif"
            text.contains("\uD83D\uDCCD") || lower.startsWith("localiza") || lower.startsWith("location") -> "location"
            lower.startsWith("contact card") || lower.startsWith("cart\u00e3o de contato") -> "contact"
            else -> "text"
        }
    }

    fun contentHash(groupName: String, author: String, text: String, timestamp: Long): String {
        // Window deduplication: round timestamp to nearest 2-second window
        val windowedTimestamp = (timestamp / 2000) * 2000
        val input = "$groupName|$author|$text|$windowedTimestamp"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
