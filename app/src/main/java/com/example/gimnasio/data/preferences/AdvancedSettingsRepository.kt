package com.example.gimnasio.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.advancedSettingsDataStore by preferencesDataStore(
    name = "advanced_settings"
)

class AdvancedSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.advancedSettingsDataStore

    private val advancedModificationsKey =
        booleanPreferencesKey("advanced_modifications_enabled")

    val advancedModificationsFlow: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[advancedModificationsKey] ?: false
        }

    suspend fun setAdvancedModificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[advancedModificationsKey] = enabled
        }
    }
}