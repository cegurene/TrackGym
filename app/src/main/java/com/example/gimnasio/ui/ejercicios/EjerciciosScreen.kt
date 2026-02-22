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
import com.example.gimnasio.data.entity.Musculo

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
    var mostrarErrorMusculo by remember { mutableStateOf(false) }
    var mostrarErrorNombre by remember { mutableStateOf(false) }

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
        var musculoSeleccionado by remember { mutableStateOf<Musculo?>(null) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuevo ejercicio") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreEjercicio,
                        onValueChange = {
                            nombreEjercicio = it
                            mostrarErrorNombre = false
                        },
                        label = { Text("Nombre del ejercicio") },
                        singleLine = true
                    )

                    if (mostrarErrorNombre) {
                        Text(
                            text = "El nombre del ejercicio no puede estar vacío",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Músculos trabajados")

                    if (mostrarErrorMusculo) {
                        Text(
                            text = "Debes seleccionar al menos un músculo",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Musculo.values().forEach { musculo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = musculoSeleccionado == musculo,
                                onCheckedChange = { checked ->
                                    musculoSeleccionado = if (checked) musculo else null
                                    mostrarErrorMusculo = false
                                }
                            )
                            Text(musculo.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        var hayError = false

                        if (nombreEjercicio.isBlank()) {
                            mostrarErrorNombre = true
                            hayError = true
                        } else {
                            mostrarErrorNombre = false
                        }

                        if (musculoSeleccionado == null) {
                            mostrarErrorMusculo = true
                            hayError = true
                        } else {
                            mostrarErrorMusculo = false
                        }

                        if (!hayError) {
                            viewModel.crearEjercicio(
                                nombreEjercicio.trim(),
                                listOf(musculoSeleccionado!!)
                            )
                            nombreEjercicio = ""
                            musculoSeleccionado = null
                            showDialog = false
                        }
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
