package com.whatsummary.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.whatsummary.data.db.dao.MessageDao
import com.whatsummary.data.db.dao.SummaryDao
import com.whatsummary.data.db.dao.TrackedGroupDao
import com.whatsummary.data.db.entity.CapturedMessage
import com.whatsummary.data.db.entity.Summary
import com.whatsummary.data.db.entity.TrackedGroup
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [CapturedMessage::class, Summary::class, TrackedGroup::class],
    version = 1,
    exportSchema = true
)
abstract class WhatsummaryDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun summaryDao(): SummaryDao
    abstract fun trackedGroupDao(): TrackedGroupDao

    companion object {
        fun create(context: Context, passphrase: ByteArray): WhatsummaryDatabase {
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                WhatsummaryDatabase::class.java,
                "whatsummary.db"
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
