package com.example.gimnasio.data.sync

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.preferences.AccountPreferencesRepository
import com.example.gimnasio.data.preferences.AccountSession
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

class InitialCloudSyncService(
    context: Context,
    private val accountPreferencesRepository: AccountPreferencesRepository,
    private val cloudSnapshotRepository: CloudSnapshotRepository = HybridCloudSnapshotRepository(context)
) {
    private val database = GymDatabase.getDatabase(context)
    private val tables = listOf(
        "rutinas",
        "ejercicios",
        "rutina_ejercicio",
        "entrenamientos",
        "entrenamiento_ejercicio",
        "series"
    )

    suspend fun syncIfNeeded(session: AccountSession) {
        val loggedSession = session as? AccountSession.LoggedIn ?: return
        val userId = resolveUserId(loggedSession)
        val db = database.openHelper.writableDatabase

        val localHasData = hasLocalData(db)
        val remotePayload = runCatching { cloudSnapshotRepository.getSnapshot(userId) }.getOrNull()

        when {
            !localHasData && !remotePayload.isNullOrBlank() -> {
                restoreSnapshot(db, remotePayload)
            }
            localHasData -> {
                val payload = buildSnapshotPayload(db)
                if (payload != remotePayload) {
                    cloudSnapshotRepository.saveSnapshot(userId = userId, payload = payload)
                }
            }
        }

        if (loggedSession.needsInitialSync) {
            accountPreferencesRepository.markInitialSyncCompleted()
        }
    }

    private fun buildSnapshotPayload(db: SupportSQLiteDatabase): String {
        return buildString {
            append("{\"snapshotVersion\":1,\"createdAt\":")
            append(System.currentTimeMillis())
            append(",\"tables\":{")
            tables.forEachIndexed { index, table ->
                if (index > 0) append(',')
                append('"').append(table).append("\":")
                append(dumpTableAsJson(db, table))
            }
            append("}}")
        }
    }

    private fun dumpTableAsJson(db: SupportSQLiteDatabase, tableName: String): String {
        val cursor = db.query("SELECT * FROM $tableName")
        cursor.use { c ->
            val columnNames = c.columnNames
            val builder = StringBuilder("[")
            var rowIndex = 0

            while (c.moveToNext()) {
                if (rowIndex > 0) builder.append(',')
                builder.append('{')

                columnNames.forEachIndexed { columnIndex, columnName ->
                    if (columnIndex > 0) builder.append(',')
                    builder.append('"').append(columnName).append("\":")
                    builder.append(readColumnAsJson(c, columnIndex))
                }

                builder.append('}')
                rowIndex++
            }

            builder.append(']')
            return builder.toString()
        }
    }

    private fun readColumnAsJson(
        cursor: android.database.Cursor,
        columnIndex: Int
    ): String {
        return when (cursor.getType(columnIndex)) {
            android.database.Cursor.FIELD_TYPE_NULL -> "null"
            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(columnIndex).toString()
            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(columnIndex).toString()
            android.database.Cursor.FIELD_TYPE_BLOB -> {
                val bytes = cursor.getBlob(columnIndex)
                val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                "\"$encoded\""
            }
            else -> "\"${escapeJson(cursor.getString(columnIndex))}\""
        }
    }

    private fun escapeJson(value: String?): String {
        if (value == null) return ""
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun hasLocalData(db: SupportSQLiteDatabase): Boolean {
        return tables.any { table ->
            db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                cursor.moveToFirst() && cursor.getLong(0) > 0L
            }
        }
    }

    private fun restoreSnapshot(db: SupportSQLiteDatabase, payload: String) {
        runCatching {
            val tablesJson = JSONObject(payload).optJSONObject("tables") ?: return@runCatching
            db.beginTransaction()
            try {
                tables.forEach { table ->
                    db.execSQL("DELETE FROM $table")
                    val rows = tablesJson.optJSONArray(table) ?: JSONArray()
                    for (index in 0 until rows.length()) {
                        val rowObject = rows.optJSONObject(index) ?: continue
                        val values = ContentValues()
                        val keys = rowObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            putJsonValue(values, key, rowObject.opt(key))
                        }
                        db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.onFailure { error ->
            Log.e("InitialCloudSync", "No se pudo restaurar snapshot", error)
        }
    }

    private fun putJsonValue(values: ContentValues, key: String, rawValue: Any?) {
        when (rawValue) {
            null, JSONObject.NULL -> values.putNull(key)
            is Int -> values.put(key, rawValue)
            is Long -> values.put(key, rawValue)
            is Float -> values.put(key, rawValue)
            is Double -> values.put(key, rawValue)
            is Boolean -> values.put(key, if (rawValue) 1 else 0)
            else -> values.put(key, rawValue.toString())
        }
    }

    private fun resolveUserId(session: AccountSession.LoggedIn): String {
        val firebaseUserId = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
        return if (!firebaseUserId.isNullOrBlank()) {
            firebaseUserId
        } else {
            session.email.replace("[^a-z0-9_]".toRegex(), "_")
        }
    }
}

