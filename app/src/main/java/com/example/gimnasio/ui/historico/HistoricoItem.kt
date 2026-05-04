package com.example.gimnasio.ui.historico

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gimnasio.data.model.EntrenamientoConRutinaYEjercicios
import com.example.gimnasio.ui.components.emojiSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun HistoricoItem(
    item: EntrenamientoConRutinaYEjercicios,
    onClick: () -> Unit,
    numero: Int
) {

    val inicioMillis = item.entrenamiento.fechaInicio
    val finMillis = item.entrenamiento.fechaFin

    val duracionMin = if (finMillis != null) {
        ((finMillis - inicioMillis) / 1000 / 60)
    } else 0

    val duracionTexto = if (duracionMin >= 60) {
        val h = duracionMin / 60
        val m = duracionMin % 60
        if (m > 0) "${h} h ${m} min" else "${h} h"
    } else {
        "$duracionMin min"
    }

    val localeEs = Locale("es", "ES")
    val diaSemana = SimpleDateFormat("EEEE", localeEs)
        .format(Date(inicioMillis))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }
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
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.entrenamiento.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "#$numero",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val ejerciciosEmoji = item.ejercicios
                .map { it.ejercicio.musculos }
                .flatten()
                .emojiSummary()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = ejerciciosEmoji,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "📅 Fecha: $diaSemana $fecha",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "⏱️ Duración: $duracionTexto",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
