package com.focusbridge.data.repository

import com.focusbridge.data.db.dao.GoalDao
import com.focusbridge.data.db.entity.GoalEntity
import com.focusbridge.data.db.entity.NextActionEntity
import com.focusbridge.domain.model.Goal
import com.focusbridge.domain.model.NextAction
import com.focusbridge.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override fun observeActiveGoal(): Flow<Goal?> =
        goalDao.observeActiveGoal().map { it?.toDomain() }

    override suspend fun getActiveGoal(): Goal? =
        goalDao.getActiveGoal()?.toDomain()

    override suspend fun upsertGoal(goal: Goal): Long {
        val id = goalDao.upsertGoal(GoalEntity.fromDomain(goal))
        goalDao.deactivateOtherGoals(id)
        return id
    }

    override suspend fun getNextActionsForGoal(goalId: Long): List<NextAction> =
        goalDao.getNextActions(goalId).map { it.toDomain() }

    override fun observeNextActionsForGoal(goalId: Long): Flow<List<NextAction>> =
        goalDao.observeNextActions(goalId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertNextAction(action: NextAction): Long =
        goalDao.upsertNextAction(NextActionEntity.fromDomain(action))

    override suspend fun deleteNextAction(actionId: Long) =
        goalDao.deleteNextAction(actionId)

    override suspend fun getNextAction(actionId: Long): NextAction? =
        goalDao.getNextAction(actionId)?.toDomain()
}
