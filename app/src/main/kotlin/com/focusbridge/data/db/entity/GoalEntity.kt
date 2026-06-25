package com.focusbridge.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.focusbridge.domain.model.Goal

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val createdAt: Long,
    val isActive: Boolean
) {
    fun toDomain() = Goal(
        id = id,
        title = title,
        description = description,
        createdAt = createdAt,
        isActive = isActive
    )

    companion object {
        fun fromDomain(goal: Goal) = GoalEntity(
            id = goal.id,
            title = goal.title,
            description = goal.description,
            createdAt = goal.createdAt,
            isActive = goal.isActive
        )
    }
}
