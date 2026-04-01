package com.example.gimnasio.ui.onboarding

import android.app.Activity
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gimnasio.R
import com.example.gimnasio.data.auth.AppleSocialAuthProvider
import com.example.gimnasio.data.auth.FirebaseEmailAuthProvider
import com.example.gimnasio.data.auth.GoogleSocialAuthProvider
import com.example.gimnasio.data.auth.SocialAuthAvailability
import com.example.gimnasio.data.auth.SocialAuthResult
import com.example.gimnasio.data.preferences.AccountActionResult
import com.example.gimnasio.data.preferences.AccountPreferencesRepository
import com.example.gimnasio.data.preferences.OnboardingPreferencesRepository
import com.example.gimnasio.ui.auth.SocialAuthButtonsRow
import kotlinx.coroutines.launch

@Composable
fun InitialAccessScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val onboardingRepository = remember { OnboardingPreferencesRepository(context) }
    val accountPreferencesRepository = remember { AccountPreferencesRepository(context) }
    val emailAuthProvider = remember { FirebaseEmailAuthProvider() }
    val googleAuthProvider = remember { GoogleSocialAuthProvider() }
    val appleAuthProvider = remember { AppleSocialAuthProvider() }
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var accountError by rememberSaveable { mutableStateOf<String?>(null) }
    var accountInfo by rememberSaveable { mutableStateOf<String?>(null) }
    var loadingProvider by rememberSaveable { mutableStateOf<String?>(null) }

    val trimmedEmail = email.trim()
    val isEmailValid = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val isPasswordValid = password.isNotBlank()
    val canSubmitCredentials = isEmailValid && isPasswordValid && SocialAuthAvailability.email().isAvailable
    val googleAvailability = SocialAuthAvailability.google(hasActivity = activity != null)
    val appleAvailability = SocialAuthAvailability.apple(hasActivity = activity != null)

    suspend fun finishOnboarding(withSession: Boolean) {
        onboardingRepository.markOnboardingCompleted()
        if (!withSession) {
            accountPreferencesRepository.logout()
        }
    }

    fun applyAuthResult(result: AccountActionResult) {
        when (result) {
            AccountActionResult.SUCCESS -> {
                accountError = null
                accountInfo = "Sesión iniciada correctamente"
                password = ""
            }
            AccountActionResult.EMAIL_YA_EXISTE -> {
                accountInfo = null
                accountError = "Ya existe una cuenta con ese email"
            }
            AccountActionResult.CREDENCIALES_INVALIDAS -> {
                accountInfo = null
                accountError = "Credenciales inválidas"
            }
            AccountActionResult.OPERACION_CANCELADA -> {
                accountInfo = null
                accountError = "Operación cancelada"
            }
            AccountActionResult.PROVEEDOR_NO_DISPONIBLE -> {
                accountInfo = null
                accountError = "Proveedor no disponible"
            }
        }
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )
                    Text(
                        text = "Gimnasio",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Empieza con tu cuenta para sincronizar entrenamientos, rutinas y ejercicios.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Inicia sesión o crea una cuenta",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                accountError = null
                                accountInfo = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Email") },
                            isError = trimmedEmail.isNotEmpty() && !isEmailValid
                        )
                        if (trimmedEmail.isNotEmpty() && !isEmailValid) {
                            Text(
                                text = "Introduce un email válido",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                accountError = null
                                accountInfo = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null
                                )
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                enabled = canSubmitCredentials,
                                onClick = {
                                    scope.launch {
                                        when (val authResult = emailAuthProvider.signIn(email, password)) {
                                            is SocialAuthResult.Success -> {
                                                applyAuthResult(
                                                    accountPreferencesRepository.loginWithEmail(authResult.user.email)
                                                )
                                                finishOnboarding(withSession = true)
                                            }
                                            SocialAuthResult.Cancelled -> {
                                                accountInfo = null
                                                accountError = "Inicio con email cancelado"
                                            }
                                            is SocialAuthResult.ExternalFlowOpened -> {
                                                accountError = null
                                                accountInfo = authResult.message
                                            }
                                            is SocialAuthResult.Error -> {
                                                accountInfo = null
                                                accountError = authResult.message
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Iniciar sesión")
                            }

                            FilledTonalButton(
                                enabled = canSubmitCredentials,
                                onClick = {
                                    scope.launch {
                                        when (val authResult = emailAuthProvider.createAccount(email, password)) {
                                            is SocialAuthResult.Success -> {
                                                applyAuthResult(
                                                    accountPreferencesRepository.loginWithEmail(authResult.user.email)
                                                )
                                                finishOnboarding(withSession = true)
                                            }
                                            SocialAuthResult.Cancelled -> {
                                                accountInfo = null
                                                accountError = "Creación de cuenta cancelada"
                                            }
                                            is SocialAuthResult.ExternalFlowOpened -> {
                                                accountError = null
                                                accountInfo = authResult.message
                                            }
                                            is SocialAuthResult.Error -> {
                                                accountInfo = null
                                                accountError = authResult.message
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Crear cuenta")
                            }
                        }

                        SocialAuthButtonsRow(
                            googleEnabled = socialAuthEnabled(googleAvailability.isAvailable, loadingProvider),
                            appleEnabled = socialAuthEnabled(appleAvailability.isAvailable, loadingProvider),
                            googleLoading = loadingProvider == "google",
                            appleLoading = loadingProvider == "apple",
                            onGoogleClick = {
                                if (activity == null) {
                                    accountInfo = null
                                    accountError = "No se pudo abrir el flujo de Google en este contexto"
                                } else {
                                    loadingProvider = "google"
                                    scope.launch {
                                        when (val socialResult = googleAuthProvider.signIn(activity)) {
                                            is SocialAuthResult.Success -> {
                                                val result = accountPreferencesRepository.loginWithGoogle(socialResult.user.email)
                                                if (result == AccountActionResult.SUCCESS) {
                                                    accountError = null
                                                    accountInfo = "Sesión iniciada con Google"
                                                    password = ""
                                                    finishOnboarding(withSession = true)
                                                } else {
                                                    applyAuthResult(result)
                                                }
                                            }
                                            SocialAuthResult.Cancelled -> {
                                                accountInfo = null
                                                accountError = "Inicio con Google cancelado"
                                            }
                                            is SocialAuthResult.ExternalFlowOpened -> {
                                                accountError = null
                                                accountInfo = socialResult.message
                                            }
                                            is SocialAuthResult.Error -> {
                                                accountInfo = null
                                                accountError = socialResult.message
                                            }
                                        }
                                        loadingProvider = null
                                    }
                                }
                            },
                            onAppleClick = {
                                if (activity == null) {
                                    accountInfo = null
                                    accountError = "No se pudo abrir el flujo de Apple ID en este contexto"
                                } else {
                                    loadingProvider = "apple"
                                    scope.launch {
                                        when (val socialResult = appleAuthProvider.signIn(activity)) {
                                            is SocialAuthResult.Success -> {
                                                val result = accountPreferencesRepository.loginWithApple(socialResult.user.email)
                                                if (result == AccountActionResult.SUCCESS) {
                                                    accountError = null
                                                    accountInfo = "Sesión iniciada con Apple ID"
                                                    password = ""
                                                    finishOnboarding(withSession = true)
                                                } else {
                                                    applyAuthResult(result)
                                                }
                                            }
                                            SocialAuthResult.Cancelled -> {
                                                accountInfo = null
                                                accountError = "Inicio con Apple ID cancelado"
                                            }
                                            is SocialAuthResult.ExternalFlowOpened -> {
                                                accountError = null
                                                accountInfo = socialResult.message
                                            }
                                            is SocialAuthResult.Error -> {
                                                accountInfo = null
                                                accountError = socialResult.message
                                            }
                                        }
                                        loadingProvider = null
                                    }
                                }
                            }
                        )

                        accountError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        accountInfo?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            finishOnboarding(withSession = false)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No iniciar sesión")
                }
            }

            item {
                Text(
                    text = "Puedes usar la app sin iniciar sesión si eliges esta opción.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

private fun socialAuthEnabled(isAvailable: Boolean, loadingProvider: String?): Boolean {
    return loadingProvider == null && isAvailable
}
