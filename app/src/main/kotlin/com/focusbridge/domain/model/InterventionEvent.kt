package com.focusbridge.domain.model

data class InterventionEvent(
    val id: Long = 0,
    val packageName: String,
    val triggeredAt: Long,
    val usageAtTriggerMs: Long,
    val nextActionId: Long? = null,
    val wasAccepted: Boolean? = null, // null = dismissed without choosing
    val acceptedAt: Long? = null
)
