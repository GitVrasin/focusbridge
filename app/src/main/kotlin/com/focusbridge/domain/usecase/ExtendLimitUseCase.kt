package com.focusbridge.domain.usecase

import com.focusbridge.domain.repository.AppConfigRepository
import com.focusbridge.domain.repository.InterventionRepository
import java.time.LocalDate
import javax.inject.Inject

class ExtendLimitUseCase @Inject constructor(
    private val appConfigRepository: AppConfigRepository,
    private val interventionRepository: InterventionRepository
) {
    companion object {
        const val EXTENSION_MS = 5 * 60 * 1000L  // 5 minutes
        const val MAX_EXTENSIONS_PER_DAY = 2
    }

    suspend operator fun invoke(packageName: String): ExtendResult {
        val today = LocalDate.now().toEpochDay().toInt()
        val extensionCount = interventionRepository.getExtensionCount(packageName, today)

        if (extensionCount >= MAX_EXTENSIONS_PER_DAY) {
            return ExtendResult.LimitReached
        }

        val app = appConfigRepository.getApp(packageName) ?: return ExtendResult.AppNotFound
        appConfigRepository.updateDailyLimit(packageName, app.dailyLimitMs + EXTENSION_MS)
        return ExtendResult.Extended(newLimitMs = app.dailyLimitMs + EXTENSION_MS)
    }

    sealed interface ExtendResult {
        data class Extended(val newLimitMs: Long) : ExtendResult
        data object LimitReached : ExtendResult
        data object AppNotFound : ExtendResult
    }
}
