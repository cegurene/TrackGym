package com.example.gimnasio.ui.historico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HistoricoDetailSettingsSheet(
    advancedModificationsEnabled: Boolean,
    onRename: () -> Unit,
    onChangeRoutine: () -> Unit,
    onChangeDuration: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ajustes del entrenamiento",
            style = MaterialTheme.typography.titleMedium
        )

        HorizontalDivider()

        TextButton(
            onClick = onRename,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✏️ Cambiar nombre")
        }

        if (advancedModificationsEnabled) {
            TextButton(
                onClick = onChangeRoutine,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 Cambiar rutina")
            }

            TextButton(
                onClick = onChangeDuration,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⏱️ Cambiar duración")
            }
        }

        TextButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🗑️ Eliminar entrenamiento",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}