package com.example.gimnasio.ui.entrenamiento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import com.example.gimnasio.ui.components.EjercicioSelectionCard
import com.example.gimnasio.ui.components.displayLabel
import com.example.gimnasio.ui.components.formatUiNumber
import com.example.gimnasio.ui.components.emojiSummary
import com.example.gimnasio.ui.components.imageRes
import java.math.RoundingMode

private fun parseDecimalInput(text: String): Float =
    text.replace(',', '.')
        .trimEnd('.')
        .toBigDecimalOrNull()
        ?.setScale(2, RoundingMode.HALF_UP)
        ?.toFloat()
        ?: 0f

private fun parseIntInput(text: String): Int = text.toIntOrNull() ?: 0

@Composable
private fun SerieInputField(
    initialValue: String,
    onValueCommit: (String) -> Unit,
    label: String,
    enabled: Boolean,
    allowDecimal: Boolean = false,
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

            val isValid = if (allowDecimal) {
                newValue.text.isEmpty() ||
                    newValue.text.matches(Regex("^\\d*(?:[.,]\\d*)?$"))
            } else {
                newValue.text.isEmpty() || newValue.text.all { it.isDigit() }
            }

            if (isValid) {
                val normalizedText = if (allowDecimal) newValue.text.replace(',', '.') else newValue.text
                textFieldValue = newValue.copy(text = normalizedText)
                onValueCommit(normalizedText)
            }
        },
        label = { Text(label) },
        modifier = modifier.width(90.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        enabled = enabled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientoScreen(
    entrenamientoId: Long,
    onBack: () -> Unit,
    onNavigateToEjercicio: (Long) -> Unit
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
    var erroresValidacion by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tiempoActual = System.currentTimeMillis()
        }
    }

    LaunchedEffect(viewModel.validationErrorFlow) {
        viewModel.validationErrorFlow.collect { (ejercicioId, mensaje) ->
            erroresValidacion = erroresValidacion.toMutableMap().apply {
                put(ejercicioId, mensaje)
            }
            delay(2000)
            erroresValidacion = erroresValidacion.toMutableMap().apply {
                remove(ejercicioId)
            }
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
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }

    val ejerciciosYaAgregados = ejercicios.map { it.entrenamientoEjercicio.ejercicioId }.toSet()
    val ejerciciosNoAgregados = ejerciciosDisponibles.filter { ejercicio ->
        !ejerciciosYaAgregados.contains(ejercicio.id)
    }

    var musculoFiltro by remember { mutableStateOf<Musculo?>(null) }
    val ejerciciosNoAgregadosFiltrados = remember(ejerciciosNoAgregados, musculoFiltro) {
        ejerciciosNoAgregados.filter { ejercicio ->
            musculoFiltro == null || ejercicio.musculos.contains(musculoFiltro)
        }
    }

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
                itemsIndexed(ejercicios) { index, ejercicioConSeries ->

                    val esCardio = ejercicioConSeries.ejercicio.musculos.contains(Musculo.CARDIO)
                    val ejercicioCompletado =
                        ejercicioConSeries.series.isNotEmpty() &&
                            ejercicioConSeries.series.all { it.completada }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            val musculosEmoji = ejercicioConSeries.ejercicio.musculos.emojiSummary()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = ejercicioCompletado,
                                    onCheckedChange = null
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = ejercicioConSeries.ejercicio.nombre,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onNavigateToEjercicio(ejercicioConSeries.ejercicio.id)
                                        },
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = musculosEmoji,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            viewModel.moverEjercicio(
                                                ejercicioConSeries,
                                                ejercicios[index - 1]
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Subir ejercicio"
                                        )
                                    }

                                    IconButton(
                                        enabled = index < ejercicios.lastIndex,
                                        onClick = {
                                            viewModel.moverEjercicio(
                                                ejercicioConSeries,
                                                ejercicios[index + 1]
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Bajar ejercicio"
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    enabled = !ejercicioCompletado,
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
                                                    initialValue = if ((serie.peso ?: 0f) == 0f) "" else serie.peso!!.formatUiNumber(),
                                                    onValueCommit = { peso ->
                                                        viewModel.actualizarPesoSerie(serie.id, parseDecimalInput(peso))
                                            },
                                            label = "Kg",
                                                    enabled = !serie.completada,
                                                    allowDecimal = true
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        SerieInputField(
                                            initialValue = if ((serie.repeticiones ?: 0) == 0) "" else serie.repeticiones.toString(),
                                            onValueCommit = { reps ->
                                                        viewModel.actualizarRepsSerie(serie.id, parseIntInput(reps))
                                            },
                                            label = "Reps",
                                            enabled = !serie.completada
                                        )
                                    } else {
                                        SerieInputField(
                                                    initialValue = if ((serie.tiempo ?: 0) == 0) "" else serie.tiempo.toString(),
                                            onValueCommit = { tiempo ->
                                                        viewModel.actualizarTiempoSerie(serie.id, parseIntInput(tiempo))
                                            },
                                            label = "Min",
                                                    enabled = !serie.completada,
                                                    allowDecimal = false
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        SerieInputField(
                                            initialValue = if ((serie.intensidad ?: 0) == 0) "" else serie.intensidad.toString(),
                                            onValueCommit = { intensidad ->
                                                        viewModel.actualizarIntensidadSerie(serie.id, parseIntInput(intensidad))
                                            },
                                            label = "Intens",
                                            enabled = !serie.completada
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val checkboxColors = if (erroresValidacion.containsKey(ejercicioConSeries.entrenamientoEjercicio.id)) {
                                        CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.error,
                                            uncheckedColor = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        CheckboxDefaults.colors()
                                    }

                                    Checkbox(
                                        checked = serie.completada,
                                        onCheckedChange = { checked ->
                                            viewModel.marcarSerieCompletada(
                                                serie.id,
                                                checked,
                                                esCardio,
                                                serie.peso,
                                                serie.repeticiones,
                                                serie.tiempo,
                                                serie.intensidad
                                            )
                                        },
                                        colors = checkboxColors
                                    )

                                    if (index == ejercicioConSeries.series.size - 1) {
                                        IconButton(
                                            onClick = { viewModel.eliminarSerie(serie.id) },
                                            enabled = !serie.completada
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.añadirSerie(
                                            ejercicioConSeries.entrenamientoEjercicio.id,
                                            esCardio
                                        )
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("+ Añadir serie")
                                }

                                if (erroresValidacion.containsKey(ejercicioConSeries.entrenamientoEjercicio.id)) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = erroresValidacion[ejercicioConSeries.entrenamientoEjercicio.id] ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
            val todasLasSeriesCompletadas = ejercicios.all { ejercicio ->
                ejercicio.series.isNotEmpty() && ejercicio.series.all { it.completada }
            }

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
                        showFinishConfirmDialog = true
                    },
                    enabled = todasLasSeriesCompletadas,
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
                var filtroExpandido by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = filtroExpandido,
                        onExpandedChange = { filtroExpandido = !filtroExpandido }
                    ) {
                        OutlinedTextField(
                            value = musculoFiltro?.displayLabel() ?: "Todos",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Filtrar por músculo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = filtroExpandido)
                            },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                    }

                    if (filtroExpandido) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gridItems(Musculo.entries.toList()) { musculo ->
                                val seleccionado = musculoFiltro == musculo
                                Surface(
                                    onClick = {
                                        musculoFiltro = if (seleccionado) null else musculo
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (seleccionado) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (seleccionado) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = musculo.imageRes()),
                                            contentDescription = musculo.displayLabel(),
                                            modifier = Modifier.height(65.dp),
                                            contentScale = ContentScale.Fit
                                        )

                                        Text(
                                            text = musculo.displayLabel(),
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ejerciciosNoAgregadosFiltrados) { ejercicio ->
                            EjercicioSelectionCard(
                                nombre = ejercicio.nombre,
                                musculos = ejercicio.musculos,
                                onClick = {
                                    viewModel.añadirEjercicioAlEntrenamiento(ejercicio.id)
                                    showAddExerciseDialog = false
                                },
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
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