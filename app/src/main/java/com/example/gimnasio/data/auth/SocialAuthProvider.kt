package com.example.gimnasio.data.auth

import android.app.Activity

enum class SocialAuthProviderType {
    GOOGLE,
    APPLE
}

data class SocialAuthUser(
    val email: String,
    val providerUserId: String? = null
)

sealed interface SocialAuthResult {
    data class Success(val user: SocialAuthUser) : SocialAuthResult
    data class ExternalFlowOpened(val message: String) : SocialAuthResult
    data class Error(val message: String) : SocialAuthResult
    data object Cancelled : SocialAuthResult
}

interface SocialAuthProvider {
    suspend fun signIn(activity: Activity): SocialAuthResult
}
