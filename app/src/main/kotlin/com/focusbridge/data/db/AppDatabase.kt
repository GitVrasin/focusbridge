package com.focusbridge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.focusbridge.data.db.dao.DistractingAppDao
import com.focusbridge.data.db.dao.GoalDao
import com.focusbridge.data.db.dao.InterventionEventDao
import com.focusbridge.data.db.dao.UsageRecordDao
import com.focusbridge.data.db.entity.DistractingAppEntity
import com.focusbridge.data.db.entity.GoalEntity
import com.focusbridge.data.db.entity.InterventionEventEntity
import com.focusbridge.data.db.entity.NextActionEntity
import com.focusbridge.data.db.entity.UsageRecordEntity

@Database(
    entities = [
        GoalEntity::class,
        NextActionEntity::class,
        DistractingAppEntity::class,
        UsageRecordEntity::class,
        InterventionEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun distractingAppDao(): DistractingAppDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun interventionEventDao(): InterventionEventDao

    companion object {
        const val DATABASE_NAME = "focusbridge.db"
    }
}
