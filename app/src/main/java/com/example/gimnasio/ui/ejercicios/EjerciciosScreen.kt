package com.example.gimnasio.ui.ejercicios

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosScreen(
    onBack: () -> Unit,
    onEjercicioClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }

    val viewModel: EjercicioViewModel = viewModel(
        factory = EjercicioViewModelFactory(database)
    )

    val ejercicios by viewModel.ejercicios.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var nombreEjercicio by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ejercicios") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Text("+")
            }
        }
    ) { padding ->

        if (ejercicios.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay ejercicios todavía",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(ejercicios) { ejercicio ->
                    EjercicioItem(
                        nombre = ejercicio.nombre,
                        onClick = {
                            onEjercicioClick(ejercicio.id)
                        }
                    )
                }
            }
        }
    }

    // 🔹 Diálogo crear ejercicio
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuevo ejercicio") },
            text = {
                OutlinedTextField(
                    value = nombreEjercicio,
                    onValueChange = { nombreEjercicio = it },
                    label = { Text("Nombre del ejercicio") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.crearEjercicio(nombreEjercicio)
                        nombreEjercicio = ""
                        showDialog = false
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
