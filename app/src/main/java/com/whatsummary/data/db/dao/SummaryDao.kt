package com.whatsummary.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.whatsummary.data.db.entity.Summary
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: Summary): Long

    @Query("SELECT * FROM summaries ORDER BY date DESC, created_at DESC")
    fun getAllSummaries(): Flow<List<Summary>>

    @Query("SELECT * FROM summaries WHERE date = :date ORDER BY group_name ASC")
    fun getSummariesByDate(date: String): Flow<List<Summary>>

    @Query("SELECT * FROM summaries WHERE id = :id")
    fun getSummaryById(id: Long): Flow<Summary?>

    @Query("SELECT * FROM summaries WHERE group_name = :groupName ORDER BY date DESC")
    fun getSummariesByGroup(groupName: String): Flow<List<Summary>>

    @Query("DELETE FROM summaries WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("SELECT * FROM summaries ORDER BY created_at DESC")
    suspend fun getAllSummariesSnapshot(): List<Summary>

    @Query("DELETE FROM summaries")
    suspend fun deleteAll()
}
