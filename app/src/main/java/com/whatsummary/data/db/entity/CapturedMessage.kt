package com.whatsummary.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["group_name", "timestamp"]),
        Index(value = ["content_hash"], unique = true)
    ]
)
data class CapturedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "group_name") val groupName: String,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "message_type") val messageType: String = "text",
    @ColumnInfo(name = "summarized") val summarized: Boolean = false,
    @ColumnInfo(name = "content_hash") val contentHash: String
)
