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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.input.TextFieldValue

@Composable
private fun SerieHistoricaInputField(
    initialValue: String,
    label: String,
    allowDecimal: Boolean = false,
    modified: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialValue))
    }

    LaunchedEffect(initialValue) {
        if (textFieldValue.text != initialValue) {
            textFieldValue = TextFieldValue(initialValue)
        }
    }

    val colores = if (modified) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.error,
            unfocusedBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.error,
            unfocusedLabelColor = MaterialTheme.colorScheme.error
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val isValid = if (allowDecimal) {
                newValue.text.isEmpty() ||
                        newValue.text.matches(Regex("^\\d*(?:[.,]\\d*)?$"))
            } else {
                newValue.text.isEmpty() ||
                        newValue.text.all { it.isDigit() }
            }

            if (isValid) {
                val normalized = if (allowDecimal) {
                    newValue.text.replace(',', '.')
                } else {
                    newValue.text
                }

                textFieldValue = newValue.copy(text = normalized)
                onValueChange(normalized)
            }
        },
        label = { Text(label) },
        modifier = Modifier.width(88.dp),
        singleLine = true,
        colors = colores,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) {
                KeyboardType.Decimal
            } else {
                KeyboardType.Number
            }
        )
    )
}

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

    val borradoresSeries by viewModel.borradoresSeries.collectAsState()

    var errorGuardarSeries by remember {
        mutableStateOf(false)
    }

    var guardandoSeries by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(entrenamientoId) {
        viewModel.descartarModificacionesSeries()
        errorGuardarSeries = false
    }

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

    val ejerciciosDisponibles by viewModel.ejerciciosDisponibles.collectAsState(
        initial = emptyList()
    )

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showRemoveExerciseDialog by remember { mutableStateOf(false) }

    var selectedExerciseToAddId by remember { mutableStateOf<Long?>(null) }
    var selectedExerciseToRemoveId by remember { mutableStateOf<Long?>(null) }

    var exerciseOperationError by remember { mutableStateOf(false) }

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

                if (borradoresSeries.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                    alpha = 0.35f
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${borradoresSeries.size} cambio(s) pendiente(s) de guardar",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleSmall
                                )

                                Button(
                                    onClick = {
                                        guardandoSeries = true
                                        errorGuardarSeries = false

                                        viewModel.guardarModificacionesSeries(
                                            entrenamientoId
                                        ) { success ->
                                            guardandoSeries = false
                                            errorGuardarSeries = !success
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !guardandoSeries
                                ) {
                                    Text(
                                        if (guardandoSeries) {
                                            "Guardando..."
                                        } else {
                                            "Guardar modificaciones"
                                        }
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.descartarModificacionesSeries()
                                        errorGuardarSeries = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !guardandoSeries
                                ) {
                                    Text("Descartar modificaciones")
                                }

                                if (errorGuardarSeries) {
                                    Text(
                                        text = "No se han podido guardar los cambios. Comprueba que todos los ejercicios conservan al menos una serie.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                items(ejerciciosOrdenados) { ejercicioConSeries ->

                    val borradoresEjercicio = borradoresSeries.values.filter {
                        it.serie.entrenamientoEjercicioId ==
                                ejercicioConSeries.entrenamientoEjercicio.id
                    }

                    val seriesVisibles = buildList {
                        ejercicioConSeries.series.forEach { serieOriginal ->
                            val borrador = borradoresSeries[serieOriginal.id]

                            when {
                                borrador == null -> add(serieOriginal)
                                !borrador.eliminada -> add(borrador.serie)
                            }
                        }

                        borradoresEjercicio
                            .filter { it.esNueva && !it.eliminada }
                            .forEach {
                                add(it.serie)
                            }
                    }

                    val volumen = calcularVolumen(seriesVisibles)

                    val tiempo = calcularTiempo(seriesVisibles)

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

                                if (ejercicioConSeries.entrenamientoEjercicio.anadidoPosteriormente) {
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Añadido posteriormente",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                seriesVisibles.forEachIndexed { index, serie ->

                                    val borrador = borradoresSeries[serie.id]

                                    val serieModificada = borrador?.let {
                                        it.esNueva || it.eliminada || it.serie != it.original
                                    } ?: false

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (serieModificada) {
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                        } else {
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}.",
                                                color = if (serieModificada) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.width(20.dp)
                                            )

                                            if (!esCardio) {

                                                SerieHistoricaInputField(
                                                    initialValue = serie.peso
                                                        ?.takeIf { it != 0f }
                                                        ?.toString()
                                                        ?: "",
                                                    label = "Kg",
                                                    allowDecimal = true,
                                                    modified = serieModificada,
                                                    onValueChange = { valor ->
                                                        val peso = valor.toFloatOrNull() ?: 0f

                                                        viewModel.actualizarPesoSerieHistorica(
                                                            entrenamientoId = entrenamientoId,
                                                            serieId = serie.id,
                                                            peso = peso
                                                        )
                                                    }
                                                )

                                                SerieHistoricaInputField(
                                                    initialValue = serie.repeticiones
                                                        ?.takeIf { it != 0 }
                                                        ?.toString()
                                                        ?: "",
                                                    label = "Reps",
                                                    modified = serieModificada,
                                                    onValueChange = { valor ->
                                                        viewModel.actualizarRepsSerieHistorica(
                                                            entrenamientoId = entrenamientoId,
                                                            serieId = serie.id,
                                                            repeticiones = valor.toIntOrNull() ?: 0
                                                        )
                                                    }
                                                )

                                            } else {

                                                SerieHistoricaInputField(
                                                    initialValue = serie.tiempo
                                                        ?.takeIf { it != 0 }
                                                        ?.toString()
                                                        ?: "",
                                                    label = "Min",
                                                    modified = serieModificada,
                                                    onValueChange = { valor ->
                                                        viewModel.actualizarTiempoSerieHistorica(
                                                            entrenamientoId = entrenamientoId,
                                                            serieId = serie.id,
                                                            tiempo = valor.toIntOrNull() ?: 0
                                                        )
                                                    }
                                                )

                                                SerieHistoricaInputField(
                                                    initialValue = serie.intensidad
                                                        ?.takeIf { it != 0 }
                                                        ?.toString()
                                                        ?: "",
                                                    label = "Intens.",
                                                    modified = serieModificada,
                                                    onValueChange = { valor ->
                                                        viewModel.actualizarIntensidadSerieHistorica(
                                                            entrenamientoId = entrenamientoId,
                                                            serieId = serie.id,
                                                            intensidad = valor.toIntOrNull() ?: 0
                                                        )
                                                    }
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.eliminarSerieHistorica(
                                                        entrenamientoId = entrenamientoId,
                                                        serieId = serie.id
                                                    )
                                                },
                                                enabled = seriesVisibles.size > 1
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar serie",
                                                    tint = if (seriesVisibles.size > 1) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.anadirSerieHistorico(
                                            entrenamientoId = entrenamientoId,
                                            entrenamientoEjercicioId =
                                                ejercicioConSeries.entrenamientoEjercicio.id,
                                            esCardio = esCardio
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("+ Añadir serie")
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
                onAddExercise = {
                    showSettings = false
                    exerciseOperationError = false

                    val idsActuales = entrenamiento?.ejercicios
                        ?.map { it.entrenamientoEjercicio.ejercicioId }
                        .orEmpty()

                    selectedExerciseToAddId = ejerciciosDisponibles
                        .firstOrNull { it.id !in idsActuales }
                        ?.id

                    showAddExerciseDialog = true
                },
                onRemoveExercise = {
                    showSettings = false
                    exerciseOperationError = false

                    selectedExerciseToRemoveId = entrenamiento?.ejercicios
                        ?.firstOrNull()
                        ?.entrenamientoEjercicio
                        ?.id

                    showRemoveExerciseDialog = true
                },
                onDelete = {
                    showSettings = false
                    showDeleteDialog = true
                }
            )
        }
    }

    if (showAddExerciseDialog) {
        val idsActuales = entrenamiento?.ejercicios
            ?.map { it.entrenamientoEjercicio.ejercicioId }
            .orEmpty()

        val ejerciciosParaAnadir = ejerciciosDisponibles
            .filter { it.id !in idsActuales }

        AlertDialog(
            onDismissRequest = {
                showAddExerciseDialog = false
            },
            title = {
                Text("Añadir ejercicio")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (ejerciciosParaAnadir.isEmpty()) {
                        Text("No hay más ejercicios disponibles para añadir.")
                    } else {
                        ejerciciosParaAnadir.forEach { ejercicio ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedExerciseToAddId == ejercicio.id,
                                    onClick = {
                                        selectedExerciseToAddId = ejercicio.id
                                        exerciseOperationError = false
                                    }
                                )

                                Text(
                                    text = ejercicio.nombre,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (exerciseOperationError) {
                        Text(
                            text = "No se ha podido añadir el ejercicio. Inténtalo de nuevo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedExerciseToAddId != null &&
                            ejerciciosParaAnadir.any {
                                it.id == selectedExerciseToAddId
                            },
                    onClick = {
                        val ejercicioId = selectedExerciseToAddId
                            ?: return@TextButton

                        viewModel.anadirEjercicioHistorico(
                            entrenamientoId = entrenamientoId,
                            ejercicioId = ejercicioId
                        ) { success ->
                            if (success) {
                                showAddExerciseDialog = false
                                exerciseOperationError = false
                            } else {
                                exerciseOperationError = true
                            }
                        }
                    }
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddExerciseDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showRemoveExerciseDialog) {
        val ejerciciosDelEntrenamiento = entrenamiento?.ejercicios
            ?.sortedBy { it.entrenamientoEjercicio.orden }
            .orEmpty()

        AlertDialog(
            onDismissRequest = {
                showRemoveExerciseDialog = false
            },
            title = {
                Text("Eliminar ejercicio")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Selecciona el ejercicio que quieres eliminar.")

                    ejerciciosDelEntrenamiento.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedExerciseToRemoveId ==
                                        item.entrenamientoEjercicio.id,
                                onClick = {
                                    selectedExerciseToRemoveId =
                                        item.entrenamientoEjercicio.id
                                    exerciseOperationError = false
                                }
                            )

                            Text(
                                text = item.ejercicio.nombre,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (exerciseOperationError) {
                        Text(
                            text = "No se ha podido eliminar el ejercicio. Inténtalo de nuevo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedExerciseToRemoveId != null,
                    onClick = {
                        val ejercicioId = selectedExerciseToRemoveId
                            ?: return@TextButton

                        viewModel.eliminarEjercicioHistorico(
                            entrenamientoId = entrenamientoId,
                            entrenamientoEjercicioId = ejercicioId
                        ) { success ->
                            if (success) {
                                showRemoveExerciseDialog = false
                                exerciseOperationError = false
                            } else {
                                exerciseOperationError = true
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveExerciseDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
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

