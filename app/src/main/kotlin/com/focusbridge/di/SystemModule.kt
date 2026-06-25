package com.focusbridge.di

import com.focusbridge.data.system.SystemUsageStatsWrapper
import com.focusbridge.data.system.UsageStatsWrapper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {

    @Binds @Singleton
    abstract fun bindUsageStatsWrapper(impl: SystemUsageStatsWrapper): UsageStatsWrapper
}
