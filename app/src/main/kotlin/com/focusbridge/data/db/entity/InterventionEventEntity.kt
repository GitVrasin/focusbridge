package com.focusbridge.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focusbridge.domain.model.InterventionEvent

@Entity(
    tableName = "intervention_events",
    indices = [
        Index("packageName"),
        Index("triggeredAt")
    ]
)
data class InterventionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val triggeredAt: Long,
    val usageAtTriggerMs: Long,
    val sessionId: Long = 0,
    val intentType: String? = null,
    val nextActionId: Long?,
    val wasAccepted: Boolean?,
    val acceptedAt: Long?,
    val isExtension: Boolean = false
) {
    fun toDomain() = InterventionEvent(
        id = id,
        packageName = packageName,
        triggeredAt = triggeredAt,
        usageAtTriggerMs = usageAtTriggerMs,
        sessionId = sessionId,
        intentType = intentType,
        nextActionId = nextActionId,
        wasAccepted = wasAccepted,
        acceptedAt = acceptedAt
    )

    companion object {
        fun fromDomain(event: InterventionEvent, isExtension: Boolean = false) =
            InterventionEventEntity(
                id = event.id,
                packageName = event.packageName,
                triggeredAt = event.triggeredAt,
                usageAtTriggerMs = event.usageAtTriggerMs,
                sessionId = event.sessionId,
                intentType = event.intentType,
                nextActionId = event.nextActionId,
                wasAccepted = event.wasAccepted,
                acceptedAt = event.acceptedAt,
                isExtension = isExtension
            )
    }
}
