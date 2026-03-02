package com.example.gimnasio.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.gimnasio.ui.ejercicios.EjercicioDetailScreen
import com.example.gimnasio.ui.entrenamiento.EntrenamientoDetailScreen
import com.example.gimnasio.ui.rutinas.RutinaDetailScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    var selectedRutinaId by remember { mutableStateOf<Long?>(null) }
    var selectedEjercicioId by remember { mutableStateOf<Long?>(null) }
    var selectedEntrenamientoId by remember { mutableStateOf<Long?>(null) }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Rutinas", "Ejercicios", "Histórico", "Estadísticas")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title, maxLines = 1) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> if (selectedRutinaId == null) {
                    RutinasTab { selectedRutinaId = it }
                } else {
                    RutinaDetailScreen(
                        rutinaId = selectedRutinaId!!,
                        onBack = { selectedRutinaId = null },
                        onStartEntrenamiento = { entrenamientoId ->
                            selectedEntrenamientoId = entrenamientoId
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onNavigateToEjercicio = { ejercicioId ->
                            selectedEjercicioId = ejercicioId
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                }
                1 -> if (selectedEjercicioId == null) {
                    EjerciciosTab { selectedEjercicioId = it }
                } else {
                    EjercicioDetailScreen(
                        ejercicioId = selectedEjercicioId!!,
                        onBack = { selectedEjercicioId = null },
                        onOpenSettings = {}
                    )
                }
                2 -> if (selectedEntrenamientoId == null) {
                    HistoricoTab { selectedEntrenamientoId = it }
                } else {
                    EntrenamientoDetailScreen(
                        entrenamientoId = selectedEntrenamientoId!!,
                        onBack = { selectedEntrenamientoId = null },
                        onNavigateToEntrenamiento = { entrenamientoId ->
                            selectedEntrenamientoId = entrenamientoId
                        },
                        onNavigateToEjercicio = { ejercicioId ->
                            selectedEjercicioId = ejercicioId
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                }
                3 -> EstadisticasTab(
                    onEstadisticasClick = {}
                )
            }
        }
    }
}
