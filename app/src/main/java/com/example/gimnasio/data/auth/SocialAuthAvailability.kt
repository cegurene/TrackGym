package com.example.gimnasio.data.auth

import com.example.gimnasio.BuildConfig

data class ProviderAvailability(
    val isAvailable: Boolean,
    val blockedReason: String? = null
)

object SocialAuthAvailability {
    fun email(): ProviderAvailability {
        if (!BuildConfig.HAS_GOOGLE_SERVICES_JSON) {
            return ProviderAvailability(
                isAvailable = false,
                blockedReason = "Falta app/google-services.json para inicializar Firebase."
            )
        }
        return ProviderAvailability(isAvailable = true)
    }

    fun google(hasActivity: Boolean): ProviderAvailability {
        if (!hasActivity) {
            return ProviderAvailability(
                isAvailable = false,
                blockedReason = "Google Sign-In solo esta disponible desde una pantalla activa."
            )
        }
        if (!BuildConfig.HAS_GOOGLE_SERVICES_JSON) {
            return ProviderAvailability(
                isAvailable = false,
                blockedReason = "Falta app/google-services.json para activar Google Sign-In."
            )
        }
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.trim().isBlank()) {
            return ProviderAvailability(
                isAvailable = false,
                blockedReason = "Configura GOOGLE_WEB_CLIENT_ID en gradle.properties."
            )
        }
        return ProviderAvailability(isAvailable = true)
    }
}
