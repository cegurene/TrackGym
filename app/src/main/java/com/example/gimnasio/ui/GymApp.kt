package com.example.gimnasio.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.gimnasio.ui.navigation.GymNavHost

@Composable
fun GymApp() {

    // ✅ AQUÍ se crea el NavController (una sola vez)
    val navController = rememberNavController()

    // ✅ Y se pasa al NavHost
    GymNavHost(
        navController = navController,
        modifier = Modifier
    )
}
