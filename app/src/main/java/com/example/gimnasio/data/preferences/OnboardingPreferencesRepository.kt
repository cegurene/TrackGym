package com.example.gimnasio.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_preferences")

class OnboardingPreferencesRepository(private val context: Context) {
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")

    val onboardingCompletedFlow: Flow<Boolean> = context.onboardingDataStore.data.map { preferences ->
        preferences[onboardingCompletedKey] ?: false
    }

    suspend fun markOnboardingCompleted() {
        context.onboardingDataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = true
        }
    }
}

