package com.whatsummary.data.repository

import com.whatsummary.data.db.dao.SummaryDao
import com.whatsummary.data.db.entity.Summary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: SummaryDao
) {

    suspend fun saveSummary(summary: Summary): Long = summaryDao.upsert(summary)

    fun getAllSummaries(): Flow<List<Summary>> = summaryDao.getAllSummaries()

    fun getSummariesByDate(date: String): Flow<List<Summary>> = summaryDao.getSummariesByDate(date)

    fun getSummaryById(id: Long): Flow<Summary?> = summaryDao.getSummaryById(id)

    fun getSummariesByGroup(groupName: String): Flow<List<Summary>> =
        summaryDao.getSummariesByGroup(groupName)

    suspend fun deleteOlderThan(cutoffDate: String) = summaryDao.deleteOlderThan(cutoffDate)

    suspend fun getAllSummariesSnapshot(): List<Summary> = summaryDao.getAllSummariesSnapshot()

    suspend fun deleteAll() = summaryDao.deleteAll()
}
