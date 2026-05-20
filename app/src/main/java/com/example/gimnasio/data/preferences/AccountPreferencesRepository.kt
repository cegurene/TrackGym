package com.example.gimnasio.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.accountDataStore by preferencesDataStore(name = "account_preferences")

class AccountPreferencesRepository(private val context: Context) {
    private val sessionStateKey = stringPreferencesKey("session_state")
    private val sessionEmailKey = stringPreferencesKey("session_email")
    private val sessionProviderKey = stringPreferencesKey("session_provider")
    private val sessionNeedsSyncKey = booleanPreferencesKey("session_needs_sync")
    private val lastSyncRemoteHashKey = stringPreferencesKey("last_sync_remote_hash")
    private val accountsKey = stringPreferencesKey("accounts")

    val accountSessionFlow: Flow<AccountSession> = context.accountDataStore.data.map { preferences ->
        when (preferences[sessionStateKey]) {
            "logged_in" -> {
                val email = preferences[sessionEmailKey]
                val provider = when (preferences[sessionProviderKey]) {
                    "google" -> AuthProvider.GOOGLE
                    else -> AuthProvider.EMAIL
                }
                val needsSync = preferences[sessionNeedsSyncKey] ?: false
                val lastSyncHash = preferences[lastSyncRemoteHashKey]
                if (email.isNullOrBlank()) {
                    AccountSession.LoggedOut
                } else {
                    AccountSession.LoggedIn(
                        email = email,
                        provider = provider,
                        needsInitialSync = needsSync,
                        lastSyncRemoteHash = lastSyncHash
                    )
                }
            }
            else -> AccountSession.LoggedOut
        }
    }

    suspend fun createAccount(email: String, password: String): AccountActionResult {
        val normalizedEmail = email.trim().lowercase()
        val normalizedPassword = password.trim()
        if (normalizedEmail.isBlank() || normalizedPassword.isBlank()) {
            return AccountActionResult.CREDENCIALES_INVALIDAS
        }

        var result = AccountActionResult.SUCCESS
        context.accountDataStore.edit { preferences ->
            val currentAccounts = decodeAccounts(preferences[accountsKey])
            if (currentAccounts.containsKey(normalizedEmail)) {
                result = AccountActionResult.EMAIL_YA_EXISTE
                return@edit
            }

            val updatedAccounts = currentAccounts + (normalizedEmail to normalizedPassword)
            preferences[accountsKey] = encodeAccounts(updatedAccounts)
            preferences[sessionStateKey] = "logged_in"
            preferences[sessionEmailKey] = normalizedEmail
            preferences[sessionProviderKey] = "email"
            preferences[sessionNeedsSyncKey] = true
        }
        return result
    }

    suspend fun login(email: String, password: String): AccountActionResult {
        val normalizedEmail = email.trim().lowercase()
        val normalizedPassword = password.trim()
        if (normalizedEmail.isBlank() || normalizedPassword.isBlank()) {
            return AccountActionResult.CREDENCIALES_INVALIDAS
        }

        var result = AccountActionResult.SUCCESS
        context.accountDataStore.edit { preferences ->
            val currentAccounts = decodeAccounts(preferences[accountsKey])
            val validPassword = currentAccounts[normalizedEmail]
            if (validPassword != normalizedPassword) {
                result = AccountActionResult.CREDENCIALES_INVALIDAS
                return@edit
            }

            preferences[sessionStateKey] = "logged_in"
            preferences[sessionEmailKey] = normalizedEmail
            preferences[sessionProviderKey] = "email"
            preferences[sessionNeedsSyncKey] = true
        }
        return result
    }

    suspend fun logout() {
        context.accountDataStore.edit { preferences ->
            preferences[sessionStateKey] = "logged_out"
            preferences.remove(sessionEmailKey)
            preferences.remove(sessionProviderKey)
            preferences[sessionNeedsSyncKey] = false
        }
    }

    suspend fun loginWithGoogle(email: String): AccountActionResult {
        return loginWithSocialProvider(
            suggestedEmail = email,
            provider = "google"
        )
    }

    suspend fun loginWithEmail(email: String): AccountActionResult {
        return loginWithSocialProvider(
            suggestedEmail = email,
            provider = "email"
        )
    }


    suspend fun markInitialSyncCompleted() {
        context.accountDataStore.edit { preferences ->
            preferences[sessionNeedsSyncKey] = false
        }
    }

    suspend fun updateLastSyncRemoteHash(hash: String?) {
        context.accountDataStore.edit { preferences ->
            if (hash == null) {
                preferences.remove(lastSyncRemoteHashKey)
            } else {
                preferences[lastSyncRemoteHashKey] = hash
            }
        }
    }

    private suspend fun loginWithSocialProvider(
        suggestedEmail: String,
        provider: String
    ): AccountActionResult {
        val normalizedEmail = suggestedEmail.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            return AccountActionResult.CREDENCIALES_INVALIDAS
        }

        context.accountDataStore.edit { preferences ->
            preferences[sessionStateKey] = "logged_in"
            preferences[sessionEmailKey] = normalizedEmail
            preferences[sessionProviderKey] = provider
            preferences[sessionNeedsSyncKey] = true
        }
        return AccountActionResult.SUCCESS
    }

    private fun decodeAccounts(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw
            .split("\n")
            .mapNotNull { line ->
                val separatorIndex = line.indexOf("::")
                if (separatorIndex <= 0 || separatorIndex >= line.length - 2) {
                    null
                } else {
                    val email = line.substring(0, separatorIndex)
                    val password = line.substring(separatorIndex + 2)
                    email to password
                }
            }
            .toMap()
    }

    private fun encodeAccounts(accounts: Map<String, String>): String {
        return accounts.entries.joinToString(separator = "\n") { (email, password) ->
            "$email::$password"
        }
    }
}
