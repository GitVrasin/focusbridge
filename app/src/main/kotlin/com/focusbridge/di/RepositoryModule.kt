package com.focusbridge.di

import com.focusbridge.data.repository.AppConfigRepositoryImpl
import com.focusbridge.data.repository.GoalRepositoryImpl
import com.focusbridge.data.repository.InterventionRepositoryImpl
import com.focusbridge.data.repository.UsageRepositoryImpl
import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.GoalRepository
import com.focusbridge.domain.repository.InterventionRepository
import com.focusbridge.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds @Singleton
    abstract fun bindAppConfigRepository(impl: AppConfigRepositoryImpl): AppConfigRepository

    @Binds @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds @Singleton
    abstract fun bindInterventionRepository(impl: InterventionRepositoryImpl): InterventionRepository
}
