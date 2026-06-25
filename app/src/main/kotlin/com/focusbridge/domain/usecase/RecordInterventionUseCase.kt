package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.InterventionEvent
import com.focusbridge.domain.repository.InterventionRepository
import javax.inject.Inject

class RecordInterventionUseCase @Inject constructor(
    private val interventionRepository: InterventionRepository
) {
    suspend fun recordTriggered(packageName: String, usageMs: Long): Long {
        return interventionRepository.recordEvent(
            InterventionEvent(
                packageName = packageName,
                triggeredAt = System.currentTimeMillis(),
                usageAtTriggerMs = usageMs
            )
        )
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

    suspend fun recordExtension(packageName: String, usageMs: Long): Long {
        return interventionRepository.recordEvent(
            InterventionEvent(
                packageName = packageName,
                triggeredAt = System.currentTimeMillis(),
                usageAtTriggerMs = usageMs
            ),
            isExtension = true
        )
    }
}
