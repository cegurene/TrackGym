package com.example.gimnasio.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.cloudSnapshotStore by preferencesDataStore(name = "cloud_snapshot_store")

interface CloudSnapshotRepository {
    suspend fun saveSnapshot(userId: String, payload: String)
}

class LocalCloudSnapshotRepository(
    private val context: Context
) : CloudSnapshotRepository {
    override suspend fun saveSnapshot(userId: String, payload: String) {
        val key = stringPreferencesKey("snapshot_${userId}")
        context.cloudSnapshotStore.edit { prefs ->
            prefs[key] = payload
        }
    }
}

