package com.whatsummary.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_groups")
data class TrackedGroup(
    @PrimaryKey
    @ColumnInfo(name = "group_name") val groupName: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)
