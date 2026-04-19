package com.example.gimnasio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.gimnasio.data.preferences.AccountPreferencesRepository
import com.example.gimnasio.data.preferences.AccountSession
import com.example.gimnasio.data.preferences.OnboardingPreferencesRepository
import com.example.gimnasio.data.preferences.ThemePreferencesRepository
import com.example.gimnasio.data.sync.InitialCloudSyncService
import com.example.gimnasio.ui.components.DismissKeyboardOnTap
import com.example.gimnasio.ui.GymApp
import com.example.gimnasio.ui.onboarding.InitialAccessScreen
import com.example.gimnasio.ui.theme.GimnasioTheme
import com.example.gimnasio.ui.theme.ThemeMode
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val themePreferencesRepository = remember { ThemePreferencesRepository(applicationContext) }
            val accountPreferencesRepository = remember { AccountPreferencesRepository(applicationContext) }
            val onboardingPreferencesRepository = remember { OnboardingPreferencesRepository(applicationContext) }
            val selectedThemeMode by themePreferencesRepository
                .themeModeFlow
                .collectAsState(initial = ThemeMode.SYSTEM)
            val accountSession by accountPreferencesRepository
                .accountSessionFlow
                .collectAsState(initial = AccountSession.LoggedOut)
            val onboardingCompleted by onboardingPreferencesRepository
                .onboardingCompletedFlow
                .collectAsState(initial = null as Boolean?)
            val isOnboardingCompleted = onboardingCompleted == true
            val initialCloudSyncService = remember(isOnboardingCompleted) {
                if (isOnboardingCompleted) {
                    InitialCloudSyncService(
                        context = applicationContext,
                        accountPreferencesRepository = accountPreferencesRepository
                    )
                } else {
                    null
                }
            }
            val scope = rememberCoroutineScope()

            LaunchedEffect(isOnboardingCompleted, accountSession, initialCloudSyncService) {
                if (isOnboardingCompleted) {
                    initialCloudSyncService?.syncIfNeeded(accountSession)
                }
            }

            LaunchedEffect(isOnboardingCompleted) {
                if (!isOnboardingCompleted) return@LaunchedEffect
                if (FirebaseApp.getApps(applicationContext).isEmpty()) return@LaunchedEffect

                val firebaseUser = runCatching { FirebaseAuth.getInstance().currentUser }
                    .getOrNull()
                    ?: return@LaunchedEffect
                if (accountSession is AccountSession.LoggedIn) return@LaunchedEffect

                val providerIds = firebaseUser.providerData.map { it.providerId }
                when {
                    providerIds.contains("google.com") -> {
                        accountPreferencesRepository.loginWithGoogle(firebaseUser.email ?: "")
                    }
                    else -> {
                        accountPreferencesRepository.loginWithEmail(firebaseUser.email ?: "")
                    }
                }
            }

            GimnasioTheme(
                themeMode = selectedThemeMode,
                dynamicColor = false
            ) {
                val view = LocalView.current
                val isDarkTheme = when (selectedThemeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                val barColor = androidx.compose.material3.MaterialTheme.colorScheme.background.toArgb()

                SideEffect {
                    window.statusBarColor = barColor
                    window.navigationBarColor = barColor

                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !isDarkTheme
                    insetsController.isAppearanceLightNavigationBars = !isDarkTheme

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }

                DismissKeyboardOnTap {
                    when {
                        onboardingCompleted == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        isOnboardingCompleted -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                GymApp(
                                    selectedThemeMode = selectedThemeMode,
                                    onThemeModeSelected = { themeMode ->
                                        scope.launch {
                                            themePreferencesRepository.setThemeMode(themeMode)
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Se requiere Android 8.0 o superior")
                                }
                            }
                        }
                        else -> {
                            InitialAccessScreen()
                        }
                    }
                }
            }
        }
    }
}
