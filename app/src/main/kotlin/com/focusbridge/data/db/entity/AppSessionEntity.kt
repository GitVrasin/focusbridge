package com.focusbridge.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focusbridge.domain.model.AppSession

@Entity(
    tableName = "app_sessions",
    indices = [
        Index("packageName"),
        Index("sessionStartMs")
    ]
)
data class AppSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val sessionStartMs: Long,
    val sessionEndMs: Long? = null,
    val intentType: String? = null,
    val durationMs: Long = 0,
    val interventionTriggered: Boolean = false
) {
    fun toDomain() = AppSession(
        id = id,
        packageName = packageName,
        sessionStartMs = sessionStartMs,
        sessionEndMs = sessionEndMs,
        intentType = intentType,
        durationMs = durationMs,
        interventionTriggered = interventionTriggered
    )
}
