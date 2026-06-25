package com.focusbridge.di

import android.content.Context
import androidx.room.Room
import com.focusbridge.data.db.AppDatabase
import com.focusbridge.data.db.dao.DistractingAppDao
import com.focusbridge.data.db.dao.GoalDao
import com.focusbridge.data.db.dao.InterventionEventDao
import com.focusbridge.data.db.dao.UsageRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .build()

    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
    @Provides fun provideAppDao(db: AppDatabase): DistractingAppDao = db.distractingAppDao()
    @Provides fun provideUsageDao(db: AppDatabase): UsageRecordDao = db.usageRecordDao()
    @Provides fun provideInterventionDao(db: AppDatabase): InterventionEventDao = db.interventionEventDao()
}
