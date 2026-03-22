package com.example.gimnasio.ui.estadisticas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: String = "",
    compactTitle: Boolean = false,
    modifier: Modifier = Modifier
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                    Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
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
    val progress = (value / total).toFloat().coerceIn(0f, 1f)
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
                text = "${String.format("%.0f", value)}$valueSuffix",
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
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isMax) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val totalEjercicios = remember(stats) {
        stats.values.sum()
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val sections = listOf(
        "Actividad",
        "Tiempo",
        "Ejercicios",
        "Volumen",
        "Rutinas"
    )
    var currentSection by remember { mutableStateOf("Actividad") }

    val actividad by viewModel.actividad.collectAsState()
    val tiempo by viewModel.tiempo.collectAsState()
    val rutinasFrecuencia by viewModel.rutinasFrecuencia.collectAsState()

    val tiempoCardioTotal by viewModel.tiempoCardioTotal.collectAsState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                currentSection = when {
                    index == 0 -> "Actividad"
                    index == 1 -> "Tiempo"
                    index == 2 -> "Ejercicios"
                    index in 3..(stats.size + 2) -> "Volumen"
                    else -> "Rutinas"
                }
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Secciones", style = MaterialTheme.typography.titleLarge)
                    }

                    items(sections) { section ->
                        val isSelected = section == currentSection
                        Text(
                            text = section,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        val index = when (section) {
                                            "Actividad" -> 0
                                            "Tiempo" -> 1
                                            "Ejercicios" -> 2
                                            "Volumen" -> stats.size + 2
                                            "Rutinas" -> stats.size + 3
                                            else -> 0
                                        }
                                        listState.animateScrollToItem(index)
                                        drawerState.close()
                                    }
                                }
                                .padding(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    ) {

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ACTIVIDAD
                item {
                    Column {
                        Text(
                            "📊 Actividad",
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
                                icon = "💪",
                                compactTitle = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // TIEMPO
                item {
                    Column {
                        Text(
                            "⏱️ Tiempo Entrenado",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
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
                                    value = tiempo.tiempoTotal,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                StatCard(
                                    title = "Duración media",
                                    value = tiempo.duracionMedia,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                records.entrenamientoMasLargo?.let {
                                    StatCard(
                                        title = "Más largo",
                                        value = it,
                                        icon = "🏃",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                records.entrenamientoMasCorto?.let {
                                    StatCard(
                                        title = "Más corto",
                                        value = it,
                                        icon = "⚡",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                StatCard(
                                    title = "Cardio total",
                                    value = tiempoCardioTotal,
                                    icon = "🏃",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // EJERCICIOS
                item {
                    Column {
                        Text(
                            "💪 Distribución de Ejercicios",
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
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                val statsOrdenados = remember(stats) {
                                    stats.toList().sortedByDescending { it.second }
                                }

                                statsOrdenados.forEach { (musculo, cantidad) ->
                                    val porcentaje = if (totalEjercicios > 0) 
                                        (cantidad * 100) / totalEjercicios else 0

                                    ProgressBarItem(
                                        label = musculo.name
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        value = cantidad.toFloat(),
                                        total = totalEjercicios.toFloat(),
                                        isMax = cantidad == statsOrdenados.first().second
                                    )
                                }
                            }
                        }
                    }
                }

                // VOLUMEN
                item {
                    Column {
                        Text(
                            "🏋️ Volumen por Músculo",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
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
                                        "Sin datos de volumen aún",
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

                // RUTINAS
                item {
                    Column {
                        Text(
                            "📋 Rutinas",
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
                            elevation = CardDefaults.cardElevation(2.dp),
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
                                        "Sin rutinas aún",
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

            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        drawerState.open()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir índice")
            }
        }
    }
}