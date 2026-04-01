package com.example.gimnasio.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class FirebaseEmailAuthProvider {
    suspend fun signIn(email: String, password: String): SocialAuthResult {
        return authenticate(email = email, password = password, createAccount = false)
    }

    suspend fun createAccount(email: String, password: String): SocialAuthResult {
        return authenticate(email = email, password = password, createAccount = true)
    }

    private suspend fun authenticate(
        email: String,
        password: String,
        createAccount: Boolean
    ): SocialAuthResult {
        val firebaseAuth = runCatching { FirebaseAuth.getInstance() }
            .getOrNull()
            ?: return SocialAuthResult.Error(
                "Firebase no esta inicializado. Revisa google-services.json."
            )

        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            return SocialAuthResult.Error("Introduce email y contrasena validos.")
        }

        return try {
            val authResult = if (createAccount) {
                firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password).awaitResult()
            } else {
                firebaseAuth.signInWithEmailAndPassword(normalizedEmail, password).awaitResult()
            }
            val user = authResult.user ?: firebaseAuth.currentUser
            SocialAuthResult.Success(
                SocialAuthUser(
                    email = user?.email ?: normalizedEmail,
                    providerUserId = user?.uid
                )
            )
        } catch (e: FirebaseAuthException) {
            SocialAuthResult.Error(e.localizedMessage ?: "No se pudo autenticar con email.")
        } catch (e: Exception) {
            SocialAuthResult.Error(e.localizedMessage ?: "No se pudo autenticar con email.")
        }
    }
}

