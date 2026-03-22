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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.TextFieldValue

@Composable
private fun SerieInputField(
    initialValue: String,
    onValueCommit: (Int) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialValue))
    }

    LaunchedEffect(initialValue) {
        if (textFieldValue.text != initialValue) {
            textFieldValue = TextFieldValue(initialValue)
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue: TextFieldValue ->

            if (newValue.text.isEmpty() || newValue.text.all { it.isDigit() }) {
                textFieldValue = newValue

                val value = newValue.text.toIntOrNull() ?: 0
                onValueCommit(value)
            }
        },
        label = { Text(label) },
        modifier = modifier.width(90.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = enabled
    )
}

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

    val entrenamiento by viewModel.entrenamiento.collectAsState(initial = null)

    var tiempoActual by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    val duracionMs = entrenamiento?.entrenamiento?.fechaInicio?.let {
        tiempoActual - it
    } ?: 0L

    fun formatearDuracion(ms: Long): String {

        val segundos = ms / 1000
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val seg = segundos % 60

        return if (horas > 0) {
            "%02d:%02d:%02d".format(horas, minutos, seg)
        } else {
            "%02d:%02d".format(minutos, seg)
        }
    }

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
                title = {
                    Column {
                        Text("Entrenamiento en curso")

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏱ ")
                            Text(
                                text = formatearDuracion(duracionMs),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
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
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp), // padding general
                verticalArrangement = Arrangement.spacedBy(15.dp) // separación entre cards
            ) {
                items(ejercicios) { ejercicioConSeries ->

                    val esCardio = ejercicioConSeries.ejercicio.musculos.contains(Musculo.CARDIO)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp) // padding interno de la card
                        ) {

                            // 🔹 Cabecera ejercicio
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
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

                            // 🔹 SERIES
                            ejercicioConSeries.series.forEachIndexed { index, serie ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 0.dp, vertical = 4.dp)
                                ) {

                                    Text(
                                        text = "Serie ${index + 1}",
                                        modifier = Modifier.width(80.dp)
                                    )

                                    if (!esCardio) {
                                        SerieInputField(
                                            initialValue = if ((serie.peso ?: 0f) == 0f) "" else (serie.peso?.toInt() ?: 0).toString(),
                                            onValueCommit = { peso ->
                                                viewModel.actualizarPesoSerie(serie.id, peso.toFloat())
                                            },
                                            label = "Kg",
                                            enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        SerieInputField(
                                            initialValue = if ((serie.repeticiones ?: 0) == 0) "" else serie.repeticiones.toString(),
                                            onValueCommit = { reps ->
                                                viewModel.actualizarRepsSerie(serie.id, reps)
                                            },
                                            label = "Reps",
                                            enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
                                        )
                                    } else {
                                        SerieInputField(
                                            initialValue = if ((serie.tiempo ?: 0) == 0) "" else serie.tiempo.toString(),
                                            onValueCommit = { tiempo ->
                                                viewModel.actualizarTiempoSerie(serie.id, tiempo)
                                            },
                                            label = "Min",
                                            enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        SerieInputField(
                                            initialValue = if ((serie.intensidad ?: 0) == 0) "" else serie.intensidad.toString(),
                                            onValueCommit = { intensidad ->
                                                viewModel.actualizarIntensidadSerie(serie.id, intensidad)
                                            },
                                            label = "Intens",
                                            enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
                                        )
                                    }

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

                            // 🔹 BOTÓN AÑADIR SERIE
                            Button(
                                onClick = {
                                    viewModel.añadirSerie(
                                        ejercicioConSeries.entrenamientoEjercicio.id,
                                        esCardio
                                    )
                                },
                                modifier = Modifier
                                    .padding(top = 8.dp),
                                enabled = !ejercicioConSeries.entrenamientoEjercicio.completado
                            ) {
                                Text("+ Añadir serie")
                            }
                        }
                    }
                }

                // 🔹 BOTÓN AÑADIR EJERCICIO
                item {
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    .padding(horizontal = 16.dp),
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
                            validationMessage = "Hay series con 0 en alguno de sus campos."
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