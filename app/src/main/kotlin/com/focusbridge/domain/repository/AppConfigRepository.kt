package com.focusbridge.domain.repository

import com.focusbridge.domain.model.DistractingApp
import kotlinx.coroutines.flow.Flow

interface AppConfigRepository {
    fun observeActiveApps(): Flow<List<DistractingApp>>
    suspend fun getActiveApps(): List<DistractingApp>
    suspend fun upsertApp(app: DistractingApp)
    suspend fun removeApp(packageName: String)
    suspend fun updateDailyLimit(packageName: String, limitMs: Long)
    suspend fun getApp(packageName: String): DistractingApp?
}
