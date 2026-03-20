package com.example.gimnasio.ui.ejercicios

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjercicioDetailScreen(
    ejercicioId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onGraficaScrollChange: (Boolean) -> Unit
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

    val ejercicioFlow = remember(viewModel, ejercicioId) {
        viewModel.getEjercicio(ejercicioId)
    }
    val ultimaSesionFlow = remember(viewModel, ejercicioId) {
        viewModel.getUltimaSesionFlow(ejercicioId)
    }
    val progresoFuerzaFlow = remember(viewModel, ejercicioId) {
        viewModel.getProgresoEjercicio(ejercicioId)
    }
    val progresoCardioFlow = remember(viewModel, ejercicioId) {
        viewModel.getProgresoEjercicioCardio(ejercicioId)
    }
    val recordsFlow = remember(viewModel, ejercicioId) {
        viewModel.getRecordsEjercicio(ejercicioId)
    }
    val recordsCardioFlow = remember(viewModel, ejercicioId) {
        viewModel.getRecordsEjercicioCardio(ejercicioId)
    }
    val comentarioFlow = remember(viewModel, ejercicioId) {
        viewModel.getComentario(ejercicioId)
    }

    val ejercicio by ejercicioFlow.collectAsState(initial = null)
    val esCardio = remember(ejercicio) {
        ejercicio?.musculos?.contains(Musculo.CARDIO) == true
    }

    val ultimaSesion by ultimaSesionFlow.collectAsState(initial = null)

    val progresoFuerza by progresoFuerzaFlow.collectAsState(initial = emptyList())

    val progresoCardio by progresoCardioFlow.collectAsState(initial = emptyList())

    val progreso = if (esCardio) progresoCardio else progresoFuerza

    val records by recordsFlow.collectAsState(initial = null)

    val recordsCardio by recordsCardioFlow.collectAsState(initial = null)

    val comentarioDB by comentarioFlow.collectAsState(initial = "")

    val pr by viewModel.getPR(ejercicioId).collectAsState(initial = null)

    val mejorSesionFuerza by viewModel.getMejorSesionFuerza(ejercicioId).collectAsState(initial = null)

    val mejorSesionCardio by viewModel.getMejorSesionCardio(ejercicioId).collectAsState(initial = null)

    val mejorCargaCardio by viewModel.getMejorCargaCardio(ejercicioId).collectAsState(initial = null)

    val scrollStateGrafica = rememberScrollState()
    var isScrollingGrafica by remember { mutableStateOf(false) }

    LaunchedEffect(isScrollingGrafica) {
        onGraficaScrollChange(isScrollingGrafica)
    }

    // ----------------------------
    // GRAFICA
    // ----------------------------
    @Composable
    fun GraficaProgresoScrollable(
        valores: List<Float>,
        fechas: List<String>,
        unidad: String,
        scrollState: ScrollState,
        onScrollStateChange: (Boolean) -> Unit = {},
        modifier: Modifier = Modifier
    ) {

        if (valores.isEmpty()) return

        val puntoWidth = 50.dp
        val graficaWidth = puntoWidth * valores.size

        val max = valores.maxOrNull() ?: 1f
        val min = valores.minOrNull() ?: 0f
        val rango = (max - min).takeIf { it != 0f } ?: 1f

        val density = LocalDensity.current

        var containerWidthPx by remember { mutableStateOf(0) }

        val scope = rememberCoroutineScope()

        val isUserScrolling = scrollState.isScrollInProgress
        LaunchedEffect(isUserScrolling) {
            onScrollStateChange(isUserScrolling)
        }

        // Auto-scroll al final
        LaunchedEffect(valores.size) {
            scrollState.scrollTo(scrollState.maxValue)
        }

        var selectedIndex by remember { mutableStateOf<Int?>(null) }
        var selectedOffset by remember { mutableStateOf(Offset.Zero) }

        // Animación tooltip
        val animatedX by animateFloatAsState(
            targetValue = selectedOffset.x,
            label = "tooltipX"
        )
        val animatedY by animateFloatAsState(
            targetValue = selectedOffset.y,
            label = "tooltipY"
        )

        Row(
            modifier = modifier
                .height(260.dp)
                .fillMaxWidth()
        ) {

            // =========================
            // EJE Y
            // =========================
            Canvas(
                modifier = Modifier
                    .width(22.dp)
                    .fillMaxHeight()
            ) {
                val divisiones = 4
                val heightGrafica = size.height - 40f
                val step = heightGrafica / divisiones

                drawLine(
                    color = Color.Gray,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, heightGrafica),
                    strokeWidth = 4f
                )

                for (i in 0..divisiones) {
                    val y = heightGrafica - i * step
                    val valor = min + (rango / divisiones) * i

                    drawLine(
                        color = Color.Gray.copy(alpha = 0.6f),
                        start = Offset(size.width - 10f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3f
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        valor.toInt().toString(),
                        0f,
                        y + 10f,
                        android.graphics.Paint().apply {
                            textSize = 28f
                            color = android.graphics.Color.DKGRAY
                        }
                    )
                }
            }

            // =========================
            // GRAFICA
            // =========================
            Box(modifier = Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .horizontalScroll(
                            state = scrollState,
                            overscrollEffect = null
                        )
                        .onSizeChanged {
                            containerWidthPx = it.width
                        }
                ) {

                    Canvas(
                        modifier = Modifier
                            .width(graficaWidth)
                            .fillMaxHeight()
                            .pointerInput(valores) {
                                detectTapGestures { offset ->

                                    val stepX = size.width / valores.size
                                    val heightGrafica = size.height - 40f

                                    val radioDeteccion = 40f // sensibilidad (ajustar)

                                    var encontrado: Int? = null
                                    var puntoOffset = Offset.Zero

                                    valores.forEachIndexed { i, valor ->

                                        val normalized = (valor - min) / rango

                                        val x = stepX * (i + 0.5f)
                                        val y = heightGrafica - normalized * heightGrafica

                                        val distancia = kotlin.math.hypot(
                                            offset.x - x,
                                            offset.y - y
                                        )

                                        if (distancia < radioDeteccion) {
                                            encontrado = i
                                            puntoOffset = Offset(x, y)
                                        }
                                    }

                                    if (encontrado != null) {
                                        selectedIndex = encontrado
                                        selectedOffset = puntoOffset
                                    } else {
                                        // 👇 AQUÍ está la clave
                                        selectedIndex = null
                                    }
                                }
                            }
                    ) {

                        val stepX = size.width / valores.size
                        val heightGrafica = size.height - 40f

                        val divisiones = 4
                        val stepY = heightGrafica / divisiones

                        // GRID
                        for (i in 0..divisiones) {
                            val y = heightGrafica - i * stepY

                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.5f
                            )
                        }

                        // EJE X
                        drawLine(
                            color = Color.Gray,
                            start = Offset(0f, heightGrafica),
                            end = Offset(size.width, heightGrafica),
                            strokeWidth = 4f
                        )

                        // =========================
                        // 1. LINEAS
                        // =========================
                        val puntos = valores.mapIndexed { i, valor ->

                            val normalized = (valor - min) / rango
                            val x = stepX * (i + 0.5f)
                            val y = heightGrafica - normalized * heightGrafica

                            Offset(x, y)
                        }

                        puntos.forEachIndexed { i, punto ->

                            if (i > 0) {

                                val prev = puntos[i - 1]
                                val valor = valores[i]
                                val prevValor = valores[i - 1]

                                val colorLinea = when {
                                    valor > prevValor -> Color(0xFF4CAF50)
                                    valor < prevValor -> Color.Red
                                    else -> Color.Blue
                                }

                                drawLine(
                                    color = colorLinea,
                                    start = prev,
                                    end = punto,
                                    strokeWidth = 6f
                                )
                            }
                        }

                        // =========================
                        // 2. PUNTOS
                        // =========================
                        puntos.forEachIndexed { i, punto ->

                            val isSelected = selectedIndex == i

                            // círculo exterior
                            drawCircle(
                                color = if (isSelected) Color.Yellow else Color.Black,
                                radius = if (isSelected) 15f else 10f,
                                center = punto
                            )

                            // círculo interior
                            drawCircle(
                                color = Color.Black,
                                radius = 6f,
                                center = punto
                            )

                            // fecha
                            if (i < fechas.size) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    fechas[i],
                                    punto.x - 25f,
                                    size.height - 5f,
                                    android.graphics.Paint().apply {
                                        textSize = 26f
                                        color = android.graphics.Color.DKGRAY
                                    }
                                )
                            }
                        }

                    }
                }

                // =========================
                // TOOLTIP SOBRE EL PUNTO
                // =========================
                selectedIndex?.let { index ->

                    val valor = valores[index]
                    val fecha = fechas.getOrNull(index) ?: ""

                    val xDp = with(density) { animatedX.toDp() }
                    val yDp = with(density) { animatedY.toDp() }

                    Card(
                        modifier = Modifier
                            .offset(
                                x = xDp - 40.dp,
                                y = yDp - 70.dp
                            ),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text("📅 $fecha")
                            Text("📊 ${valor.toInt()} $unidad")
                        }
                    }
                }

                // =========================
                // FLECHAS
                // =========================
                if (scrollState.value > 0) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                scrollState.animateScrollBy(-containerWidthPx.toFloat())
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }

                if (scrollState.value < scrollState.maxValue) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                scrollState.animateScrollBy(containerWidthPx.toFloat())
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
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

                                if(esCardio) {
                                    Text("🏃‍♂️ Cardio", style = MaterialTheme.typography.titleLarge)
                                } else {
                                    Text("💪 Músculo", style = MaterialTheme.typography.titleLarge)

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        ejercicioLocal.musculos.joinToString { it.name }
                                    )
                                }
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
                                                        "Serie ${index + 1}: ${serie.peso ?: 0} kg × ${serie.repeticiones ?: 0} reps"
                                                    )
                                                } else {
                                                    Text(
                                                        "Serie ${index + 1}: ${serie.tiempo ?: 0} min × ${serie.intensidad ?: 1} intensidad"
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
                    //     COMENTARIOS
                    // -------------------
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Text(
                                    text = "💬 Comentarios",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(Modifier.height(12.dp))


                                // Estado editable por el usuario, inicializado con DB
                                var comentarioTemp by rememberSaveable { mutableStateOf(comentarioDB) }

                                // Si cambia el valor de la DB, actualizamos el TextField
                                LaunchedEffect(comentarioDB) {
                                    comentarioTemp = comentarioDB
                                }

                                OutlinedTextField(
                                    value = comentarioTemp,
                                    onValueChange = { nuevoValor ->
                                        comentarioTemp = nuevoValor
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 120.dp),
                                    placeholder = { Text("Escribe un comentario...") },
                                    maxLines = Int.MAX_VALUE,
                                    singleLine = false
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    // Botón Cancelar: restaura el valor original
                                    Button(
                                        onClick = { comentarioTemp = comentarioDB },
                                        enabled = comentarioTemp != comentarioDB
                                    ) {
                                        Text("Cancelar")
                                    }

                                    // Botón Guardar: actualiza DB
                                    Button(
                                        onClick = {
                                            viewModel.actualizarComentario(
                                                ejercicioId,
                                                if (comentarioTemp.isBlank()) null else comentarioTemp
                                            )
                                        },
                                        enabled = comentarioTemp != comentarioDB
                                    ) {
                                        Text("Guardar")
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

                                    // -------------------
                                    // DATOS BASE
                                    // -------------------

                                    val valores = remember(progreso) {
                                        progreso.map { it.valor }
                                    }

                                    val unidad = if (esCardio) "pts" else "kg"

                                    val primerValor = valores.first()
                                    val ultimoValor = valores.last()
                                    val diferencia = ultimoValor - primerValor
                                    val colorDiferencia = when {
                                        diferencia > 0 -> Color(0xFF4CAF50)
                                        diferencia < 0 -> Color.Red
                                        else -> Color.Gray
                                    }

                                    val fechas = remember(progreso) {
                                        progreso.map {
                                            val fecha = Instant
                                                .ofEpochMilli(it.fecha)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()

                                            "${fecha.dayOfMonth}/${fecha.monthValue}"
                                        }
                                    }

                                    // -------------------
                                    // TEXTO
                                    // -------------------

                                    if (esCardio) {

                                        FilaDato(
                                            "Primer valor registrado:",
                                            "${primerValor.toInt()} $unidad"
                                        )

                                        FilaDato(
                                            "Último valor registrado:",
                                            "${ultimoValor.toInt()} $unidad"
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        FilaDato(
                                            "Diferencia:",
                                            "${if (diferencia >= 0) "+" else "-"}${diferencia.toInt()} $unidad",
                                            colorDiferencia
                                        )

                                    } else {

                                        FilaDato(
                                            "Primer valor registrado:",
                                            "$primerValor $unidad"
                                        )

                                        FilaDato(
                                            "Último valor registrado:",
                                            "$ultimoValor $unidad"
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        FilaDato(
                                            "Diferencia:",
                                            "${if (diferencia >= 0) "+" else ""}$diferencia $unidad",
                                            colorDiferencia
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // -------------------
                                    // GRAFICAS
                                    // -------------------

                                    val valoresExtra = remember(progreso, esCardio) {
                                        if (esCardio) {
                                            progreso.map { (it.tiempo ?: 0).toFloat() }
                                        } else {
                                            progreso.map { it.pesoMax ?: 0f }
                                        }
                                    }

                                    Text(
                                        text = if (esCardio) "Tiempo total (min)" else "Peso máximo (kg)",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    GraficaProgresoScrollable(
                                        valores = valoresExtra,
                                        fechas = fechas,
                                        unidad = if (esCardio) "min" else "kg",
                                        scrollState = scrollStateGrafica,
                                        onScrollStateChange = { isScrollingGrafica = it },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(24.dp))

                                    Text(
                                        text = if (esCardio) "Carga de cardio (pts)" else "Volumen levantado (kg)",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    GraficaProgresoScrollable(
                                        valores = valores,
                                        fechas = fechas,
                                        unidad = if (esCardio) "min" else "kg",
                                        scrollState = scrollStateGrafica,
                                        onScrollStateChange = { isScrollingGrafica = it },
                                        modifier = Modifier.fillMaxWidth()
                                    )

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

                                        pr?.let {
                                            FilaDato("PR:", "${it.pr} kg")
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Mayor volumen en 1 serie:",
                                            "${records!!.volumenMaxSerie ?: 0f} kg"
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        mejorSesionFuerza?.let {
                                            FilaDato("Mejor sesión:", "${it.mejorSesion} kg")
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Volumen total:",
                                            "${records!!.volumenTotal ?: 0f} kg"
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Series totales:",
                                            "${records!!.seriesTotales}"
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Repeticiones totales:",
                                            "${records!!.repeticionesTotales ?: 0}"
                                        )
                                    }

                                } else {

                                    if (recordsCardio == null) {
                                        Text("Aún no hay estadísticas.")
                                    } else {

                                        mejorCargaCardio?.let {
                                            FilaDato(
                                                "Mejor carga:",
                                                "${it.carga}"
                                            )

                                            FilaDato(
                                                "",
                                                "${it.tiempo} min × ${it.intensidad} intensidad"
                                            )
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        mejorSesionCardio?.let {
                                            FilaDato("Mejor sesión", "${it.mejorSesion}")
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Mejor carga en 1 serie:",
                                            "${recordsCardio!!.mejorTiempo?.toInt() ?: 0} pts"
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Carga total:",
                                            "${recordsCardio!!.tiempoTotal?.toInt() ?: 0} pts"
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Series totales:",
                                            "${recordsCardio!!.seriesTotales}"
                                        )
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

@Composable
fun FilaDato(
    label: String,
    valor: String,
    colorValor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(valor, color = colorValor)
    }
}