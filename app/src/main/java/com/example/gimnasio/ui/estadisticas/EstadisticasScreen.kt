package com.example.gimnasio.ui.estadisticas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.ejercicios.EjercicioViewModel
import com.example.gimnasio.ui.ejercicios.EjercicioViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    val rutinas by viewModel.rutinas.collectAsState()

    val rutinasFrecuencia by viewModel.rutinasFrecuencia.collectAsState()

    val tiempoCardioTotal by viewModel.tiempoCardioTotal.collectAsState()

    // Detectar sección activa automáticamente
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
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Índice", style = MaterialTheme.typography.titleLarge)
                    }

                    items(sections) { section ->
                        val isSelected = section == currentSection
                        Text(
                            text = section,
                            modifier = Modifier
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) {

        Box(modifier = Modifier.fillMaxSize()) {

            // -------------------
            // CONTENIDO PRINCIPAL
            // -------------------
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // -------------------
                //     ACTIVIDAD
                // -------------------
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Text("📈 Actividad", style = MaterialTheme.typography.titleLarge)

                            Spacer(Modifier.height(12.dp))

                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Entrenamientos esta semana")
                                    Text("${actividad.entrenamientosSemana}")
                                }
                            }

                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Entrenamientos este mes")
                                    Text("${actividad.entrenamientosMes}")
                                }
                            }

                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Total de entrenamientos")
                                    Text("${resumen.totalEntrenamientos}")
                                }
                            }
                        }
                    }
                }

                // -------------------
                //       TIEMPO
                // -------------------
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Text("⏱ Tiempo entrenado", style = MaterialTheme.typography.titleLarge)

                            Spacer(Modifier.height(12.dp))

                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Tiempo total entrenado")
                                    Text(tiempo.tiempoTotal)
                                }
                            }

                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Duración media")
                                    Text(tiempo.duracionMedia)
                                }
                            }

                            records.entrenamientoMasLargo?.let {
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Entrenamiento más largo")
                                        Text(it)
                                    }
                                }
                            }

                            records.entrenamientoMasCorto?.let {
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Entrenamiento más corto")
                                        Text(it)
                                    }
                                }
                            }

                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Tiempo total de cardio")
                                    Text(tiempoCardioTotal)
                                }
                            }
                        }
                    }
                }

                // -------------------
                // EJERCICIOS
                // -------------------
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Text("💪 Ejercicios", style = MaterialTheme.typography.titleLarge)
                            Text("Total ejercicios creados: ${resumen.totalEjercicios}")
                            Spacer(Modifier.height(12.dp))


                            val statsOrdenados = remember(stats) {
                                stats.toList().sortedByDescending { it.second }
                            }

                            statsOrdenados.forEach { (musculo, cantidad) ->

                                val porcentaje =
                                    if (totalEjercicios > 0) (cantidad * 100) / totalEjercicios else 0

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .padding(horizontal = 12.dp)
                                ) {

                                    Text("${musculo.name} • $cantidad ejercicios • $porcentaje%")

                                    LinearProgressIndicator(
                                        progress = porcentaje / 100f,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------
                // VOLUMEN POR MÚSCULO
                // -------------------
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🏋️ Volumen por músculo", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))

                            val volumenOrdenado = remember(volumenPorMusculo) {
                                volumenPorMusculo.toList().sortedByDescending { it.second }
                            }

                            val maxVolumen = remember(volumenOrdenado) {
                                volumenOrdenado.maxOfOrNull { it.second } ?: 1.0
                            }

                            volumenOrdenado.forEach { (musculo, volumen) ->
                                val progreso = (volumen / maxVolumen).toFloat().coerceIn(0f, 1f)
                                val progresoAnimado by animateFloatAsState(targetValue = progreso, label = "")

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progresoAnimado)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                )
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (volumen == maxVolumen) "🔥 ${musculo.name}" else musculo.name,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "${String.format("%.1f", volumen)} kg",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------
                //      RUTINAS
                // -------------------
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {

                        Column(Modifier.padding(16.dp)) {

                            Text("📋 Rutinas", style = MaterialTheme.typography.titleLarge)
                            Text("Total rutinas creadas: ${resumen.totalRutinas}")
                            Spacer(Modifier.height(12.dp))

                            val rutinasOrdenadas = remember(rutinasFrecuencia) {
                                rutinasFrecuencia.sortedByDescending { it.veces }
                            }

                            val maxVeces = remember(rutinasOrdenadas) {
                                rutinasOrdenadas.maxOfOrNull { it.veces } ?: 1
                            }

                            rutinasOrdenadas.forEach { rutina ->

                                val progreso = rutina.veces.toFloat() / maxVeces
                                val progresoAnimado by animateFloatAsState(
                                    targetValue = progreso,
                                    label = ""
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .padding(horizontal = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progresoAnimado)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                )
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (rutina.veces == maxVeces) "🔥 ${rutina.nombre}" else rutina.nombre,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text("${rutina.veces} veces", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

            // -------------------
            // BOTÓN FLOTANTE ESTILO iPad
            // -------------------
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