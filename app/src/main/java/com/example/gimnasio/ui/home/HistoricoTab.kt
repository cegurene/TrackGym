package com.example.gimnasio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.gimnasio.ui.historico.HistoricoScreen


@Composable
fun HistoricoTab(
    onEntrenamientoClick: (Long) -> Unit = {}
) {
    HistoricoScreen(
        onEntrenamientoClick = onEntrenamientoClick
    )
}
