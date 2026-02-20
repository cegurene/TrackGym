package com.example.gimnasio.ui.entrenamiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EntrenamientoSettingsSheet(
    onRename: () -> Unit
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

        Divider()

        TextButton(
            onClick = onRename,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✏️ Cambiar nombre")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}