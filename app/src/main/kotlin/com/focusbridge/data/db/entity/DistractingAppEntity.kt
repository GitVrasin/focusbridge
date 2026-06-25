package com.focusbridge.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.focusbridge.domain.model.DistractingApp

@Entity(tableName = "distracting_apps")
data class DistractingAppEntity(
    @PrimaryKey
    val packageName: String,
    val displayName: String,
    val iconBase64: String,
    val dailyLimitMs: Long,
    val isActive: Boolean,
    val cooldownMs: Long
) {
    fun toDomain() = DistractingApp(
        packageName = packageName,
        displayName = displayName,
        iconBase64 = iconBase64,
        dailyLimitMs = dailyLimitMs,
        isActive = isActive,
        cooldownMs = cooldownMs
    )

    companion object {
        fun fromDomain(app: DistractingApp) = DistractingAppEntity(
            packageName = app.packageName,
            displayName = app.displayName,
            iconBase64 = app.iconBase64,
            dailyLimitMs = app.dailyLimitMs,
            isActive = app.isActive,
            cooldownMs = app.cooldownMs
        )
    }
}
