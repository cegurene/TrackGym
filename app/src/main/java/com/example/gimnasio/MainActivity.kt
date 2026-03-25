package com.example.gimnasio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.gimnasio.data.preferences.ThemePreferencesRepository
import com.example.gimnasio.ui.GymApp
import com.example.gimnasio.ui.theme.GimnasioTheme
import com.example.gimnasio.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val themePreferencesRepository = remember { ThemePreferencesRepository(applicationContext) }
            val selectedThemeMode by themePreferencesRepository
                .themeModeFlow
                .collectAsState(initial = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()

            GimnasioTheme(
                themeMode = selectedThemeMode,
                dynamicColor = false
            ) {
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
