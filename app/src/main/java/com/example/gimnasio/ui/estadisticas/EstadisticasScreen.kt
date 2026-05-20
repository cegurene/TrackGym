package com.example.gimnasio.ui.estadisticas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

@Composable
fun DuracionCampanaGauss(
    duraciones: List<Double>,
    modifier: Modifier = Modifier
) {
    if (duraciones.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Se necesitan al menos 2 entrenamientos para mostrar la distribución",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val mean = duraciones.average()
    val stdDev = sqrt(duraciones.map { (it - mean).pow(2) }.average()).coerceAtLeast(1.0)

    val minDur = duraciones.minOrNull() ?: 0.0
    val maxDur = duraciones.maxOrNull() ?: 100.0
    
    // Extendemos el rango un poco para que la campana se vea completa
    val rangeMin = (minDur - stdDev).coerceAtLeast(0.0)
    val rangeMax = maxDur + stdDev
    val range = rangeMax - rangeMin

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    Column(modifier = modifier) {
        Text(
            text = "Distribución de duración",
            style = MaterialTheme.typography.labelMedium,
            color = onSurfaceColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val width = size.width
            val labelHeight = 20.dp.toPx()
            val graphHeight = size.height - labelHeight
            val steps = 100
            
            val path = Path()
            val fillPath = Path()

            // Función de densidad de probabilidad Gaussiana
            fun g(x: Double): Double {
                return (1.0 / (stdDev * sqrt(2.0 * PI))) * exp(-0.5 * ((x - mean) / stdDev).pow(2.0))
            }

            // Encontramos el valor máximo de g(x) para escalar la gráfica
            val maxG = g(mean)
            
            for (i in 0..steps) {
                val xVal = rangeMin + (i.toDouble() / steps) * range
                val yVal = g(xVal)
                
                val xPos = (i.toDouble() / steps) * width
                val yPos = graphHeight - (yVal / maxG).toFloat() * graphHeight

                if (i == 0) {
                    path.moveTo(xPos.toFloat(), yPos)
                    fillPath.moveTo(xPos.toFloat(), graphHeight)
                    fillPath.lineTo(xPos.toFloat(), yPos)
                } else {
                    path.lineTo(xPos.toFloat(), yPos)
                    fillPath.lineTo(xPos.toFloat(), yPos)
                }
                
                if (i == steps) {
                    fillPath.lineTo(xPos.toFloat(), graphHeight)
                    fillPath.close()
                }
            }

            // Dibujar el área rellena
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.4f),
                        primaryColor.copy(alpha = 0.05f)
                    )
                )
            )

            // Dibujar la línea de la campana
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Eje X (Línea base)
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.3f),
                start = Offset(0f, graphHeight),
                end = Offset(width, graphHeight),
                strokeWidth = 1.dp.toPx()
            )

            // Etiquetas de tiempo
            val paint = android.graphics.Paint().apply {
                color = onSurfaceColor.toArgb()
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // Dibujar 3 etiquetas: min, media, max
            val labels = listOf(rangeMin, mean, rangeMax)
            labels.forEach { valTime ->
                val xPos = ((valTime - rangeMin) / range) * width
                drawContext.canvas.nativeCanvas.drawText(
                    "${valTime.toInt()}m",
                    xPos.toFloat(),
                    size.height - 5.dp.toPx(),
                    paint
                )
            }
        }
    }
}

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun ProgressBarItem(
    label: String,
    value: Float,
    total: Float,
    isMax: Boolean = false,
    icon: String = "",
    valueSuffix: String = ""
) {
    val progress = if (total > 0f) (value / total).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon.isNotEmpty()) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${String.format(Locale.getDefault(), "%.0f", value)}$valueSuffix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isMax) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
            )
        }
    }
}

private fun sectionToIndex(section: String, statsSize: Int): Int {
    return when (section) {
        "Actividad" -> 0
        "Tiempo" -> 1
        "Ejercicios" -> 2
        "Volumen" -> 3
        "Rutinas" -> 4
        else -> 0
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun EstadisticasScreen(
    onEstadisticasClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }

    val viewModel: EstadisticasViewModel = viewModel(
        factory = EstadisticasViewModelFactory(database)
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStats()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val stats by viewModel.distribucionMusculos.collectAsState()
    val resumen by viewModel.resumenGeneral.collectAsState()
    val records by viewModel.records.collectAsState()
    val volumenPorMusculo by viewModel.volumenPorMusculo.collectAsState()
    val actividad by viewModel.actividad.collectAsState()
    val tiempo by viewModel.tiempo.collectAsState()
    val rutinasFrecuencia by viewModel.rutinasFrecuencia.collectAsState()
    val tiempoCardioTotal by viewModel.tiempoCardioTotal.collectAsState()
    val duracionesEntrenamientos by viewModel.duracionesEntrenamientos.collectAsState()

    val totalEjercicios = remember(stats) { stats.values.sum() }

    val sections = listOf("Actividad", "Tiempo", "Ejercicios", "Volumen", "Rutinas")
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showSectionsMenu by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf("Actividad") }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index ->
                when {
                    index == 0 -> "Actividad"
                    index == 1 -> "Tiempo"
                    index == 2 -> "Ejercicios"
                    index == 3 -> "Volumen"
                    else -> "Rutinas"
                }
            }
            .collectLatest { currentSection = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "📊 Actividad",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Semana",
                            value = "${actividad.entrenamientosSemana}",
                            icon = "🔥",
                            compactTitle = true,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Mes",
                            value = "${actividad.entrenamientosMes}",
                            icon = "📅",
                            compactTitle = true,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total",
                            value = "${resumen.totalEntrenamientos}",
                            icon = "🏁",
                            compactTitle = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "entrenamientos",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            item {
                Column {
                    Text(
                        text = "⏱️ Tiempo Entrenado",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Tiempo total",
                                value = tiempo.tiempoTotal
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            StatCard(
                                title = "Duración media",
                                value = tiempo.duracionMedia
                            )

                            records.entrenamientoMasLargo?.let {
                                StatCard(
                                    title = "Más largo",
                                    value = it,
                                    icon = "⏳"
                                )
                            }

                            records.entrenamientoMasCorto?.let {
                                StatCard(
                                    title = "Más corto",
                                    value = it,
                                    icon = "⚡"
                                )
                            }

                            StatCard(
                                title = "Cardio total",
                                value = tiempoCardioTotal,
                                icon = "❤️"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            DuracionCampanaGauss(
                                duraciones = duracionesEntrenamientos,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "💪 Distribución de Ejercicios",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    StatCard(
                        title = "Total creados",
                        value = "${resumen.totalEjercicios}",
                        icon = "📋"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val statsOrdenados = remember(stats) {
                                stats.toList().sortedByDescending { it.second }
                            }

                            statsOrdenados.forEach { (musculo, cantidad) ->
                                ProgressBarItem(
                                    label = musculo.name
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    value = cantidad.toFloat(),
                                    total = totalEjercicios.toFloat(),
                                    isMax = cantidad == statsOrdenados.firstOrNull()?.second
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "🏋️ Volumen por Músculo",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val volumenOrdenado = remember(volumenPorMusculo) {
                                volumenPorMusculo
                                    .filterKeys { it != Musculo.CARDIO }
                                    .toList()
                                    .sortedByDescending { it.second }
                            }

                            val maxVolumen = remember(volumenOrdenado) {
                                volumenOrdenado.maxOfOrNull { it.second } ?: 1.0
                            }

                            if (volumenOrdenado.isEmpty()) {
                                Text(
                                    text = "Sin datos de volumen aún",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                volumenOrdenado.forEach { (musculo, volumen) ->
                                    ProgressBarItem(
                                        label = musculo.name
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        value = volumen.toFloat(),
                                        total = maxVolumen.toFloat(),
                                        isMax = volumen == maxVolumen,
                                        icon = if (volumen == maxVolumen) "🔥" else "",
                                        valueSuffix = " kg"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "🗂️ Rutinas",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    StatCard(
                        title = "Total rutinas",
                        value = "${resumen.totalRutinas}",
                        icon = "📚"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val rutinasOrdenadas = remember(rutinasFrecuencia) {
                                rutinasFrecuencia.sortedByDescending { it.veces }
                            }

                            val maxVeces = remember(rutinasOrdenadas) {
                                rutinasOrdenadas.maxOfOrNull { it.veces } ?: 1
                            }

                            if (rutinasOrdenadas.isEmpty()) {
                                Text(
                                    text = "Sin rutinas aún",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                rutinasOrdenadas.forEach { rutina ->
                                    ProgressBarItem(
                                        label = rutina.nombre,
                                        value = rutina.veces.toFloat(),
                                        total = maxVeces.toFloat(),
                                        isMax = rutina.veces == maxVeces,
                                        icon = if (rutina.veces == maxVeces) "🔥" else "",
                                        valueSuffix = " veces"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

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
                        "Actividad" -> "📊 Actividad"
                        "Tiempo" -> "⏱️ Tiempo"
                        "Ejercicios" -> "💪 Ejercicios"
                        "Volumen" -> "🏋️ Volumen"
                        "Rutinas" -> "🗂️ Rutinas"
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
                                val index = sectionToIndex(section, stats.size)
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
