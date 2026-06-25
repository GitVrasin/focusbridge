package com.focusbridge.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    val isOnboardingComplete: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val lastServicePingMs: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.LAST_SERVICE_PING_MS] ?: 0L
    }

    // MODE A = false (per-app), MODE B = true (global combined limit)
    val isGlobalLimitMode: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.GLOBAL_LIMIT_MODE] ?: false
    }

    // Combined daily limit used when isGlobalLimitMode = true
    val globalLimitMs: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.GLOBAL_LIMIT_MS] ?: (30 * 60 * 1000L) // default 30 min
    }

    // Epoch-ms timestamp until which all interventions are silenced (0 = not muted)
    val muteUntilMs: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.MUTE_UNTIL_MS] ?: 0L
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        store.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun recordServicePing() {
        store.edit { it[Keys.LAST_SERVICE_PING_MS] = System.currentTimeMillis() }
    }

    suspend fun setGlobalLimitMode(enabled: Boolean) {
        store.edit { it[Keys.GLOBAL_LIMIT_MODE] = enabled }
    }

    suspend fun setGlobalLimitMs(limitMs: Long) {
        store.edit { it[Keys.GLOBAL_LIMIT_MS] = limitMs }
    }

    /** Silences all interventions until midnight tonight. Resets automatically (compared at runtime). */
    suspend fun muteForToday() {
        val midnight = LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        store.edit { it[Keys.MUTE_UNTIL_MS] = midnight }
    }

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LAST_SERVICE_PING_MS = longPreferencesKey("last_service_ping_ms")
        val GLOBAL_LIMIT_MODE = booleanPreferencesKey("global_limit_mode")
        val GLOBAL_LIMIT_MS = longPreferencesKey("global_limit_ms")
        val MUTE_UNTIL_MS = longPreferencesKey("mute_until_ms")
    }
}
