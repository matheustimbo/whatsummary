package com.whatsummary.di

import android.content.Context
import com.whatsummary.data.db.WhatsummaryDatabase
import com.whatsummary.data.db.dao.MessageDao
import com.whatsummary.data.db.dao.SummaryDao
import com.whatsummary.data.db.dao.TrackedGroupDao
import com.whatsummary.data.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        preferences: UserPreferences
    ): WhatsummaryDatabase {
        val passphrase = preferences.dbPassphrase.toByteArray(Charsets.UTF_8)
        return WhatsummaryDatabase.create(context, passphrase)
    }

    @Provides
    fun provideMessageDao(database: WhatsummaryDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideSummaryDao(database: WhatsummaryDatabase): SummaryDao = database.summaryDao()

    @Provides
    fun provideTrackedGroupDao(database: WhatsummaryDatabase): TrackedGroupDao =
        database.trackedGroupDao()
}
