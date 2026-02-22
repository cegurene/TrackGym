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

    val stats by viewModel.distribucionMusculos.collectAsState()
    val resumen by viewModel.resumenGeneral.collectAsState()
    val records by viewModel.records.collectAsState()
    val volumenPorMusculo by viewModel.volumenPorMusculo.collectAsState()

    val totalEjercicios = stats.values.sum()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val sections = listOf("Resumen", "Distribución", "Volumen", "Récords")
    var currentSection by remember { mutableStateOf("Resumen") }

    // Detectar sección activa automáticamente
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                currentSection = when {
                    index == 0 -> "Resumen"
                    index in 1..stats.size -> "Distribución"
                    index == stats.size + 1 -> "Volumen"
                    else -> "Récords"
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
                                            "Resumen" -> 0
                                            "Distribución" -> 1
                                            "Volumen" -> stats.size + 1
                                            "Récords" -> stats.size + 2
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
                // 1️⃣ RESUMEN GENERAL
                // -------------------
                item {
                    Text("Resumen", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total ejercicios: ${resumen.totalEjercicios}")
                    Text("Total rutinas: ${resumen.totalRutinas}")
                    Text("Total entrenamientos: ${resumen.totalEntrenamientos}")
                    resumen.musculoMasEntrenado?.let {
                        Text("Músculo más trabajado: $it")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // -------------------
                // 2️⃣ DISTRIBUCIÓN POR MÚSCULO
                // -------------------
                item {
                    Text("Distribución por músculo", style = MaterialTheme.typography.titleLarge)
                    //Spacer(modifier = Modifier.height(20.dp))
                }

                items(stats.toList().sortedByDescending { it.second }) { (musculo, cantidad) ->
                    val porcentaje = if (totalEjercicios > 0) (cantidad * 100) / totalEjercicios else 0

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("${musculo.name} • $cantidad ejercicios • $porcentaje%")
                        LinearProgressIndicator(
                            progress = porcentaje / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // -------------------
                // 3️⃣ VOLUMEN POR MÚSCULO
                // -------------------
                item {
                    Text(
                        "Volumen por músculo",
                        style = MaterialTheme.typography.titleLarge
                    )
                    //Spacer(modifier = Modifier.height(8.dp))
                }

                val listaOrdenada = volumenPorMusculo
                    .toList()
                    .sortedByDescending { it.second }

                val maxVolumen = listaOrdenada.maxOfOrNull { it.second } ?: 1.0

                items(listaOrdenada) { (musculo, volumen) ->

                    val progreso = (volumen / maxVolumen).toFloat().coerceIn(0f, 1f)
                    val progresoAnimado by animateFloatAsState(
                        targetValue = progreso,
                        label = ""
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        onClick = { /* opcional */ }
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {

                            // 🔵 Barra de fondo proporcional
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progresoAnimado)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    )
                            )

                            // 🔤 Contenido encima
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (volumen == maxVolumen)
                                        "🔥 ${musculo.name}"
                                    else musculo.name,
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


                // -------------------
                // 4️⃣ RÉCORDS PERSONALES
                // -------------------
                item {
                    Text("Récords personales", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    records.diaMasVolumen?.let {
                        val fechaSolo = it.substringBefore("T")
                        val volumenDia = records.volumenDiaMasVolumen ?: 0.0

                        Text("Día con más volumen: $fechaSolo")
                        Text("Volumen total ese día: ${String.format("%.1f", volumenDia)} kg")
                    }

                    records.serieMasVolumen?.let { record ->
                        Text(
                            "Serie con más volumen: " +
                                    "${String.format("%.1f", record.volumen)} kg\n" +
                                    "Ejercicio: ${record.nombreEjercicio}\n" +
                                    "Músculo: ${record.musculo}"
                        )
                    }

                    records.entrenamientoMasLargo?.let { Text("Entrenamiento más largo: $it ") }
                    records.entrenamientoMasCorto?.let { Text("Entrenamiento más corto: $it ") }

                    Spacer(modifier = Modifier.height(16.dp))
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