package com.example.gimnasio.ui.rutinas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.ui.main.MainViewModel
import com.example.gimnasio.ui.main.MainViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasScreen(
    onRutinaClick: (Long) -> Unit,
    onVerEjerciciosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context)
    )
    val rutinas by viewModel.rutinas.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var nombreRutina by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Rutinas") },
                actions = {
                    TextButton(onClick = onVerEjerciciosClick) {
                        Text("Ejercicios")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
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
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva rutina") },
            text = {
                OutlinedTextField(
                    value = nombreRutina,
                    onValueChange = { nombreRutina = it },
                    label = { Text("Nombre de la rutina") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nombreRutina.isNotBlank()) {
                            viewModel.crearRutina(nombreRutina)
                        }
                        nombreRutina = ""
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
