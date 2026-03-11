package com.example.gimnasio.ui.ejercicios

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.model.PuntoProgreso
import com.example.gimnasio.data.model.UltimaSesionEjercicio
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjercicioDetailScreen(
    ejercicioId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }

    val viewModel: EjercicioViewModel = viewModel(
        factory = EjercicioViewModelFactory(database)
    )

    var showSettings by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showEditMusculosDialog by remember { mutableStateOf(false) }

    var nuevoNombre by remember { mutableStateOf("") }
    var musculosSeleccionados by remember { mutableStateOf(setOf<Musculo>()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ejercicio by viewModel
        .getEjercicio(ejercicioId)
        .collectAsState()

    val ultimaSesion by viewModel
        .getUltimaSesionFlow(ejercicioId)
        .collectAsState(initial = null)

    val progreso by viewModel
        .getProgresoEjercicio(ejercicioId)
        .collectAsState(initial = emptyList())

    val records by viewModel
        .getRecordsEjercicio(ejercicioId)
        .collectAsState(initial = null)

    val recordsCardio by viewModel
        .getRecordsEjercicioCardio(ejercicioId)
        .collectAsState(initial = null)

    //val esCardio = ejercicio?.musculos?.contains(Musculo.CARDIO) == true

    fun formatearFecha(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    // ----------------------------
    // GRAFICA DE PESO
    // ----------------------------
    @Composable
    fun GraficaProgresoPeso(progreso: List<PuntoProgreso>, modifier: Modifier = Modifier) {
        if (progreso.size < 2) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no hay suficientes datos para la gráfica")
            }
            return
        }

        val pesos = progreso.mapNotNull { it.pesoMax }
        val maxPeso = pesos.maxOrNull() ?: 1f
        val minPeso = pesos.minOrNull() ?: 0f
        val rangoPeso = (maxPeso - minPeso).takeIf { it != 0f } ?: 1f

        val colorLinea = MaterialTheme.colorScheme.primary
        val colorEjes = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

        Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
            val paddingLeft = 90f
            val paddingBottom = 60f
            val width = size.width - paddingLeft
            val height = size.height - paddingBottom
            val stepX = width / (progreso.size - 1)

            // EJE Y
            drawLine(color = colorEjes, start = Offset(paddingLeft, 0f), end = Offset(paddingLeft, height), strokeWidth = 4f)
            // EJE X
            drawLine(color = colorEjes, start = Offset(paddingLeft, height), end = Offset(size.width, height), strokeWidth = 4f)

            val divisiones = 4
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 30f
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            for (i in 0..divisiones) {
                val y = height - (height / divisiones) * i
                drawLine(color = colorEjes.copy(alpha = 0.2f), start = Offset(paddingLeft, y), end = Offset(size.width, y), strokeWidth = 2f)
                val peso = minPeso + (rangoPeso / divisiones) * i
                drawContext.canvas.nativeCanvas.drawText("${peso.toInt()}", paddingLeft - 20f, y + 10f, textPaint)
            }

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 34f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawContext.canvas.nativeCanvas.drawText("Entrenamientos", paddingLeft + width / 2, size.height - 10f, labelPaint)
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(-90f, 20f, height / 2)
            drawContext.canvas.nativeCanvas.drawText("Peso (kg)", 20f, height / 2, labelPaint)
            drawContext.canvas.nativeCanvas.restore()

            val puntos = progreso.mapIndexedNotNull { index, punto ->
                punto.pesoMax?.let {
                    val x = paddingLeft + index * stepX
                    val normalized = (it - minPeso) / rangoPeso
                    val y = height - (normalized * height)
                    Offset(x, y)
                }
            }

            for (i in 0 until puntos.size - 1) {
                drawLine(color = colorLinea, start = puntos[i], end = puntos[i + 1], strokeWidth = 6f)
            }
            puntos.forEach { drawCircle(color = colorLinea, radius = 8f, center = it) }
        }
    }

    // ----------------------------
    // GRAFICA DE TIEMPO (CARDIO)
    // ----------------------------
    @Composable
    fun GraficaProgresoTiempo(progreso: List<PuntoProgreso>, modifier: Modifier = Modifier) {
        if (progreso.size < 2) {
            Box(
                modifier = modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) { Text("Aún no hay suficientes datos para la gráfica") }
            return
        }

        val tiempos = progreso.mapNotNull { it.tiempo }
        val maxTiempo = tiempos.maxOrNull() ?: 1
        val minTiempo = tiempos.minOrNull() ?: 0
        val rangoTiempo = (maxTiempo - minTiempo).takeIf { it != 0 } ?: 1

        val colorLinea = MaterialTheme.colorScheme.primary
        val colorEjes = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

        Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
            val paddingLeft = 90f
            val paddingBottom = 60f
            val width = size.width - paddingLeft
            val height = size.height - paddingBottom
            val stepX = width / (progreso.size - 1)

            // EJE Y
            drawLine(color = colorEjes, start = Offset(paddingLeft, 0f), end = Offset(paddingLeft, height), strokeWidth = 4f)
            // EJE X
            drawLine(color = colorEjes, start = Offset(paddingLeft, height), end = Offset(size.width, height), strokeWidth = 4f)

            val divisiones = 4
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 30f
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            for (i in 0..divisiones) {
                val y = height - (height / divisiones) * i
                drawLine(color = colorEjes.copy(alpha = 0.2f), start = Offset(paddingLeft, y), end = Offset(size.width, y), strokeWidth = 2f)
                val tiempo = minTiempo + (rangoTiempo / divisiones) * i
                drawContext.canvas.nativeCanvas.drawText("${tiempo} min", paddingLeft - 20f, y + 10f, textPaint)
            }

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 34f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawContext.canvas.nativeCanvas.drawText("Entrenamientos", paddingLeft + width / 2, size.height - 10f, labelPaint)
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(-90f, 20f, height / 2)
            drawContext.canvas.nativeCanvas.drawText("Tiempo (min)", 20f, height / 2, labelPaint)
            drawContext.canvas.nativeCanvas.restore()

            val puntos = progreso.mapIndexedNotNull { index, punto ->
                punto.tiempo?.let {
                    val x = paddingLeft + index * stepX
                    val normalized = (it - minTiempo).toFloat() / rangoTiempo
                    val y = height - (normalized * height)
                    Offset(x, y)
                }
            }

            for (i in 0 until puntos.size - 1) {
                drawLine(color = colorLinea, start = puntos[i], end = puntos[i + 1], strokeWidth = 6f)
            }
            puntos.forEach { drawCircle(color = colorLinea, radius = 8f, center = it) }
        }
    }

    // ----------------------------
    // CONTENIDO PRINCIPAL
    // ----------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ejercicio?.nombre ?: "Ejercicio") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
                actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "Ajustes") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            ejercicio?.let { ejercicioLocal ->

                val esCardio = ejercicioLocal.musculos.contains(Musculo.CARDIO)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // -------------------
                    //       MÚSCULO
                    // -------------------

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Text("💪 Músculo", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    ejercicioLocal.musculos.joinToString { it.name }
                                )
                            }
                        }
                    }

                    // -------------------
                    //    ÚLTIMA SESIÓN
                    // -------------------

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Text("Última sesión", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(12.dp))

                                if (ultimaSesion == null) {

                                    Text("Aún no se ha realizado este ejercicio.")

                                } else {

                                    Card(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {

                                            val fecha = Instant
                                                .ofEpochMilli(ultimaSesion!!.fecha)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()

                                            Text("Fecha: $fecha")

                                            Spacer(Modifier.height(8.dp))

                                            ultimaSesion!!.series.forEachIndexed { index, serie ->

                                                if (!esCardio) {
                                                    Text(
                                                        "Serie ${index + 1}: ${serie.peso ?: 0} kg x ${serie.repeticiones ?: 0}"
                                                    )
                                                } else {
                                                    Text(
                                                        "Serie ${index + 1}: ${serie.tiempo ?: 0} min"
                                                    )
                                                }

                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }

                    // -------------------
                    //       ACTIVIDAD
                    // -------------------

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Text("📈 Actividad", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(12.dp))

                                if (progreso.isEmpty()) {

                                    Text("Aún no hay datos de progreso.")

                                } else {

                                    if (!esCardio) {

                                        val primerPeso = progreso.first().pesoMax ?: 0f
                                        val ultimoPeso = progreso.last().pesoMax ?: 0f
                                        val diferencia = ultimoPeso - primerPeso

                                        Text("Primer peso registrado: ${primerPeso} kg")
                                        Text("Último peso registrado: ${ultimoPeso} kg")

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = "Diferencia: ${if (diferencia >= 0) "+" else ""}$diferencia kg",
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Spacer(Modifier.height(16.dp))

                                        GraficaProgresoPeso(
                                            progreso = progreso,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                    } else {

                                        val tiempos = progreso.map { it.tiempo ?: 0 }

                                        val primerTiempo = tiempos.first()
                                        val ultimoTiempo = tiempos.last()
                                        val diferencia = ultimoTiempo - primerTiempo

                                        Text("Primer tiempo registrado: $primerTiempo min")
                                        Text("Último tiempo registrado: $ultimoTiempo min")

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = "Diferencia: ${if (diferencia >= 0) "+" else ""}$diferencia min",
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Spacer(Modifier.height(16.dp))

                                        GraficaProgresoTiempo(
                                            progreso = progreso,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // -------------------
                    //        RECORDS
                    // -------------------

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Text("🏆 Records", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(12.dp))

                                if (!esCardio) {

                                    if (records == null) {
                                        Text("Aún no hay estadísticas.")
                                    } else {

                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Mayor volumen en 1 serie:")
                                                Text("${records!!.volumenMaxSerie ?: 0f} kg")
                                            }
                                        }

                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Volumen total:")
                                                Text("${records!!.volumenTotal ?: 0f} kg")
                                            }
                                        }

                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Series totales:")
                                                Text("${records!!.seriesTotales}")
                                            }
                                        }

                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Repeticiones totales:")
                                                Text("${records!!.repeticionesTotales ?: 0}")
                                            }
                                        }

                                    }

                                } else {

                                    if (recordsCardio == null) {
                                        Text("Aún no hay estadísticas.")
                                    } else {

                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Mejor tiempo:")
                                                Text("${recordsCardio!!.mejorTiempo ?: 0} min")
                                            }
                                        }

                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Tiempo total:")
                                                Text("${recordsCardio!!.tiempoTotal ?: 0} min")
                                            }
                                        }

                                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Series totales:")
                                                Text("${recordsCardio!!.seriesTotales}")
                                            }
                                        }

                                    }

                                }

                            }
                        }
                    }

                }

            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }


        }
    }

    // ----------------------------
    // BottomSheet de ajustes
    // ----------------------------
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            EjercicioSettingsSheet(
                onRename = {
                    showSettings = false
                    nuevoNombre = ejercicio?.nombre ?: ""
                    showRenameDialog = true
                },
                onDelete = {
                    showSettings = false
                    showDeleteDialog = true
                },
                onEditMusculos = {
                    showSettings = false
                    musculosSeleccionados = ejercicio?.musculos?.toSet() ?: emptySet()
                    showEditMusculosDialog = true
                }
            )
        }
    }

    // ----------------------------
    // Diálogo borrar
    // ----------------------------
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar '${ejercicio?.nombre ?: "Ejercicio"}'") },
            text = { Text("Esta acción no se puede deshacer. ¿Seguro que quieres eliminar este ejercicio?") },
            confirmButton = { TextButton(onClick = { viewModel.borrarEjercicio(ejercicioId); showDeleteDialog = false; onBack() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    // ----------------------------
    // Diálogo renombrar
    // ----------------------------
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Cambiar nombre") },
            text = { OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it }, label = { Text("Nombre del ejercicio") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.renombrarEjercicio(ejercicioId, nuevoNombre); showRenameDialog = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") } }
        )
    }

    // ----------------------------
    // Diálogo editar músculos
    // ----------------------------
    if (showEditMusculosDialog) {
        AlertDialog(
            onDismissRequest = { showEditMusculosDialog = false },
            title = { Text("Selecciona músculos") },
            text = {
                Column {
                    Musculo.values().forEach { musculo ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = musculosSeleccionados.contains(musculo),
                                onCheckedChange = { checked ->
                                    musculosSeleccionados = if (checked) musculosSeleccionados + musculo else musculosSeleccionados - musculo
                                }
                            )
                            Text(musculo.name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.actualizarMusculos(ejercicioId, musculosSeleccionados.toList()); showEditMusculosDialog = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { showEditMusculosDialog = false }) { Text("Cancelar") } }
        )
    }
}