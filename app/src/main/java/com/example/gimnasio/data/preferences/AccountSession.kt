package com.example.gimnasio.data.preferences

enum class AuthProvider {
    EMAIL,
    GOOGLE
}

sealed interface AccountSession {
    data object LoggedOut : AccountSession
    data class LoggedIn(
        val email: String,
        val provider: AuthProvider = AuthProvider.EMAIL,
        val needsInitialSync: Boolean = false
    ) : AccountSession
}

enum class AccountActionResult {
    SUCCESS,
    EMAIL_YA_EXISTE,
    CREDENCIALES_INVALIDAS,
    OPERACION_CANCELADA,
    PROVEEDOR_NO_DISPONIBLE
}
