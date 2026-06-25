package com.focusbridge.domain.usecase

import com.focusbridge.domain.model.NextAction
import com.focusbridge.domain.repository.GoalRepository
import javax.inject.Inject

class GetNextActionUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(): NextAction? {
        val goal = goalRepository.getActiveGoal() ?: return null
        return goalRepository.getNextActionsForGoal(goal.id).minByOrNull { it.sortOrder }
    }
}
