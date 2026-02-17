package com.example.gimnasio.ui.home

import androidx.compose.runtime.*
import com.example.gimnasio.ui.rutinas.RutinasScreen

@Composable
fun RutinasTab(
    onRutinaClick: (Long) -> Unit
) {
    RutinasScreen(
        onRutinaClick = onRutinaClick,
        onVerEjerciciosClick = {}
    )
}
