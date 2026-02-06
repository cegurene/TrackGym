package com.example.gimnasio.ui.rutinas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
    onCrearRutinaClick: () -> Unit,
    onVerEjerciciosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(context))
    val rutinas by viewModel.rutinas.collectAsState()

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
            FloatingActionButton(onClick = onCrearRutinaClick) {
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
}
