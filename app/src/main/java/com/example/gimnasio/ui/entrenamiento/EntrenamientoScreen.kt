package com.example.gimnasio.ui.entrenamiento

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.ui.rutinas.RutinaViewModel
import com.example.gimnasio.ui.rutinas.RutinaViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientoScreen(
    entrenamientoId: Long,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val viewModel: EntrenamientoViewModel = viewModel(
        factory = EntrenamientoViewModelFactory(
            context.applicationContext as Application,
            entrenamientoId
        )
    )

    val ejercicios by viewModel
        .ejerciciosDelEntrenamiento
        .collectAsState(initial = emptyList())

    //val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }
    val ejercicioDao = remember { database.ejercicioDao() }

    val ejerciciosDisponibles by ejercicioDao
        .getAll()
        .collectAsState(initial = emptyList())

    var showAddExerciseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamiento en curso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                items(ejercicios) { ejercicioConSeries ->

                    // 🔹 Cabecera ejercicio
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Text(
                            text = ejercicioConSeries.ejercicio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                viewModel.eliminarEjercicio(
                                    ejercicioConSeries.entrenamientoEjercicio.id
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar ejercicio"
                            )
                        }
                    }

                    val totalSeries = ejercicioConSeries.series.size

                    ejercicioConSeries.series.forEachIndexed { index, serie ->

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 16.dp, bottom = 8.dp)
                                .fillMaxWidth()
                        ) {

                            Checkbox(
                                checked = serie.completada,
                                onCheckedChange = { checked ->
                                    viewModel.marcarSerieCompletada(
                                        serie.id,
                                        checked
                                    )
                                }
                            )

                            Text(
                                text = "Serie ${index + 1}",
                                modifier = Modifier.width(80.dp)
                            )

                            OutlinedTextField(
                                value = serie.peso.toString(),
                                onValueChange = {
                                    val peso = it.toFloatOrNull() ?: 0f
                                    viewModel.actualizarPesoSerie(serie.id, peso)
                                },
                                label = { Text("Kg") },
                                modifier = Modifier.width(90.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = serie.repeticiones.toString(),
                                onValueChange = {
                                    val reps = it.toIntOrNull() ?: 0
                                    viewModel.actualizarRepsSerie(serie.id, reps)
                                },
                                label = { Text("Reps") },
                                modifier = Modifier.width(90.dp),
                                singleLine = true
                            )

                            if (index == totalSeries - 1) {
                                IconButton(
                                    onClick = {
                                        viewModel.eliminarSerie(serie.id)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar serie"
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.añadirSerie(
                                ejercicioConSeries.entrenamientoEjercicio.id,
                                0f,
                                0
                            )
                        },
                        modifier = Modifier
                            .padding(start = 32.dp, bottom = 16.dp)
                    ) {
                        Text("+ Añadir serie")
                    }
                }

                item {
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("+ Añadir ejercicio")
                    }
                }
            }

            // 🔹 BOTONES INFERIORES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        viewModel.cancelarEntrenamiento {
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        viewModel.finalizarEntrenamiento {
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Finalizar")
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Añadir ejercicio") },
            text = {
                LazyColumn {
                    items(ejerciciosDisponibles) { ejercicio ->
                        TextButton(
                            onClick = {
                                viewModel.añadirEjercicioAlEntrenamiento(ejercicio.id)
                                showAddExerciseDialog = false
                            }
                        ) {
                            Text(ejercicio.nombre)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showAddExerciseDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

}
