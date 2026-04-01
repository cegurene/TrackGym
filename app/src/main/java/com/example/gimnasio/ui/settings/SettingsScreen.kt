package com.example.gimnasio.ui.settings

import android.app.Activity
import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gimnasio.data.auth.AppleSocialAuthProvider
import com.example.gimnasio.data.auth.FirebaseEmailAuthProvider
import com.example.gimnasio.data.auth.GoogleSocialAuthProvider
import com.example.gimnasio.data.auth.SocialAuthAvailability
import com.example.gimnasio.data.auth.SocialAuthResult
import com.example.gimnasio.data.preferences.AccountActionResult
import com.example.gimnasio.data.preferences.AccountPreferencesRepository
import com.example.gimnasio.data.preferences.AccountSession
import com.example.gimnasio.data.preferences.AuthProvider
import com.example.gimnasio.ui.auth.SocialAuthButtonsRow
import com.example.gimnasio.ui.theme.ThemeMode
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val accountPreferencesRepository = remember { AccountPreferencesRepository(context) }
    val emailAuthProvider = remember { FirebaseEmailAuthProvider() }
    val googleSocialAuthProvider = remember { GoogleSocialAuthProvider() }
    val appleSocialAuthProvider = remember { AppleSocialAuthProvider() }
    val accountSession by accountPreferencesRepository.accountSessionFlow.collectAsState(initial = AccountSession.LoggedOut)
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var accountError by rememberSaveable { mutableStateOf<String?>(null) }
    var accountInfo by rememberSaveable { mutableStateOf<String?>(null) }
    var socialLoadingProvider by rememberSaveable { mutableStateOf<String?>(null) }

    val trimmedEmail = email.trim()
    val isEmailValid = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val isPasswordValid = password.isNotBlank()
    val emailAvailability = SocialAuthAvailability.email()
    val canSubmitCredentials = isEmailValid && isPasswordValid && emailAvailability.isAvailable
    val googleAvailability = SocialAuthAvailability.google(hasActivity = activity != null)
    val appleAvailability = SocialAuthAvailability.apple(hasActivity = activity != null)

    val accountStatusText = when (val session = accountSession) {
        AccountSession.LoggedOut -> "No has iniciado sesión"
        is AccountSession.LoggedIn -> {
            val providerLabel = when (session.provider) {
                AuthProvider.EMAIL -> "Email"
                AuthProvider.GOOGLE -> "Google"
                AuthProvider.APPLE -> "Apple ID"
            }
            "Sesión activa: ${session.email} ($providerLabel)"
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                if (accountSession is AccountSession.LoggedIn) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sesión iniciada",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = accountStatusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = if ((accountSession as AccountSession.LoggedIn).needsInitialSync) {
                                    "Tienes sincronización pendiente. Los cambios se guardarán online cuando termine el primer sincronizado."
                                } else {
                                    "Tu cuenta ya está conectada y los cambios se sincronizan entre dispositivos."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        if (FirebaseApp.getApps(context).isNotEmpty()) {
                                            runCatching { FirebaseAuth.getInstance().signOut() }
                                        }
                                        accountPreferencesRepository.logout()
                                        accountError = null
                                        accountInfo = "Sesión cerrada"
                                    }
                                }
                            ) {
                                Text("Cerrar sesión")
                            }

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
                } else {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Inicia sesión o crea una cuenta",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Inicia sesión para sincronizar tus entrenamientos, rutinas y ejercicios entre dispositivos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

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
                                googleEnabled = socialAuthEnabled(googleAvailability.isAvailable, socialLoadingProvider),
                                appleEnabled = socialAuthEnabled(appleAvailability.isAvailable, socialLoadingProvider),
                                googleLoading = socialLoadingProvider == "google",
                                appleLoading = socialLoadingProvider == "apple",
                                onGoogleClick = {
                                    if (activity == null) {
                                        accountInfo = null
                                        accountError = "No se pudo abrir el flujo de Google en este contexto"
                                    } else {
                                        socialLoadingProvider = "google"
                                        scope.launch {
                                            when (val socialResult = googleSocialAuthProvider.signIn(activity)) {
                                                is SocialAuthResult.Success -> {
                                                    val result = accountPreferencesRepository.loginWithGoogle(socialResult.user.email)
                                                    if (result == AccountActionResult.SUCCESS) {
                                                        accountError = null
                                                        accountInfo = "Sesión iniciada con Google"
                                                        password = ""
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
                                            socialLoadingProvider = null
                                        }
                                    }
                                },
                                onAppleClick = {
                                    if (activity == null) {
                                        accountInfo = null
                                        accountError = "No se pudo abrir el flujo de Apple ID en este contexto"
                                    } else {
                                        socialLoadingProvider = "apple"
                                        scope.launch {
                                            when (val socialResult = appleSocialAuthProvider.signIn(activity)) {
                                                is SocialAuthResult.Success -> {
                                                    val result = accountPreferencesRepository.loginWithApple(socialResult.user.email)
                                                    if (result == AccountActionResult.SUCCESS) {
                                                        accountError = null
                                                        accountInfo = "Sesión iniciada con Apple ID"
                                                        password = ""
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
                                            socialLoadingProvider = null
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
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "Apariencia",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(ThemeMode.entries.toList()) { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "Sistema"
                    ThemeMode.LIGHT -> "Claro"
                    ThemeMode.DARK -> "Oscuro"
                }

                Card(
                    onClick = { onThemeModeSelected(mode) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "Usa el tema del dispositivo"
                                    ThemeMode.LIGHT -> "Tema claro siempre"
                                    ThemeMode.DARK -> "Tema oscuro siempre"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        RadioButton(
                            selected = mode == selectedThemeMode,
                            onClick = { onThemeModeSelected(mode) }
                        )
                    }
                }
            }
        }
    }
}

private fun socialAuthEnabled(isAvailable: Boolean, loadingProvider: String?): Boolean {
    return loadingProvider == null && isAvailable
}
