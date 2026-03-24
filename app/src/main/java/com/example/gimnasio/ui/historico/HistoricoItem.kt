package com.example.gimnasio.ui.historico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.gimnasio.data.model.EntrenamientoConRutina


@Composable
fun HistoricoItem(
    item: EntrenamientoConRutina,
    onClick: () -> Unit,
    numero: Int
) {

    val inicioMillis = item.entrenamiento.fechaInicio
    val finMillis = item.entrenamiento.fechaFin

    val duracionMin = if (finMillis != null) {
        ((finMillis - inicioMillis) / 1000 / 60)
    } else 0

    val fecha = android.text.format.DateFormat
        .format("dd/MM/yyyy", inicioMillis)
        .toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.entrenamiento.nombre,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = numero.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Basado en: ${item.rutina.nombre}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "📅 Fecha: $fecha",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "⏱️ Duración: $duracionMin min",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
