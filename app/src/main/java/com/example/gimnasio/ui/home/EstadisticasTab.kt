package com.example.gimnasio.ui.home

import androidx.compose.runtime.Composable
import com.example.gimnasio.ui.estadisticas.EstadisticasScreen


@Composable
fun EstadisticasTab(
    onEstadisticasClick: () -> Unit
) {
    EstadisticasScreen(
        onEstadisticasClick = onEstadisticasClick
    )
}