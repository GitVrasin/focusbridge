package com.focusbridge.service

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session state machine. Tracks active foreground sessions per package.
 *
 * A session begins when the app moves to foreground and ends when it has been
 * in the background for longer than SESSION_GAP_MS (60 seconds).
 *
 * This class is purely in-memory. Callers are responsible for persisting sessions to the DB.
 */
@Singleton
class SessionTracker @Inject constructor() {

    data class ActiveSession(
        val dbId: Long,
        val packageName: String,
        val startMs: Long,
        val intentType: String? = null,
        val interventionTriggered: Boolean = false,
        val lastForegroundMs: Long
    )

    private val TAG = "FocusBridge.Sessions"
    private val SESSION_GAP_MS = 60_000L

    private val sessions = mutableMapOf<String, ActiveSession>()

    /**
     * Call when the app has been observed in the foreground.
     * Returns true if this is the start of a new session.
     */
    fun onForeground(packageName: String, nowMs: Long, dbId: Long = 0): Boolean {
        val current = sessions[packageName]
        return if (current == null) {
            sessions[packageName] = ActiveSession(
                dbId = dbId,
                packageName = packageName,
                startMs = nowMs,
                lastForegroundMs = nowMs
            )
            Log.d(TAG, "New session started: $packageName at $nowMs")
            true
        } else {
            sessions[packageName] = current.copy(lastForegroundMs = nowMs)
            false
        }
    }

    /**
     * Call on each poll tick for apps not currently in the foreground.
     * Returns the ended session if the gap threshold was exceeded, null otherwise.
     */
    fun checkSessionEnd(packageName: String, nowMs: Long): ActiveSession? {
        val current = sessions[packageName] ?: return null
        return if (nowMs - current.lastForegroundMs > SESSION_GAP_MS) {
            sessions.remove(packageName)
            Log.d(TAG, "Session ended: $packageName duration=${nowMs - current.startMs}ms")
            current
        } else {
            null
        }
    }

    fun setDbId(packageName: String, dbId: Long) {
        sessions[packageName]?.let { sessions[packageName] = it.copy(dbId = dbId) }
    }

    fun setIntentType(packageName: String, intentType: String) {
        sessions[packageName]?.let { sessions[packageName] = it.copy(intentType = intentType) }
    }

    fun markInterventionTriggered(packageName: String) {
        sessions[packageName]?.let { sessions[packageName] = it.copy(interventionTriggered = true) }
    }

    fun hasInterventionTriggered(packageName: String): Boolean =
        sessions[packageName]?.interventionTriggered == true

    fun getSession(packageName: String): ActiveSession? = sessions[packageName]

    fun getSessionDurationMs(packageName: String, nowMs: Long): Long =
        sessions[packageName]?.let { nowMs - it.startMs } ?: 0L

    /** Restore a session from DB on service restart (crash recovery). */
    fun restoreSession(packageName: String, dbId: Long, startMs: Long, intentType: String?) {
        if (!sessions.containsKey(packageName)) {
            sessions[packageName] = ActiveSession(
                dbId = dbId,
                packageName = packageName,
                startMs = startMs,
                intentType = intentType,
                lastForegroundMs = System.currentTimeMillis()
            )
            Log.d(TAG, "Restored session: $packageName dbId=$dbId")
        }
    }

    /** Force-end all sessions (e.g., on day rollover). */
    fun clearAll() = sessions.clear()

    fun allSessions(): Map<String, ActiveSession> = sessions.toMap()
}
