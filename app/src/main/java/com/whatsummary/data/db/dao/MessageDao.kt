package com.whatsummary.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.whatsummary.data.db.entity.CapturedMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: CapturedMessage): Long

    @Query("""
        SELECT * FROM messages
        WHERE group_name = :groupName
        AND summarized = 0
        AND timestamp >= :startOfDay
        AND timestamp < :endOfDay
        ORDER BY timestamp ASC
    """)
    suspend fun getUnsummarizedMessages(
        groupName: String,
        startOfDay: Long,
        endOfDay: Long
    ): List<CapturedMessage>

    @Query("""
        SELECT * FROM messages
        WHERE group_name = :groupName
        AND timestamp >= :startOfDay
        AND timestamp < :endOfDay
        ORDER BY timestamp ASC
    """)
    suspend fun getMessagesForGroupOnDate(
        groupName: String,
        startOfDay: Long,
        endOfDay: Long
    ): List<CapturedMessage>

    @Query("UPDATE messages SET summarized = 1 WHERE group_name = :groupName AND timestamp >= :startOfDay AND timestamp < :endOfDay")
    suspend fun markAsSummarized(groupName: String, startOfDay: Long, endOfDay: Long)

    @Query("DELETE FROM messages WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE group_name = :groupName AND timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getMessageCountToday(groupName: String, startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT DISTINCT group_name FROM messages ORDER BY group_name")
    fun getAllGroupNames(): Flow<List<String>>

    @Query("""
        SELECT * FROM messages
        WHERE group_name = :groupName
        AND timestamp >= :since
        ORDER BY timestamp DESC
    """)
    fun observeMessagesForGroup(groupName: String, since: Long): Flow<List<CapturedMessage>>
}
