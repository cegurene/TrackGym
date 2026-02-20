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

    val database = remember { GymDatabase.getDatabase(context) }
    val ejercicioDao = remember { database.ejercicioDao() }

    val ejerciciosDisponibles by ejercicioDao
        .getAll()
        .collectAsState(initial = emptyList())

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }

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

                    // 🔹 Cabecera ejercicio con checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        Checkbox(
                            checked = ejercicioConSeries.entrenamientoEjercicio.completado,
                            onCheckedChange = { checked ->
                                viewModel.marcarEjercicioCompletado(
                                    ejercicioConSeries.entrenamientoEjercicio.id,
                                    checked
                                )
                            }
                        )

                        Text(
                            text = ejercicioConSeries.ejercicio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            enabled = !ejercicioConSeries.entrenamientoEjercicio.completado,
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

                    // 🔹 Series
                    ejercicioConSeries.series.forEachIndexed { index, serie ->

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {

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
                                singleLine = true,
                                enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
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
                                singleLine = true,
                                enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
                            )

                            if (index == ejercicioConSeries.series.size - 1) {
                                IconButton(
                                    onClick = { viewModel.eliminarSerie(serie.id) },
                                    enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
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
                            .padding(start = 32.dp, bottom = 16.dp),
                        enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
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
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {

                        val haySinCompletar = ejercicios.any {
                            !it.entrenamientoEjercicio.completado
                        }

                        if (haySinCompletar) {
                            showErrorDialog = true
                            return@Button
                        }

                        val hayEjercicioSinSeries = ejercicios.any {
                            it.series.isEmpty()
                        }

                        if (hayEjercicioSinSeries) {
                            validationMessage = "Hay ejercicios sin ninguna serie registrada."
                            showValidationDialog = true
                            return@Button
                        }

                        val haySeriesInvalidas = ejercicios.any { ejercicio ->
                            ejercicio.series.any { serie ->
                                serie.peso == 0f || serie.repeticiones == 0
                            }
                        }

                        if (haySeriesInvalidas) {
                            validationMessage = "Hay series con 0 repeticiones o 0 peso."
                            showValidationDialog = true
                            return@Button
                        }

                        // 👇 Si todo está correcto mostramos confirmación
                        showFinishConfirmDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Finalizar")
                }
            }
        }
    }

    // 🔹 Dialogos
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
                TextButton(onClick = { showAddExerciseDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar entrenamiento") },
            text = { Text("¿Estás seguro? Se perderán los datos.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelarEntrenamiento { onBack() }
                }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("No") }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Ejercicios sin completar") },
            text = { Text("Debes completar o eliminar todos los ejercicios antes de finalizar.") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) { Text("Entendido") }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Entrenamiento completado") },
            text = { Text("¡Buen trabajo!") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false; onBack() }) { Text("Aceptar") }
            }
        )
    }

    if (showValidationDialog) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            title = { Text("Datos incompletos") },
            text = { Text(validationMessage) },
            confirmButton = {
                TextButton(onClick = { showValidationDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            title = { Text("Finalizar entrenamiento") },
            text = { Text("¿Estás seguro de que quieres finalizar el entrenamiento?") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirmDialog = false
                    viewModel.finalizarEntrenamiento {
                        showSuccessDialog = true
                    }
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFinishConfirmDialog = false
                }) {
                    Text("No")
                }
            }
        )
    }
}
