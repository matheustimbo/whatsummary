package com.whatsummary.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    indices = [Index(value = ["group_name", "date"])]
)
data class Summary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "group_name") val groupName: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "message_count") val messageCount: Int,
    @ColumnInfo(name = "model_used") val modelUsed: String = "claude-haiku-4-5-20251001",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
