package com.example.gimnasio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.gimnasio.data.preferences.AccountPreferencesRepository
import com.example.gimnasio.data.preferences.AccountSession
import com.example.gimnasio.data.preferences.OnboardingPreferencesRepository
import com.example.gimnasio.data.preferences.ThemePreferencesRepository
import com.example.gimnasio.data.sync.InitialCloudSyncService
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
                .collectAsState(initial = false)
            val initialCloudSyncService = remember(onboardingCompleted) {
                if (onboardingCompleted) {
                    InitialCloudSyncService(
                        context = applicationContext,
                        accountPreferencesRepository = accountPreferencesRepository
                    )
                } else {
                    null
                }
            }
            val scope = rememberCoroutineScope()

            LaunchedEffect(onboardingCompleted, accountSession, initialCloudSyncService) {
                if (onboardingCompleted) {
                    initialCloudSyncService?.syncIfNeeded(accountSession)
                }
            }

            LaunchedEffect(onboardingCompleted) {
                if (!onboardingCompleted) return@LaunchedEffect
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!onboardingCompleted) {
                        InitialAccessScreen()
                    } else {
                        GymApp(
                            selectedThemeMode = selectedThemeMode,
                            onThemeModeSelected = { themeMode ->
                                scope.launch {
                                    themePreferencesRepository.setThemeMode(themeMode)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
