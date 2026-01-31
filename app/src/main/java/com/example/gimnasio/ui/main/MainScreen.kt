package com.example.gimnasio.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen(
    onRutinaClick: (Long) -> Unit
) {

    val context = LocalContext.current

    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(context)
    )

    val rutinas by viewModel.rutinas.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
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
