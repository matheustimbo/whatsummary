package com.whatsummary.data.repository

import com.whatsummary.data.db.dao.MessageDao
import com.whatsummary.data.db.dao.TrackedGroupDao
import com.whatsummary.data.db.entity.CapturedMessage
import com.whatsummary.data.db.entity.TrackedGroup
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val trackedGroupDao: TrackedGroupDao
) {

    suspend fun insertMessage(message: CapturedMessage) {
        messageDao.insert(message)
        trackedGroupDao.insertIfNotExists(
            TrackedGroup(groupName = message.groupName)
        )
    }

    suspend fun getUnsummarizedMessages(groupName: String, date: LocalDate): List<CapturedMessage> {
        val (start, end) = dayBounds(date)
        return messageDao.getUnsummarizedMessages(groupName, start, end)
    }

    suspend fun getMessagesForGroupOnDate(groupName: String, date: LocalDate): List<CapturedMessage> {
        val (start, end) = dayBounds(date)
        return messageDao.getMessagesForGroupOnDate(groupName, start, end)
    }

    suspend fun markAsSummarized(groupName: String, date: LocalDate) {
        val (start, end) = dayBounds(date)
        messageDao.markAsSummarized(groupName, start, end)
    }

    suspend fun deleteOlderThan(days: Int) {
        val cutoff = LocalDate.now().minusDays(days.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        messageDao.deleteOlderThan(cutoff)
    }

    fun getMessageCountToday(groupName: String): Flow<Int> {
        val (start, end) = dayBounds(LocalDate.now())
        return messageDao.getMessageCountToday(groupName, start, end)
    }

    fun getAllGroupNames(): Flow<List<String>> = messageDao.getAllGroupNames()

    fun observeRecentMessages(groupName: String, days: Int = 7): Flow<List<CapturedMessage>> {
        val since = LocalDate.now().minusDays(days.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return messageDao.observeMessagesForGroup(groupName, since)
    }

    private fun dayBounds(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}
