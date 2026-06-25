package com.focusbridge.domain.repository

import com.focusbridge.domain.model.AppSession

interface SessionRepository {
    suspend fun startSession(packageName: String, startMs: Long): Long
    suspend fun closeSession(sessionId: Long, endMs: Long, durationMs: Long, interventionTriggered: Boolean)
    suspend fun markInterventionTriggered(sessionId: Long)
    suspend fun updateIntent(sessionId: Long, intentType: String)
    suspend fun getOpenSessions(todayStartMs: Long): List<AppSession>
    suspend fun getSessionsInRange(fromMs: Long, toMs: Long): List<AppSession>
    suspend fun getTotalDurationMs(fromMs: Long, toMs: Long): Long
    suspend fun getCountByIntent(fromMs: Long, toMs: Long, intentType: String): Int
    suspend fun getSessionCount(fromMs: Long, toMs: Long): Int
}
