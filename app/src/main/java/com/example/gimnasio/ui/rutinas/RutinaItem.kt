package com.example.gimnasio.ui.rutinas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gimnasio.data.model.RutinaConEjercicios

@Composable
fun RutinaItem(
    rutinaConEjercicios: RutinaConEjercicios,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = rutinaConEjercicios.rutina.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${rutinaConEjercicios.ejercicios.size} ejercicios",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

