package com.example.gimnasio.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.gimnasio.ui.navigation.GymNavHost

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GymApp() {
    val navController = rememberNavController()

    // Surface principal con safeDrawingPadding para respetar el notch y la barra inferior
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        GymNavHost(
            navController = navController
        )
    }
}
