package com.focusbridge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focusbridge.data.db.entity.AppSessionEntity

@Dao
interface AppSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: AppSessionEntity): Long

    @Update
    suspend fun update(session: AppSessionEntity)

    @Query("SELECT * FROM app_sessions WHERE id = :id")
    suspend fun getById(id: Long): AppSessionEntity?

    @Query("SELECT * FROM app_sessions WHERE sessionEndMs IS NULL AND sessionStartMs >= :todayStartMs")
    suspend fun getOpenSessions(todayStartMs: Long): List<AppSessionEntity>

    @Query("UPDATE app_sessions SET intentType = :intentType WHERE id = :sessionId")
    suspend fun updateIntentType(sessionId: Long, intentType: String)

    @Query("""
        UPDATE app_sessions
        SET sessionEndMs = :endMs, durationMs = :durationMs, interventionTriggered = :triggered
        WHERE id = :sessionId
    """)
    suspend fun closeSession(sessionId: Long, endMs: Long, durationMs: Long, triggered: Boolean)

    @Query("UPDATE app_sessions SET interventionTriggered = 1 WHERE id = :sessionId")
    suspend fun markInterventionTriggered(sessionId: Long)

    @Query("""
        SELECT * FROM app_sessions
        WHERE sessionStartMs >= :fromMs AND sessionStartMs < :toMs
        ORDER BY sessionStartMs ASC
    """)
    suspend fun getSessionsInRange(fromMs: Long, toMs: Long): List<AppSessionEntity>

    @Query("""
        SELECT COALESCE(SUM(durationMs), 0) FROM app_sessions
        WHERE sessionStartMs >= :fromMs AND sessionStartMs < :toMs
    """)
    suspend fun getTotalDurationMs(fromMs: Long, toMs: Long): Long

    @Query("""
        SELECT COUNT(*) FROM app_sessions
        WHERE sessionStartMs >= :fromMs AND sessionStartMs < :toMs
        AND intentType = :intentType
    """)
    suspend fun getCountByIntent(fromMs: Long, toMs: Long, intentType: String): Int

    @Query("""
        SELECT COUNT(*) FROM app_sessions
        WHERE sessionStartMs >= :fromMs AND sessionStartMs < :toMs
    """)
    suspend fun getSessionCount(fromMs: Long, toMs: Long): Int
}
