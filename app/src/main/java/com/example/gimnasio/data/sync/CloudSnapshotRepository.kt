package com.example.gimnasio.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gimnasio.data.auth.awaitResult
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first

private val Context.cloudSnapshotStore by preferencesDataStore(name = "cloud_snapshot_store")

interface CloudSnapshotRepository {
    suspend fun saveSnapshot(userId: String, payload: String)
    suspend fun getSnapshot(userId: String): String?
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

    override suspend fun getSnapshot(userId: String): String? {
        val key = stringPreferencesKey("snapshot_${userId}")
        return context.cloudSnapshotStore.data.first()[key]
    }
}

class FirebaseCloudSnapshotRepository(
    private val context: Context
) : CloudSnapshotRepository {
    private val snapshotsCollection = "user_snapshots"

    override suspend fun saveSnapshot(userId: String, payload: String) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseFirestore.getInstance()
            .collection(snapshotsCollection)
            .document(userId)
            .set(
                mapOf(
                    "payload" to payload,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .awaitResult()
    }

    override suspend fun getSnapshot(userId: String): String? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        val document = FirebaseFirestore.getInstance()
            .collection(snapshotsCollection)
            .document(userId)
            .get()
            .awaitResult()
        return document.getString("payload")
    }
}

class HybridCloudSnapshotRepository(
    context: Context
) : CloudSnapshotRepository {
    private val localRepository = LocalCloudSnapshotRepository(context)
    private val firebaseRepository = FirebaseCloudSnapshotRepository(context)

    override suspend fun saveSnapshot(userId: String, payload: String) {
        val remoteSaved = runCatching {
            firebaseRepository.saveSnapshot(userId, payload)
        }.isSuccess

        if (!remoteSaved) {
            localRepository.saveSnapshot(userId, payload)
        }
    }

    override suspend fun getSnapshot(userId: String): String? {
        val remotePayload = runCatching {
            firebaseRepository.getSnapshot(userId)
        }.getOrNull()

        if (!remotePayload.isNullOrBlank()) {
            localRepository.saveSnapshot(userId, remotePayload)
            return remotePayload
        }

        return localRepository.getSnapshot(userId)
    }
}

