package com.whatsummary.data.repository

import com.whatsummary.data.db.dao.TrackedGroupDao
import com.whatsummary.data.db.entity.TrackedGroup
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val trackedGroupDao: TrackedGroupDao
) {

    fun getAllGroups(): Flow<List<TrackedGroup>> = trackedGroupDao.getAllGroups()

    suspend fun getEnabledGroups(): List<TrackedGroup> = trackedGroupDao.getEnabledGroups()

    suspend fun setEnabled(groupName: String, enabled: Boolean) {
        trackedGroupDao.setEnabled(groupName, enabled)
    }

    suspend fun ensureGroupExists(groupName: String) {
        trackedGroupDao.insertIfNotExists(TrackedGroup(groupName = groupName))
    }

    suspend fun deleteAll() = trackedGroupDao.deleteAll()
}
