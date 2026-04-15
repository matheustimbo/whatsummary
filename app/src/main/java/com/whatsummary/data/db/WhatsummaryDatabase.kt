package com.whatsummary.data.db

import android.content.Context
import android.util.Log
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
        private const val TAG = "WhatsummaryDatabase"
        private const val DB_NAME = "whatsummary.db"

        fun create(context: Context, passphrase: ByteArray): WhatsummaryDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                WhatsummaryDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration()

            // Try SQLCipher encryption; if the native lib failed to load or
            // the library isn't available, fall back to a plain (unencrypted)
            // database so the app can still function.
            return try {
                val factory = SupportOpenHelperFactory(passphrase)
                builder.openHelperFactory(factory).build()
            } catch (e: Throwable) {
                Log.e(TAG, "SQLCipher unavailable, using unencrypted database", e)
                builder.build()
            }
        }
    }
}
