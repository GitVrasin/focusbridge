package com.focusbridge.domain.repository

import com.focusbridge.domain.model.UsageRecord
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    suspend fun upsertRecord(packageName: String, dateEpochDay: Int, totalTimeMs: Long)
    suspend fun getRecord(packageName: String, dateEpochDay: Int): UsageRecord?
    fun observeRecordsForDay(dateEpochDay: Int): Flow<List<UsageRecord>>
    suspend fun getRecordsForDay(dateEpochDay: Int): List<UsageRecord>
    suspend fun deleteOlderThan(dateEpochDay: Int)
}
