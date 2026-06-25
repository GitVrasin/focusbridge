package com.focusbridge.domain.model

data class UsageRecord(
    val id: Long = 0,
    val packageName: String,
    val dateEpochDay: Int,
    val totalTimeMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
