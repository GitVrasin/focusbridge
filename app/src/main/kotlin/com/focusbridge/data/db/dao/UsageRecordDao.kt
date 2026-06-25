package com.focusbridge.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.focusbridge.data.db.entity.UsageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {

    @Query("""
        INSERT OR REPLACE INTO usage_records (packageName, dateEpochDay, totalTimeMs, updatedAt)
        VALUES (:packageName, :dateEpochDay, :totalTimeMs, :updatedAt)
    """)
    suspend fun upsert(packageName: String, dateEpochDay: Int, totalTimeMs: Long, updatedAt: Long)

    @Query("SELECT * FROM usage_records WHERE packageName = :packageName AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getRecord(packageName: String, dateEpochDay: Int): UsageRecordEntity?

    @Query("SELECT * FROM usage_records WHERE dateEpochDay = :dateEpochDay")
    fun observeRecordsForDay(dateEpochDay: Int): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_records WHERE dateEpochDay = :dateEpochDay")
    suspend fun getRecordsForDay(dateEpochDay: Int): List<UsageRecordEntity>

    @Query("DELETE FROM usage_records WHERE dateEpochDay < :dateEpochDay")
    suspend fun deleteOlderThan(dateEpochDay: Int)
}
