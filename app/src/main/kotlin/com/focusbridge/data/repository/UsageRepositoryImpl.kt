package com.focusbridge.data.repository

import com.focusbridge.data.db.dao.UsageRecordDao
import com.focusbridge.domain.model.UsageRecord
import com.focusbridge.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageRecordDao
) : UsageRepository {

    override suspend fun upsertRecord(packageName: String, dateEpochDay: Int, totalTimeMs: Long) {
        dao.upsert(packageName, dateEpochDay, totalTimeMs, System.currentTimeMillis())
    }

    override suspend fun getRecord(packageName: String, dateEpochDay: Int): UsageRecord? =
        dao.getRecord(packageName, dateEpochDay)?.toDomain()

    override fun observeRecordsForDay(dateEpochDay: Int): Flow<List<UsageRecord>> =
        dao.observeRecordsForDay(dateEpochDay).map { list -> list.map { it.toDomain() } }

    override suspend fun getRecordsForDay(dateEpochDay: Int): List<UsageRecord> =
        dao.getRecordsForDay(dateEpochDay).map { it.toDomain() }

    override suspend fun deleteOlderThan(dateEpochDay: Int) =
        dao.deleteOlderThan(dateEpochDay)
}
