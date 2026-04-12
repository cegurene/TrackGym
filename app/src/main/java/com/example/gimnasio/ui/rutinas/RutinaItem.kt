package com.example.gimnasio.ui.rutinas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.gimnasio.data.model.RutinaConEjercicios
import com.example.gimnasio.ui.components.emoji

@Composable
fun RutinaItem(
    rutinaConEjercicios: RutinaConEjercicios,
    onClick: () -> Unit
) {
    val totalEjercicios = rutinaConEjercicios.ejercicios.size
    val iconosRutina = rutinaConEjercicios.ejercicios
        .flatMap { it.musculos }
        .map { it.emoji() }
        .distinct()
        .joinToString(" ")
        .ifBlank { "🗂️" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rutinaConEjercicios.rutina.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = iconosRutina,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$totalEjercicios ejercicios",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

