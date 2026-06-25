package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.InterventionEvent
import com.focusbridge.domain.repository.InterventionRepository
import com.focusbridge.domain.repository.SessionRepository
import javax.inject.Inject

class RecordInterventionUseCase @Inject constructor(
    private val interventionRepository: InterventionRepository,
    private val sessionRepository: SessionRepository
) {
    suspend fun recordTriggered(packageName: String, usageMs: Long, sessionId: Long = 0L): Long {
        return interventionRepository.recordEvent(
            InterventionEvent(
                packageName = packageName,
                triggeredAt = System.currentTimeMillis(),
                usageAtTriggerMs = usageMs,
                sessionId = sessionId
            )
        )
    }

    suspend fun recordIntentSelected(eventId: Long, sessionId: Long, intentType: String) {
        interventionRepository.updateIntentType(eventId, intentType)
        if (sessionId > 0L) {
            sessionRepository.updateIntent(sessionId, intentType)
        }
    }

    suspend fun recordAccepted(eventId: Long, nextActionId: Long) {
        interventionRepository.updateAccepted(
            eventId = eventId,
            nextActionId = nextActionId,
            acceptedAt = System.currentTimeMillis()
        )
    }

    suspend fun recordDismissed(eventId: Long) {
        interventionRepository.updateDismissed(eventId)
    }
}
