package com.example.gimnasio.ui.rutinas

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasScreen(
    onRutinaClick: (Long) -> Unit,
    onVerEjerciciosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: RutinaViewModel = viewModel(
        factory = RutinaViewModelFactory(context.applicationContext as Application)
    )
    val rutinas by viewModel.rutinas.collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var nombreRutina by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Rutinas") }
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(rutinas) { rutinaConEjercicios ->
                RutinaItem(
                    rutinaConEjercicios = rutinaConEjercicios,
                    onClick = {
                        onRutinaClick(rutinaConEjercicios.rutina.id)
                    }
                )
            }
        }
    }

    // 🔹 Diálogo crear rutina
    if (showDialog) {
        var mostrarErrorNombre by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva rutina") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreRutina,
                        onValueChange = {
                            nombreRutina = it
                            mostrarErrorNombre = false
                        },
                        label = { Text("Nombre de la rutina") },
                        singleLine = true
                    )

                    if (mostrarErrorNombre) {
                        Text(
                            text = "El nombre de la rutina no puede estar vacío",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nombreRutina.isBlank()) {
                            mostrarErrorNombre = true
                        } else {
                            viewModel.insertar(nombreRutina.trim())
                            nombreRutina = ""
                            showDialog = false
                            mostrarErrorNombre = false
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
