package com.example.gimnasio.ui.historico

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.ui.components.emojiSummary
import com.example.gimnasio.ui.components.primaryImageRes
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoDetailScreen(
    entrenamientoId: Long,
    advancedModificationsEnabled: Boolean,
    onBack: () -> Unit,
    onNavigateToEntrenamiento: (Long) -> Unit,
    onNavigateToEjercicio: (Long) -> Unit
) {

    val context = LocalContext.current

    val viewModel: HistoricoDetailViewModel = viewModel(
        factory = HistoricoDetailViewModelFactory(context)
    )

    val entrenamiento by viewModel
        .getEntrenamientoCompleto(entrenamientoId)
        .collectAsState(initial = null)

    val entrenamientoConRutina by viewModel
        .getEntrenamiento(entrenamientoId)
        .collectAsState(initial = null)

    val entrenamientoActivo by viewModel
        .entrenamientoActivo
        .collectAsState(initial = null)

    val rutinas by viewModel.rutinas.collectAsState(initial = emptyList())

    var showSettings by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }
    var mostrarErrorNombreEntrenamientoDuplicado by remember { mutableStateOf(false) }

    var showChangeRoutineDialog by remember {
        mutableStateOf(false)
    }

    var showChangeDurationDialog by remember {
        mutableStateOf(false)
    }

    var selectedRoutineId by remember {
        mutableStateOf<Long?>(null)
    }

    var durationHours by remember {
        mutableStateOf("0")
    }

    var durationMinutes by remember {
        mutableStateOf("0")
    }

    var advancedOperationError by remember {
        mutableStateOf(false)
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏋️ Detalle entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
                    }
                }

            )
        }
    ) { padding ->

        if (entrenamiento == null || entrenamientoConRutina == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando...")
            }
        } else {

            val data = entrenamiento!!
            val rutina = entrenamientoConRutina!!.rutina
            val ejerciciosOrdenados = data.ejercicios.sortedBy { it.entrenamientoEjercicio.orden }

            fun calcularVolumen(series: List<SerieEntity>): Int {
                return series.sumOf {
                    ((it.peso ?: 0f) * (it.repeticiones ?: 0)).toInt()
                }
            }

            fun calcularTiempo(series: List<SerieEntity>): Float {
                return series.sumOf {
                    (it.tiempo ?: 0f).toDouble()
                }
                    .toFloat()
            }


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = entrenamiento?.entrenamiento?.nombre ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 3
                                )

                                FilledTonalButton(
                                    onClick = {
                                        viewModel.repetirEntrenamiento(data) { nuevoId ->
                                            onNavigateToEntrenamiento(nuevoId)
                                        }
                                    },
                                    enabled = entrenamientoActivo == null
                                ) {
                                    Text("Repetir")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val ejerciciosEmoji = data.ejercicios
                                .map { it.ejercicio.musculos }
                                .flatten()
                                .emojiSummary()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Basado en: ${rutina.nombre}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = ejerciciosEmoji,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                }

                items(ejerciciosOrdenados) { ejercicioConSeries ->

                    val volumen = calcularVolumen(ejercicioConSeries.series)

                    val tiempo = calcularTiempo(ejercicioConSeries.series)

                    val esCardio = ejercicioConSeries.ejercicio.musculos.contains(Musculo.CARDIO)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        onClick = {
                            onNavigateToEjercicio(ejercicioConSeries.ejercicio.id)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = ejercicioConSeries.ejercicio.musculos.primaryImageRes()),
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0.25f),
                                contentScale = ContentScale.Fit
                            )

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = ejercicioConSeries.ejercicio.nombre,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                ejercicioConSeries.series.forEachIndexed { index, serie ->

                                    if (!esCardio) {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Serie ${index + 1}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                text = "${serie.peso}kg × ${serie.repeticiones} reps"
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))


                                    } else {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Serie ${index + 1}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                text = "${serie.tiempo ?: 0}min × ${serie.intensidad} intensidad"
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))


                                    }

                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (!esCardio) {

                                    Text(
                                        text = "Volumen total: $volumen kg",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                } else {

                                    Text(
                                        text = "Tiempo total: $tiempo min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Sin implementar - Intensidad")

                                }

                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            HistoricoDetailSettingsSheet(
                advancedModificationsEnabled = advancedModificationsEnabled,
                onRename = {
                    showSettings = false
                    nuevoNombre = entrenamiento?.entrenamiento?.nombre ?: ""
                    mostrarErrorNombreEntrenamientoDuplicado = false
                    showRenameDialog = true
                },
                onChangeRoutine = {
                    showSettings = false
                    selectedRoutineId = entrenamiento?.entrenamiento?.rutinaId
                    advancedOperationError = false
                    showChangeRoutineDialog = true
                },
                onChangeDuration = {
                    showSettings = false

                    val actual = entrenamiento?.entrenamiento

                    if (actual != null) {
                        val fechaFin = actual.fechaFin ?: actual.fechaInicio
                        val duracionMinutos = (
                                (fechaFin - actual.fechaInicio).coerceAtLeast(0L) / 60_000L
                                )

                        durationHours = (duracionMinutos / 60L).toString()
                        durationMinutes = (duracionMinutos % 60L).toString()
                    }

                    advancedOperationError = false
                    showChangeDurationDialog = true
                },
                onDelete = {
                    showSettings = false
                    showDeleteDialog = true
                }
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Cambiar nombre") },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = {
                        nuevoNombre = it
                        mostrarErrorNombreEntrenamientoDuplicado = false
                    },
                    label = { Text("Nombre del entrenamiento") },
                    singleLine = true
                )

                if (mostrarErrorNombreEntrenamientoDuplicado) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ya existe un entrenamiento con ese nombre. Cambialo.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renombrarEntrenamiento(entrenamientoId, nuevoNombre) { resultado ->
                            when (resultado) {
                                HistoricoDetailViewModel.NombreOperacionResultado.OK -> {
                                    showRenameDialog = false
                                    mostrarErrorNombreEntrenamientoDuplicado = false
                                }

                                HistoricoDetailViewModel.NombreOperacionResultado.DUPLICADO -> {
                                    mostrarErrorNombreEntrenamientoDuplicado = true
                                }

                                HistoricoDetailViewModel.NombreOperacionResultado.VACIO -> {
                                    mostrarErrorNombreEntrenamientoDuplicado = false
                                }
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showChangeRoutineDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangeRoutineDialog = false
            },
            title = {
                Text("Cambiar rutina")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Selecciona la nueva rutina asociada a este entrenamiento."
                    )

                    rutinas.forEach { rutina ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRoutineId == rutina.id,
                                onClick = {
                                    selectedRoutineId = rutina.id
                                    advancedOperationError = false
                                }
                            )

                            Text(
                                text = rutina.nombre,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (advancedOperationError) {
                        Text(
                            text = "No se ha podido cambiar la rutina. Inténtalo de nuevo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedRoutineId != null,
                    onClick = {
                        val rutinaId = selectedRoutineId ?: return@TextButton

                        viewModel.cambiarRutina(
                            entrenamientoId = entrenamientoId,
                            nuevaRutinaId = rutinaId
                        ) { success ->
                            if (success) {
                                showChangeRoutineDialog = false
                                advancedOperationError = false
                            } else {
                                advancedOperationError = true
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangeRoutineDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showChangeDurationDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangeDurationDialog = false
            },
            title = {
                Text("Cambiar duración")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Introduce la duración total del entrenamiento."
                    )

                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = {
                            durationHours = it.filter { char -> char.isDigit() }
                            advancedOperationError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Horas")
                        },
                        suffix = {
                            Text("h")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = {
                            durationMinutes = it.filter { char -> char.isDigit() }
                            advancedOperationError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Minutos")
                        },
                        suffix = {
                            Text("min")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    if (advancedOperationError) {
                        Text(
                            text = "No se ha podido actualizar la duración. Comprueba los valores e inténtalo de nuevo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val horas = durationHours.toLongOrNull()
                        val minutos = durationMinutes.toLongOrNull()

                        if (horas == null || minutos == null || minutos !in 0..59) {
                            advancedOperationError = true
                            return@TextButton
                        }

                        viewModel.actualizarDuracionEntrenamiento(
                            entrenamientoId = entrenamientoId,
                            horas = horas,
                            minutos = minutos
                        ) { success ->
                            if (success) {
                                showChangeDurationDialog = false
                                advancedOperationError = false
                            } else {
                                advancedOperationError = true
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangeDurationDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ⚠️ Diálogo de confirmación de borrar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar '${entrenamiento?.entrenamiento?.nombre ?: "Entrenamiento"}'") },
            text = {
                Text("Esta acción no se puede deshacer. ¿Seguro que quieres eliminar este entrenamiento?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.borrarEntrenamiento(entrenamientoId)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

}

