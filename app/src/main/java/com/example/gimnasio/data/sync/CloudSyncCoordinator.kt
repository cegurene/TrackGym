package com.example.gimnasio.data.sync

import android.content.Context
import com.example.gimnasio.data.preferences.AccountPreferencesRepository
import kotlinx.coroutines.flow.first

class CloudSyncCoordinator(context: Context) {
    private val accountPreferencesRepository = AccountPreferencesRepository(context)
    private val syncService = InitialCloudSyncService(
        context = context,
        accountPreferencesRepository = accountPreferencesRepository
    )

    suspend fun syncNow() {
        val session = accountPreferencesRepository.accountSessionFlow.first()
        syncService.syncIfNeeded(session)
    }
}

