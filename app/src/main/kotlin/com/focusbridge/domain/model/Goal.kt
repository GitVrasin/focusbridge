package com.focusbridge.domain.model

data class Goal(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
