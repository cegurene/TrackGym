package com.example.gimnasio.ui.rutinas

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun CrearRutinaDialog(
    onCrear: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.text.isNotBlank()) {
                        onCrear(nombre.text.trim())
                    }
                }
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        },
        title = { Text("Nueva rutina") },
        text = {
            TextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la rutina") }
            )
        }
    )
}
