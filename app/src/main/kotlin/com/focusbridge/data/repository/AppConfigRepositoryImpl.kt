package com.focusbridge.data.repository

import com.focusbridge.data.db.dao.DistractingAppDao
import com.focusbridge.data.db.entity.DistractingAppEntity
import com.focusbridge.domain.model.DistractingApp
import com.focusbridge.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppConfigRepositoryImpl @Inject constructor(
    private val dao: DistractingAppDao
) : AppConfigRepository {

    override fun observeActiveApps(): Flow<List<DistractingApp>> =
        dao.observeActiveApps().map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveApps(): List<DistractingApp> =
        dao.getActiveApps().map { it.toDomain() }

    override suspend fun upsertApp(app: DistractingApp) =
        dao.upsertApp(DistractingAppEntity.fromDomain(app))

    override suspend fun removeApp(packageName: String) =
        dao.removeApp(packageName)

    override suspend fun updateDailyLimit(packageName: String, limitMs: Long) =
        dao.updateDailyLimit(packageName, limitMs)

    override suspend fun getApp(packageName: String): DistractingApp? =
        dao.getApp(packageName)?.toDomain()
}
