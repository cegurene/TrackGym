package com.example.gimnasio.ui.home

import androidx.compose.runtime.Composable
import com.example.gimnasio.ui.ejercicios.EjerciciosScreen

@Composable
fun EjerciciosTab(
    onEjercicioClick: (Long) -> Unit
) {
    EjerciciosScreen(
        onBack = {},
        onEjercicioClick = onEjercicioClick
    )
}
