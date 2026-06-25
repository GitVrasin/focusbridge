package com.focusbridge.domain.model

data class AppSession(
    val id: Long = 0,
    val packageName: String,
    val sessionStartMs: Long,
    val sessionEndMs: Long? = null,
    val intentType: String? = null,
    val durationMs: Long = 0,
    val interventionTriggered: Boolean = false
)
