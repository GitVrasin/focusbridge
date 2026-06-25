package com.focusbridge.data.repository

import com.focusbridge.data.db.dao.AppSessionDao
import com.focusbridge.data.db.entity.AppSessionEntity
import com.focusbridge.domain.model.AppSession
import com.focusbridge.domain.repository.SessionRepository
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val dao: AppSessionDao
) : SessionRepository {

    override suspend fun startSession(packageName: String, startMs: Long): Long =
        dao.insert(AppSessionEntity(packageName = packageName, sessionStartMs = startMs))

    override suspend fun closeSession(sessionId: Long, endMs: Long, durationMs: Long, interventionTriggered: Boolean) =
        dao.closeSession(sessionId, endMs, durationMs, interventionTriggered)

    override suspend fun markInterventionTriggered(sessionId: Long) =
        dao.markInterventionTriggered(sessionId)

    override suspend fun updateIntent(sessionId: Long, intentType: String) =
        dao.updateIntentType(sessionId, intentType)

    override suspend fun getOpenSessions(todayStartMs: Long): List<AppSession> =
        dao.getOpenSessions(todayStartMs).map { it.toDomain() }

    override suspend fun getSessionsInRange(fromMs: Long, toMs: Long): List<AppSession> =
        dao.getSessionsInRange(fromMs, toMs).map { it.toDomain() }

    override suspend fun getTotalDurationMs(fromMs: Long, toMs: Long): Long =
        dao.getTotalDurationMs(fromMs, toMs)

    override suspend fun getCountByIntent(fromMs: Long, toMs: Long, intentType: String): Int =
        dao.getCountByIntent(fromMs, toMs, intentType)

    override suspend fun getSessionCount(fromMs: Long, toMs: Long): Int =
        dao.getSessionCount(fromMs, toMs)
}
