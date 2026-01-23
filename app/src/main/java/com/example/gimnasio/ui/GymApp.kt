package com.example.gimnasio.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.gimnasio.ui.rutinas.NuevaRutinaDialog
import com.example.gimnasio.ui.rutinas.RutinasScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymApp() {

    val rutinas = remember { mutableStateListOf<String>() }
    var mostrarDialogo = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gimnasio") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogo.value = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir rutina"
                )
            }
        }
    ) { innerPadding ->

        RutinasScreen(
            rutinas = rutinas,
            modifier = Modifier.padding(innerPadding)
        )

        if (mostrarDialogo.value) {
            NuevaRutinaDialog(
                onGuardar = { nombre ->
                    rutinas.add(nombre)
                    mostrarDialogo.value = false
                },
                onCancelar = {
                    mostrarDialogo.value = false
                }
            )
        }
    }
}

