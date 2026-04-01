package com.example.gimnasio.data.auth

import android.app.Activity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.OAuthProvider

class AppleSocialAuthProvider : SocialAuthProvider {
    override suspend fun signIn(activity: Activity): SocialAuthResult {
        val availability = SocialAuthAvailability.apple(hasActivity = true)
        if (!availability.isAvailable) {
            return SocialAuthResult.Error(availability.blockedReason ?: "Apple ID no esta disponible.")
        }
        if (FirebaseApp.getApps(activity).isEmpty()) {
            return SocialAuthResult.Error("Firebase no esta inicializado. Revisa google-services.json.")
        }

        val firebaseAuth = FirebaseAuth.getInstance()
        return try {
            val provider = OAuthProvider.newBuilder("apple.com")
            provider.scopes = listOf("email", "name")

            val authResult = firebaseAuth.pendingAuthResult?.awaitResult()
                ?: firebaseAuth
                    .startActivityForSignInWithProvider(activity, provider.build())
                    .awaitResult()

            val user = authResult.user ?: firebaseAuth.currentUser
            val safeEmail = user?.email ?: "apple_user_${user?.uid ?: "anon"}@apple.local"
            SocialAuthResult.Success(
                SocialAuthUser(
                    email = safeEmail,
                    providerUserId = user?.uid
                )
            )
        } catch (e: FirebaseAuthException) {
            SocialAuthResult.Error(e.localizedMessage ?: "No se pudo iniciar sesion con Apple ID.")
        } catch (e: IllegalStateException) {
            SocialAuthResult.Error(e.localizedMessage ?: "Apple ID no esta configurado correctamente.")
        } catch (e: Exception) {
            SocialAuthResult.Error(e.localizedMessage ?: "No se pudo iniciar sesion con Apple ID.")
        }
    }
}
