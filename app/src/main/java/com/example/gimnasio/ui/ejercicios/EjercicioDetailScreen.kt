package com.example.gimnasio.ui.ejercicios

import android.os.Build
import android.app.Application
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.R
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.components.displayLabel
import com.example.gimnasio.ui.components.formatUiNumber
import com.example.gimnasio.ui.components.labelWithEmoji
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: String = "",
    compactTitle: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (compactTitle) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (icon.isNotEmpty()) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (icon.isNotEmpty()) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (compactTitle) {
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                } else {
                    Modifier
                }
            )
        }
    }
}

private fun sectionToIndex(section: String): Int {
    return when (section) {
        "Músculo" -> 0
        "Última sesión" -> 1
        "Comentarios" -> 2
        "Actividad" -> 3
        "Records" -> 4
        else -> 0
    }
}

private fun musculoImageRes(musculo: Musculo): Int {
    return when (musculo) {
        Musculo.PECHO -> R.drawable.ic_muscle_pecho
        Musculo.ESPALDA -> R.drawable.ic_muscle_espalda
        Musculo.HOMBROS -> R.drawable.ic_muscle_hombros
        Musculo.BICEPS -> R.drawable.ic_muscle_biceps
        Musculo.TRICEPS -> R.drawable.ic_muscle_triceps
        Musculo.ANTEBRAZOS -> R.drawable.ic_muscle_antebrazo
        Musculo.CUÁDRICEPS -> R.drawable.ic_muscle_cuadriceps
        Musculo.FEMORAL -> R.drawable.ic_muscle_femoral
        Musculo.ADUCTOR -> R.drawable.ic_muscle_aductor
        Musculo.ABDUCTOR -> R.drawable.ic_muscle_abductor
        Musculo.GEMELOS -> R.drawable.ic_muscle_gemelos
        Musculo.ABDOMINALES -> R.drawable.ic_muscle_abdominales
        Musculo.CARDIO -> R.drawable.ic_muscle_cardio
    }
}

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
        factory = EjercicioViewModelFactory(context.applicationContext as Application, database)
    )

    var showSettings by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showEditMusculosDialog by remember { mutableStateOf(false) }
    var mostrarErrorNombreEjercicioDuplicado by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    var nuevoNombre by remember { mutableStateOf("") }
    var musculoSeleccionado by remember { mutableStateOf<Musculo?>(null) }
    var mostrarErrorMusculo by remember { mutableStateOf(false) }

    // ========== MENÚ DE SECCIONES ==========
    val sections = listOf("Músculo", "Última sesión", "Comentarios", "Actividad", "Records")
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showSectionsMenu by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf("Músculo") }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index ->
                when {
                    index == 0 -> "Músculo"
                    index == 1 -> "Última sesión"
                    index == 2 -> "Comentarios"
                    index == 3 -> "Actividad"
                    else -> "Records"
                }
            }
            .collectLatest { currentSection = it }
    }

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

    @Composable
    fun GraficaProgresoScrollable(
        valores: List<Float>,
        fechas: List<String>,
        unidad: String,
        detallesTooltip: List<String?> = emptyList(),
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
        val promedio = valores.average().toFloat()
        val divisiones = 4
        val yAxisValores = remember(min, rango, divisiones) {
            (divisiones downTo 0).map { i ->
                min + (rango / divisiones) * i
            }
        }
        val xAxisLabelSpaceDp = 40.dp
        val xAxisLabelSpacePx = with(LocalDensity.current) { xAxisLabelSpaceDp.toPx() }

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

        Column(modifier = modifier) {
            Row(
                modifier = Modifier
                    .height(260.dp)
                    .fillMaxWidth()
            ) {

                // =========================
                // EJE Y
                // =========================
                Box(
                    modifier = Modifier
                        .width(62.dp)
                        .fillMaxHeight()
                ) {
                    val yAxisHeightPx = with(LocalDensity.current) { 260.dp.toPx() }
                    val yAxisGraphHeightPx = (yAxisHeightPx - xAxisLabelSpacePx).coerceAtLeast(1f)
                    val yAxisStepPx = yAxisGraphHeightPx / divisiones

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val heightGrafica = size.height - xAxisLabelSpacePx
                        val step = heightGrafica / divisiones

                        drawLine(
                            color = Color.Gray,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, heightGrafica),
                            strokeWidth = 4f
                        )

                        for (i in 0..divisiones) {
                            val y = heightGrafica - i * step
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.6f),
                                start = Offset(size.width - 10f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 3f
                            )
                        }
                    }

                    yAxisValores.forEachIndexed { index, valorTick ->
                        val yOffsetPx = index * yAxisStepPx
                        Text(
                            text = valorTick.formatUiNumber(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(
                                    x = (-14).dp,
                                    y = with(LocalDensity.current) { yOffsetPx.toDp() } - 8.dp
                                )
                        )
                    }
                }

                // =========================
                // GRAFICA
                // =========================
                Box(modifier = Modifier.fillMaxSize()) {

                    Row(
                        modifier = Modifier
                            .horizontalScroll(state = scrollState)
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
                                        val heightGrafica = size.height - xAxisLabelSpacePx

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
                            val heightGrafica = size.height - xAxisLabelSpacePx

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

                            // LÍNEA DE PROMEDIO
                            val normalizedPromedio = (promedio - min) / rango
                            val yPromedio = heightGrafica - normalizedPromedio * heightGrafica
                            drawLine(
                                color = Color(0xFFFF9800).copy(alpha = 0.7f),
                                start = Offset(0f, yPromedio),
                                end = Offset(size.width, yPromedio),
                                strokeWidth = 3f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 5f)
                                )
                            )

                            // EJE X
                            drawLine(
                                color = Color.Gray,
                                start = Offset(0f, heightGrafica),
                                end = Offset(size.width, heightGrafica),
                                strokeWidth = 4f
                            )

                            // =========================
                            // PUNTOS Y LÍNEAS
                            // =========================
                            val puntos = valores.mapIndexed { i, valor ->

                                val normalized = (valor - min) / rango
                                val x = stepX * (i + 0.5f)
                                val y = heightGrafica - normalized * heightGrafica

                                Offset(x, y)
                            }

                            // RELLENO BAJO LA LÍNEA
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(puntos.first().x, heightGrafica)
                                puntos.forEach { moveTo(it.x, it.y) }
                                lineTo(puntos.last().x, heightGrafica)
                                close()
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                            )

                            // LÍNEAS ENTRE PUNTOS
                            puntos.forEachIndexed { i, punto ->

                                if (i > 0) {

                                    val prev = puntos[i - 1]
                                    val valor = valores[i]
                                    val prevValor = valores[i - 1]

                                    val colorLinea = when {
                                        valor > prevValor -> Color(0xFF4CAF50)
                                        valor < prevValor -> Color(0xFFE53935)
                                        else -> Color(0xFF1976D2)
                                    }

                                    drawLine(
                                        color = colorLinea,
                                        start = prev,
                                        end = punto,
                                        strokeWidth = 5f
                                    )
                                }
                            }

                            // PUNTOS
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
                    // TOOLTIP MEJORADO
                    // =========================
                    selectedIndex?.let { index ->

                        val valor = valores[index]
                        val fecha = fechas.getOrNull(index) ?: ""
                        val detalle = detallesTooltip.getOrNull(index)
                        val diferencia = if (index > 0) valor - valores[index - 1] else 0f
                        val colorDiferencia = when {
                            diferencia > 0 -> Color(0xFF4CAF50)
                            diferencia < 0 -> Color(0xFFE53935)
                            else -> Color.Gray
                        }

                        val xDp = with(density) { animatedX.toDp() }
                        val yDp = with(density) { animatedY.toDp() }

                        Card(
                            modifier = Modifier
                                .offset(
                                    x = xDp - 60.dp,
                                    y = yDp - 100.dp
                                ),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "📅 $fecha",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "📊 ${valor.formatUiNumber()} $unidad",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!detalle.isNullOrBlank()) {
                                    Text(
                                        "🏷 $detalle",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (diferencia != 0f) {
                                    Text(
                                        "${if (diferencia > 0) "▲" else "▼"} ${kotlin.math.abs(diferencia).formatUiNumber()} $unidad",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorDiferencia
                                    )
                                }
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

            // =========================
            // ESTADÍSTICAS DEBAJO
            // =========================
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Máximo",
                    value = "${max.toInt()}",
                    icon = "📈",
                    compactTitle = true,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Mínimo",
                    value = "${min.toInt()}",
                    icon = "📉",
                    compactTitle = true,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Promedio",
                    value = "${promedio.toInt()}",
                    icon = "📊",
                    compactTitle = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // ----------------------------
    // CONTENIDO PRINCIPAL
    // ----------------------------
    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = onBack) { Text("←") }

                    Text(
                        text = ejercicio?.nombre ?: "Ejercicio",
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 8.dp, end = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip
                    )

                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            ejercicio?.let { ejercicioLocal ->

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ejercicioLocal.musculos.firstOrNull()?.let { musculo ->
                                    Image(
                                        painter = painterResource(id = musculoImageRes(musculo)),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .alpha(0.25f),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Column(Modifier.padding(16.dp)) {
                                    if (esCardio) {
                                        Text("🏃 Cardio", style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.height(88.dp))
                                    } else {
                                        Text("Músculo", style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.height(68.dp))
                                        Text(
                                            ejercicioLocal.musculos.joinToString(" • ") { it.labelWithEmoji() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // -------------------
                    //    ÚLTIMA SESIÓN
                    // -------------------

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("⏱️ Última sesión", style = MaterialTheme.typography.titleLarge)
                                }

                                Spacer(Modifier.height(12.dp))

                                if (ultimaSesion == null) {

                                    Text("Aún no se ha realizado este ejercicio.")

                                } else {

                                    val fecha = Instant
                                        .ofEpochMilli(ultimaSesion!!.fecha)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()

                                    // Mostrar fecha en un card destacado
                                    StatCard(
                                        title = "Fecha",
                                        value = fecha.format(dateFormatter),
                                        icon = "📅"
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Text(
                                        "Series realizadas",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    // Mostrar cada serie con mejor formato
                                    ultimaSesion!!.series.forEachIndexed { index, serie ->

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp)),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            elevation = CardDefaults.cardElevation(1.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Serie ${index + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )

                                                    Spacer(Modifier.width(32.dp))

                                                    if (!esCardio) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    "${serie.peso ?: 0}",
                                                                    style = MaterialTheme.typography.headlineSmall,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text(
                                                                    "kg",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }

                                                            Text("x", style = MaterialTheme.typography.titleLarge)

                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    "${serie.repeticiones ?: 0}",
                                                                    style = MaterialTheme.typography.headlineSmall,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text(
                                                                    "reps",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    "${serie.tiempo ?: 0}",
                                                                    style = MaterialTheme.typography.headlineSmall,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text(
                                                                    "min",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }

                                                            Text("@", style = MaterialTheme.typography.titleLarge)

                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(
                                                                    "${serie.intensidad ?: 1}",
                                                                    style = MaterialTheme.typography.headlineSmall,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                Text(
                                                                    "intensidad",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (index < ultimaSesion!!.series.size - 1) {
                                            Spacer(Modifier.height(8.dp))
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
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

                                    val valoresVolumen = remember(progreso) {
                                        progreso.map { it.valor }
                                    }

                                    val valoresExtra = remember(progreso, esCardio) {
                                        if (esCardio) {
                                            progreso.map { (it.tiempo ?: 0).toFloat() }
                                        } else {
                                            progreso.map { it.pesoMax ?: 0f }
                                        }
                                    }

                                    val unidadExtra = if (esCardio) "min" else "kg"
                                    val unidadVolumen = if (esCardio) "pts" else "kg"

                                    val fechas = remember(progreso) {
                                        progreso.map {
                                            val fecha = Instant
                                                .ofEpochMilli(it.fecha)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()

                                            fecha.format(dateFormatter)
                                        }
                                    }

                                    val detallesGraficaExtra = remember(progreso, esCardio) {
                                        if (esCardio) {
                                            progreso.map {
                                                val tiempo = it.tiempo ?: 0
                                                val intensidad = it.intensidadTiempoMax ?: 0
                                                if (tiempo > 0) "$tiempo min x $intensidad intensidad" else null
                                            }
                                        } else {
                                            progreso.map {
                                                val pesoMax = it.pesoMax ?: 0f
                                                val reps = it.repeticionesPesoMax ?: 0
                                                if (pesoMax > 0f) "${pesoMax.formatUiNumber()} kg x $reps reps" else null
                                            }
                                        }
                                    }

                                    // -------------------
                                    // GRAFICA 1 (EXTRA)
                                    // -------------------

                                    Text(
                                        text = if (esCardio) "Tiempo total (min)" else "Peso máximo (kg)",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    val primerExtra = valoresExtra.first()
                                    val ultimoExtra = valoresExtra.last()
                                    val diferenciaExtra = ultimoExtra - primerExtra
                                    val colorDiferenciaExtra = when {
                                        diferenciaExtra > 0 -> Color(0xFF4CAF50)
                                        diferenciaExtra < 0 -> Color.Red
                                        else -> Color.Gray
                                    }

                                    if (esCardio) {

                                        FilaDato(
                                            "Primer valor registrado:",
                                            "${primerExtra.formatUiNumber()} $unidadExtra"
                                        )

                                        FilaDato(
                                            "Último valor registrado:",
                                            "${ultimoExtra.formatUiNumber()} $unidadExtra"
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        FilaDato(
                                            "Diferencia:",
                                            "${if (diferenciaExtra >= 0) "+" else "-"}${kotlin.math.abs(diferenciaExtra).formatUiNumber()} $unidadExtra",
                                            colorDiferenciaExtra
                                        )

                                    } else {

                                        FilaDato(
                                            "Primer valor registrado:",
                                            "${primerExtra.formatUiNumber()} $unidadExtra"
                                        )

                                        FilaDato(
                                            "Último valor registrado:",
                                            "${ultimoExtra.formatUiNumber()} $unidadExtra"
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        FilaDato(
                                            "Diferencia:",
                                            "${if (diferenciaExtra >= 0) "+" else ""}${diferenciaExtra.formatUiNumber()} $unidadExtra",
                                            colorDiferenciaExtra
                                        )
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    GraficaProgresoScrollable(
                                        valores = valoresExtra,
                                        fechas = fechas,
                                        unidad = unidadExtra,
                                        detallesTooltip = detallesGraficaExtra,
                                        scrollState = scrollStateGrafica,
                                        onScrollStateChange = { isScrollingGrafica = it },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(24.dp))

                                    // -------------------
                                    // GRAFICA 2 (VOLUMEN / CARGA)
                                    // -------------------

                                    Text(
                                        text = if (esCardio) "Carga de cardio (pts)" else "Volumen levantado (kg)",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    val primerVolumen = valoresVolumen.first()
                                    val ultimoVolumen = valoresVolumen.last()
                                    val diferenciaVolumen = ultimoVolumen - primerVolumen
                                    val colorDiferenciaVolumen = when {
                                        diferenciaVolumen > 0 -> Color(0xFF4CAF50)
                                        diferenciaVolumen < 0 -> Color.Red
                                        else -> Color.Gray
                                    }

                                    FilaDato(
                                        "Primer valor registrado:",
                                        "${primerVolumen.formatUiNumber()} $unidadVolumen"
                                    )

                                    FilaDato(
                                        "Último valor registrado:",
                                        "${ultimoVolumen.formatUiNumber()} $unidadVolumen"
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    FilaDato(
                                        "Diferencia:",
                                        "${if (diferenciaVolumen >= 0) "+" else ""}${diferenciaVolumen.formatUiNumber()} $unidadVolumen",
                                        colorDiferenciaVolumen
                                    )

                                    Spacer(Modifier.height(16.dp))

                                    GraficaProgresoScrollable(
                                        valores = valoresVolumen,
                                        fechas = fechas,
                                        unidad = unidadVolumen,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Text("🏆 Records", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(12.dp))

                                if (!esCardio) {

                                    if (records == null) {
                                        Text("Aún no hay estadísticas.")
                                    } else {

                                        pr?.let {
                                            FilaDato("PR:", "${it.pr.formatUiNumber()} kg")
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Mayor volumen en 1 serie:",
                                            "${(records!!.volumenMaxSerie ?: 0f).formatUiNumber()} kg"
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        mejorSesionFuerza?.let {
                                            FilaDato("Mejor sesión:", "${it.mejorSesion.formatUiNumber()} kg")
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Volumen total:",
                                            "${(records!!.volumenTotal ?: 0f).formatUiNumber()} kg"
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
                                                it.carga.formatUiNumber()
                                            )

                                            FilaDato(
                                                "",
                                                "${it.tiempo} min × ${it.intensidad} intensidad"
                                            )
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        mejorSesionCardio?.let {
                                            FilaDato("Mejor sesión", it.mejorSesion.formatUiNumber())
                                        }

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Mejor carga en 1 serie:",
                                            "${recordsCardio!!.mejorTiempo ?: 0} pts"
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        FilaDato(
                                            "Carga total:",
                                            "${recordsCardio!!.tiempoTotal ?: 0} pts"
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

            }

            // ========== MENÚ FLOTANTE ==========
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
            ) {
                FloatingActionButton(
                    onClick = { showSectionsMenu = !showSectionsMenu }
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Abrir índice")
                }

                DropdownMenu(
                    expanded = showSectionsMenu,
                    onDismissRequest = { showSectionsMenu = false }
                ) {
                    sections.forEach { section ->
                        val isSelected = section == currentSection
                        val sectionLabel = when (section) {
                            "Músculo" -> "💪 Músculo"
                            "Última sesión" -> "⏱️ Última sesión"
                            "Comentarios" -> "💬 Comentarios"
                            "Actividad" -> "📈 Actividad"
                            "Records" -> "🏆 Records"
                            else -> section
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sectionLabel,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                coroutineScope.launch {
                                    val index = sectionToIndex(section)
                                    listState.animateScrollToItem(index)
                                    showSectionsMenu = false
                                }
                            }
                        )
                    }
                }
            }
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
                    mostrarErrorNombreEjercicioDuplicado = false
                    showRenameDialog = true
                },
                onDelete = {
                    showSettings = false
                    showDeleteDialog = true
                },
                onEditMusculos = {
                    showSettings = false
                    musculoSeleccionado = ejercicio?.musculos?.firstOrNull()
                    mostrarErrorMusculo = false
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
            text = {
                Column {
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = {
                            nuevoNombre = it
                            mostrarErrorNombreEjercicioDuplicado = false
                        },
                        label = { Text("Nombre del ejercicio") },
                        singleLine = true
                    )

                    if (mostrarErrorNombreEjercicioDuplicado) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ya existe un ejercicio con ese nombre. Cambialo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renombrarEjercicio(ejercicioId, nuevoNombre) { resultado ->
                            when (resultado) {
                                EjercicioViewModel.NombreOperacionResultado.OK -> {
                                    showRenameDialog = false
                                    mostrarErrorNombreEjercicioDuplicado = false
                                }
                                EjercicioViewModel.NombreOperacionResultado.DUPLICADO -> {
                                    mostrarErrorNombreEjercicioDuplicado = true
                                }
                                EjercicioViewModel.NombreOperacionResultado.VACIO -> {
                                    mostrarErrorNombreEjercicioDuplicado = false
                                }
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") } }
        )
    }

    // ----------------------------
    // Diálogo editar músculos
    // ----------------------------
    if (showEditMusculosDialog) {
        AlertDialog(
            onDismissRequest = { showEditMusculosDialog = false },
            title = { Text("Cambiar músculo") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (mostrarErrorMusculo) {
                        Text(
                            text = "Debes seleccionar un músculo",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Musculo.entries.forEach { musculo ->
                            FilterChip(
                                selected = musculoSeleccionado == musculo,
                                onClick = {
                                    musculoSeleccionado = if (musculoSeleccionado == musculo) null else musculo
                                    mostrarErrorMusculo = false
                                },
                                label = { Text(musculo.labelWithEmoji()) },
                                leadingIcon = if (musculoSeleccionado == musculo) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val seleccionado = musculoSeleccionado
                    if (seleccionado == null) {
                        mostrarErrorMusculo = true
                    } else {
                        viewModel.actualizarMusculos(ejercicioId, listOf(seleccionado))
                        showEditMusculosDialog = false
                    }
                }) {
                    Text("Guardar")
                }
            },
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