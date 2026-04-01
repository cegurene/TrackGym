package com.example.gimnasio.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.gimnasio.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSocialAuthProvider : SocialAuthProvider {
    override suspend fun signIn(activity: Activity): SocialAuthResult {
        val availability = SocialAuthAvailability.google(hasActivity = true)
        if (!availability.isAvailable) {
            return SocialAuthResult.Error(availability.blockedReason ?: "Google Sign-In no esta disponible.")
        }

        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (FirebaseApp.getApps(activity).isEmpty()) {
            return SocialAuthResult.Error("Firebase no esta inicializado. Revisa google-services.json.")
        }

        return try {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialResult = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = credentialResult.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SocialAuthResult.Error("Google no devolvio una credencial valida.")
            }

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val authResult = FirebaseAuth
                .getInstance()
                .signInWithCredential(firebaseCredential)
                .awaitResult()
            val user = authResult.user ?: FirebaseAuth.getInstance().currentUser

            SocialAuthResult.Success(
                SocialAuthUser(
                    email = user?.email ?: googleCredential.id,
                    providerUserId = user?.uid
                )
            )
        } catch (_: GetCredentialCancellationException) {
            SocialAuthResult.Cancelled
        } catch (_: GoogleIdTokenParsingException) {
            SocialAuthResult.Error("No se pudo leer la respuesta de Google.")
        } catch (e: GetCredentialException) {
            SocialAuthResult.Error(e.localizedMessage ?: "No se pudo iniciar sesion con Google.")
        } catch (e: IllegalStateException) {
            SocialAuthResult.Error(e.localizedMessage ?: "Google Sign-In no esta configurado correctamente.")
        } catch (e: Exception) {
            SocialAuthResult.Error(e.localizedMessage ?: "No se pudo iniciar sesion con Google.")
        }
    }
}
