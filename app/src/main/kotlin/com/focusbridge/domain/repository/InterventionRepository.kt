package com.focusbridge.domain.repository

import com.focusbridge.domain.model.InterventionEvent
import kotlinx.coroutines.flow.Flow

interface InterventionRepository {
    suspend fun recordEvent(event: InterventionEvent, isExtension: Boolean = false): Long
    suspend fun updateAccepted(eventId: Long, nextActionId: Long, acceptedAt: Long)
    suspend fun updateDismissed(eventId: Long)
    suspend fun updateIntentType(eventId: Long, intentType: String)
    suspend fun getLastTriggerTime(packageName: String): Long
    fun observeEventsForDay(dateEpochDay: Int): Flow<List<InterventionEvent>>
    suspend fun getEventsForDay(dateEpochDay: Int): List<InterventionEvent>
    suspend fun getExtensionCount(packageName: String, dateEpochDay: Int): Int
}
