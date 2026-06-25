package com.focusbridge.domain.repository

import com.focusbridge.domain.model.Goal
import com.focusbridge.domain.model.NextAction
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeActiveGoal(): Flow<Goal?>
    suspend fun getActiveGoal(): Goal?
    suspend fun upsertGoal(goal: Goal): Long
    suspend fun getNextActionsForGoal(goalId: Long): List<NextAction>
    fun observeNextActionsForGoal(goalId: Long): Flow<List<NextAction>>
    suspend fun upsertNextAction(action: NextAction): Long
    suspend fun deleteNextAction(actionId: Long)
    suspend fun getNextAction(actionId: Long): NextAction?
}
