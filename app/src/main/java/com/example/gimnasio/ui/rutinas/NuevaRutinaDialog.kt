package com.example.gimnasio.ui.rutinas

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun NuevaRutinaDialog(
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    val texto = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva rutina") },
        text = {
            OutlinedTextField(
                value = texto.value,
                onValueChange = { texto.value = it },
                label = { Text("Nombre de la rutina") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (texto.value.isNotBlank()) {
                        onGuardar(texto.value)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
