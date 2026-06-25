package com.focusbridge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusbridge.data.db.entity.DistractingAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistractingAppDao {

    @Query("SELECT * FROM distracting_apps WHERE isActive = 1")
    fun observeActiveApps(): Flow<List<DistractingAppEntity>>

    @Query("SELECT * FROM distracting_apps WHERE isActive = 1")
    suspend fun getActiveApps(): List<DistractingAppEntity>

    @Query("SELECT * FROM distracting_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): DistractingAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: DistractingAppEntity)

    @Query("UPDATE distracting_apps SET isActive = 0 WHERE packageName = :packageName")
    suspend fun removeApp(packageName: String)

    @Query("UPDATE distracting_apps SET dailyLimitMs = :limitMs WHERE packageName = :packageName")
    suspend fun updateDailyLimit(packageName: String, limitMs: Long)
}
