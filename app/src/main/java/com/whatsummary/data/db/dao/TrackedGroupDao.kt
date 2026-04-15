package com.whatsummary.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.whatsummary.data.db.entity.TrackedGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedGroupDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(group: TrackedGroup): Long

    @Query("SELECT * FROM tracked_groups ORDER BY group_name ASC")
    fun getAllGroups(): Flow<List<TrackedGroup>>

    @Query("SELECT * FROM tracked_groups WHERE is_enabled = 1 ORDER BY group_name ASC")
    suspend fun getEnabledGroups(): List<TrackedGroup>

    @Query("UPDATE tracked_groups SET is_enabled = :enabled WHERE group_name = :groupName")
    suspend fun setEnabled(groupName: String, enabled: Boolean)

    @Query("DELETE FROM tracked_groups")
    suspend fun deleteAll()
}
