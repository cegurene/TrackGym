package com.example.gimnasio.ui.ejercicios

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.components.emoji

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EjercicioItem(
    nombre: String,
    musculos: List<Musculo>,
    onClick: () -> Unit
) {
    val iconosMusculo = musculos
        .toSet()
        .joinToString(" ") { it.emoji() }
        .ifBlank { "🏋️" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = iconosMusculo,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}


