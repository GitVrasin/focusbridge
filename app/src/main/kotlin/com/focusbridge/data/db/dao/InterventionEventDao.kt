package com.focusbridge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusbridge.data.db.entity.InterventionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: InterventionEventEntity): Long

    @Query("UPDATE intervention_events SET wasAccepted = 1, nextActionId = :nextActionId, acceptedAt = :acceptedAt WHERE id = :eventId")
    suspend fun updateAccepted(eventId: Long, nextActionId: Long, acceptedAt: Long)

    @Query("UPDATE intervention_events SET wasAccepted = 0 WHERE id = :eventId")
    suspend fun updateDismissed(eventId: Long)

    @Query("UPDATE intervention_events SET intentType = :intentType WHERE id = :eventId")
    suspend fun updateIntentType(eventId: Long, intentType: String)

    @Query("""
        SELECT MAX(triggeredAt) FROM intervention_events
        WHERE packageName = :packageName
    """)
    suspend fun getLastTriggerTime(packageName: String): Long?

    @Query("""
        SELECT * FROM intervention_events
        WHERE triggeredAt >= :startOfDayMs AND triggeredAt < :endOfDayMs
        ORDER BY triggeredAt DESC
    """)
    fun observeEventsForDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<InterventionEventEntity>>

    @Query("""
        SELECT * FROM intervention_events
        WHERE triggeredAt >= :startOfDayMs AND triggeredAt < :endOfDayMs
        ORDER BY triggeredAt DESC
    """)
    suspend fun getEventsForDay(startOfDayMs: Long, endOfDayMs: Long): List<InterventionEventEntity>

    @Query("""
        SELECT COUNT(*) FROM intervention_events
        WHERE packageName = :packageName
        AND isExtension = 1
        AND triggeredAt >= :startOfDayMs AND triggeredAt < :endOfDayMs
    """)
    suspend fun getExtensionCount(packageName: String, startOfDayMs: Long, endOfDayMs: Long): Int
}
