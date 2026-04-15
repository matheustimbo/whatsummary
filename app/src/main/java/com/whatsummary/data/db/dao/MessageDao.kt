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

    /**
     * Returns the most recent message (by timestamp) captured for each group
     * that appears in the messages table. Uses a correlated subquery per row.
     */
    @Query("""
        SELECT m.* FROM messages m
        WHERE m.timestamp = (
            SELECT MAX(timestamp) FROM messages WHERE group_name = m.group_name
        )
        ORDER BY m.timestamp DESC
    """)
    fun observeLatestPerGroup(): Flow<List<CapturedMessage>>

    /**
     * Returns [group_name, count] pairs for all messages captured today,
     * so the Home screen can show how many new messages arrived per group.
     */
    @Query("""
        SELECT group_name AS groupName, COUNT(*) AS count FROM messages
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
        GROUP BY group_name
    """)
    fun observeTodayCountPerGroup(startOfDay: Long, endOfDay: Long): Flow<List<GroupCount>>
}

data class GroupCount(
    val groupName: String,
    val count: Int
)
