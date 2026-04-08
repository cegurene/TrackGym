package com.example.gimnasio.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gimnasio.ui.ejercicios.EjercicioDetailScreen
import com.example.gimnasio.ui.historico.HistoricoDetailScreen
import com.example.gimnasio.ui.rutinas.RutinaDetailScreen
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit
) {
    var selectedRutinaId by remember { mutableStateOf<Long?>(null) }
    var selectedEjercicioId by remember { mutableStateOf<Long?>(null) }
    var selectedEntrenamientoId by remember { mutableStateOf<Long?>(null) }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Rutinas", "Ejercicios", "Histórico", "Estadísticas")

    val shouldHandleBack =
        (pagerState.currentPage == 0 && selectedRutinaId != null) ||
            (pagerState.currentPage == 1 && selectedEjercicioId != null) ||
            (pagerState.currentPage == 2 && selectedEntrenamientoId != null) ||
            pagerState.currentPage != 0

    BackHandler(enabled = shouldHandleBack) {
        when (pagerState.currentPage) {
            0 -> selectedRutinaId = null
            1 -> {
                if (selectedEjercicioId != null) {
                    selectedEjercicioId = null
                } else {
                    scope.launch { pagerState.animateScrollToPage(0) }
                }
            }
            2 -> {
                if (selectedEntrenamientoId != null) {
                    selectedEntrenamientoId = null
                } else {
                    scope.launch { pagerState.animateScrollToPage(0) }
                }
            }
            3 -> scope.launch { pagerState.animateScrollToPage(0) }
            else -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 40.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                maxLines = 1,
                                //fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes"
                    )
                }
            }
        }

        var isScrollingGrafica by remember { mutableStateOf(false) }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isScrollingGrafica,
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
                        onOpenSettings = {},
                        onGraficaScrollChange = { isScrollingGrafica = it }
                    )
                }
                2 -> if (selectedEntrenamientoId == null) {
                    HistoricoTab { selectedEntrenamientoId = it }
                } else {
                    HistoricoDetailScreen(
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
