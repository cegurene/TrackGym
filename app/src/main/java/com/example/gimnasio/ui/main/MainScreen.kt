package com.example.gimnasio.ui.main

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
import com.example.gimnasio.data.model.RutinaConEjercicios
import com.example.gimnasio.ui.rutinas.RutinaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onIrAEjercicios: () -> Unit,
    onRutinaClick: (Long) -> Unit
) {
    val context = LocalContext.current

    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context)
    )

    val rutinas by viewModel.rutinas.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutinas") },
                actions = {
                    TextButton(onClick = onIrAEjercicios) {
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

        if (rutinas.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay rutinas creadas")
            }
        } else {
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
    }

    if (showDialog) {
        CrearRutinaDialog(
            onCrear = {
                viewModel.crearRutina(it)
                showDialog = false
            },
            onCancelar = { showDialog = false }
        )
    }
}
