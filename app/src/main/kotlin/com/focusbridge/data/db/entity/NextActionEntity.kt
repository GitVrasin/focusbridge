package com.focusbridge.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focusbridge.domain.model.NextAction
import com.focusbridge.domain.model.NextActionType

@Entity(
    tableName = "next_actions",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class NextActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val label: String,
    val type: String,  // NextActionType.name()
    val target: String,
    val sortOrder: Int
) {
    fun toDomain() = NextAction(
        id = id,
        goalId = goalId,
        label = label,
        type = NextActionType.valueOf(type),
        target = target,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomain(action: NextAction) = NextActionEntity(
            id = action.id,
            goalId = action.goalId,
            label = action.label,
            type = action.type.name,
            target = action.target,
            sortOrder = action.sortOrder
        )
    }
}
