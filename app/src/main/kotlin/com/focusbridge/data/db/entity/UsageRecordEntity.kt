package com.focusbridge.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import com.focusbridge.domain.model.UsageRecord

@Entity(
    tableName = "usage_records",
    primaryKeys = ["packageName", "dateEpochDay"],
    indices = [Index("dateEpochDay")]
)
data class UsageRecordEntity(
    val packageName: String,
    val dateEpochDay: Int,
    val totalTimeMs: Long,
    val updatedAt: Long
) {
    fun toDomain() = UsageRecord(
        packageName = packageName,
        dateEpochDay = dateEpochDay,
        totalTimeMs = totalTimeMs,
        updatedAt = updatedAt
    )
}
