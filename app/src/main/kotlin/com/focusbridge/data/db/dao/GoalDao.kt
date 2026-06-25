package com.focusbridge.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusbridge.data.db.entity.GoalEntity
import com.focusbridge.data.db.entity.NextActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE isActive = 1 LIMIT 1")
    fun observeActiveGoal(): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveGoal(): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity): Long

    @Query("UPDATE goals SET isActive = 0 WHERE id != :keepId")
    suspend fun deactivateOtherGoals(keepId: Long)

    @Query("SELECT * FROM next_actions WHERE goalId = :goalId ORDER BY sortOrder ASC")
    fun observeNextActions(goalId: Long): Flow<List<NextActionEntity>>

    @Query("SELECT * FROM next_actions WHERE goalId = :goalId ORDER BY sortOrder ASC")
    suspend fun getNextActions(goalId: Long): List<NextActionEntity>

    @Query("SELECT * FROM next_actions WHERE id = :actionId LIMIT 1")
    suspend fun getNextAction(actionId: Long): NextActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNextAction(action: NextActionEntity): Long

    @Query("DELETE FROM next_actions WHERE id = :actionId")
    suspend fun deleteNextAction(actionId: Long)
}
