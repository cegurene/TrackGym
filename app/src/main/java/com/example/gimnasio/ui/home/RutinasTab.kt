package com.example.gimnasio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gimnasio.ui.rutinas.RutinasScreen

@Composable
fun RutinasTab(
    onRutinaClick: (Long) -> Unit
) {
    // Reutilizamos tu pantalla real de rutinas
    RutinasScreen(
        onRutinaClick = onRutinaClick,
        onVerEjerciciosClick = {}
    )
}
