package com.example.oraclepredictanything.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oracle_settings")

class AppPreferences(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "DARK", "LIGHT"
        val KEY_DEFAULT_HORIZON = intPreferencesKey("default_horizon")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "DARK" // Dark mode as hero
    }

    val defaultHorizon: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_HORIZON] ?: 7
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setDefaultHorizon(horizon: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_HORIZON] = horizon.coerceIn(1, 30)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}
