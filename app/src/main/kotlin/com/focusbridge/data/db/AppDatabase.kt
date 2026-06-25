package com.focusbridge.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focusbridge.data.db.dao.AppSessionDao
import com.focusbridge.data.db.dao.DistractingAppDao
import com.focusbridge.data.db.dao.GoalDao
import com.focusbridge.data.db.dao.InterventionEventDao
import com.focusbridge.data.db.dao.UsageRecordDao
import com.focusbridge.data.db.entity.AppSessionEntity
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
        InterventionEventEntity::class,
        AppSessionEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun distractingAppDao(): DistractingAppDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun interventionEventDao(): InterventionEventDao
    abstract fun appSessionDao(): AppSessionDao

    companion object {
        const val DATABASE_NAME = "focusbridge.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        sessionStartMs INTEGER NOT NULL,
                        sessionEndMs INTEGER,
                        intentType TEXT,
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        interventionTriggered INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_app_sessions_packageName ON app_sessions(packageName)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_app_sessions_sessionStartMs ON app_sessions(sessionStartMs)"
                )
                database.execSQL(
                    "ALTER TABLE intervention_events ADD COLUMN sessionId INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE intervention_events ADD COLUMN intentType TEXT"
                )
            }
        }
    }
}
