package com.focusbridge.data.repository

import com.focusbridge.data.db.dao.InterventionEventDao
import com.focusbridge.data.db.entity.InterventionEventEntity
import com.focusbridge.domain.model.InterventionEvent
import com.focusbridge.domain.repository.InterventionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class InterventionRepositoryImpl @Inject constructor(
    private val dao: InterventionEventDao
) : InterventionRepository {

    override suspend fun recordEvent(event: InterventionEvent, isExtension: Boolean): Long =
        dao.insert(InterventionEventEntity.fromDomain(event, isExtension))

    override suspend fun updateAccepted(eventId: Long, nextActionId: Long, acceptedAt: Long) =
        dao.updateAccepted(eventId, nextActionId, acceptedAt)

    override suspend fun updateDismissed(eventId: Long) =
        dao.updateDismissed(eventId)

    override suspend fun updateIntentType(eventId: Long, intentType: String) =
        dao.updateIntentType(eventId, intentType)

    override suspend fun getLastTriggerTime(packageName: String): Long =
        dao.getLastTriggerTime(packageName) ?: 0L

    override fun observeEventsForDay(dateEpochDay: Int): Flow<List<InterventionEvent>> {
        val (start, end) = dayBounds(dateEpochDay)
        return dao.observeEventsForDay(start, end).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEventsForDay(dateEpochDay: Int): List<InterventionEvent> {
        val (start, end) = dayBounds(dateEpochDay)
        return dao.getEventsForDay(start, end).map { it.toDomain() }
    }

    override suspend fun getExtensionCount(packageName: String, dateEpochDay: Int): Int {
        val (start, end) = dayBounds(dateEpochDay)
        return dao.getExtensionCount(packageName, start, end)
    }

    private fun dayBounds(epochDay: Int): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.ofEpochDay(epochDay.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.ofEpochDay(epochDay.toLong() + 1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}
