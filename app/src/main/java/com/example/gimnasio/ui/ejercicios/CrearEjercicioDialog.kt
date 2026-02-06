package com.example.gimnasio.ui.ejercicios

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun CrearEjercicioDialog(
    onCrear: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo ejercicio") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del ejercicio") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCrear(nombre)
                }
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
