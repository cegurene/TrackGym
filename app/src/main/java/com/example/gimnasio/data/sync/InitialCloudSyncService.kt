package com.example.gimnasio.data.sync

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.preferences.AccountPreferencesRepository
import com.example.gimnasio.data.preferences.AccountSession

class InitialCloudSyncService(
    context: Context,
    private val accountPreferencesRepository: AccountPreferencesRepository,
    private val cloudSnapshotRepository: CloudSnapshotRepository = LocalCloudSnapshotRepository(context)
) {
    private val database = GymDatabase.getDatabase(context)

    suspend fun syncIfNeeded(session: AccountSession) {
        val loggedSession = session as? AccountSession.LoggedIn ?: return
        if (!loggedSession.needsInitialSync) return

        val payload = buildSnapshotPayload(database.openHelper.readableDatabase)
        val userId = loggedSession.email.replace("[^a-z0-9_]".toRegex(), "_")
        cloudSnapshotRepository.saveSnapshot(userId = userId, payload = payload)
        accountPreferencesRepository.markInitialSyncCompleted()
    }

    private fun buildSnapshotPayload(db: SupportSQLiteDatabase): String {
        val tables = listOf(
            "rutinas",
            "ejercicios",
            "rutina_ejercicio",
            "entrenamientos",
            "entrenamiento_ejercicio",
            "series"
        )

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
}

