package com.example.gimnasio.ui.historico

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    onEntrenamientoClick: (Long) -> Unit
) {

    val context = LocalContext.current

    val viewModel: HistoricoViewModel = viewModel(
        factory = HistoricoViewModelFactory(context)
    )

    val entrenamientosTotales by viewModel.entrenamientos
        .collectAsState(initial = emptyList())

    val entrenamientos by viewModel.entrenamientosFiltrados
        .collectAsState(initial = emptyList())

    val rutinas by viewModel.rutinas.collectAsState()
    val selectedRutinaId by viewModel.selectedRutinaId.collectAsState()
    val rutinaSeleccionada = remember(rutinas, selectedRutinaId) {
        rutinas.firstOrNull { it.id == selectedRutinaId }
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 Histórico") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            FilledTonalButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("🧩", modifier = Modifier.padding(end = 8.dp))
                if (selectedRutinaId == null) {
                    Text("Filtrar por rutina")
                } else {
                    Text("Filtrar (1)")
                }
            }

            AnimatedVisibility(
                visible = selectedRutinaId != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.limpiarFiltroRutina() },
                            label = { Text(rutinaSeleccionada?.nombre ?: "Rutina") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar filtro"
                                )
                            }
                        )
                    }
                }
            }

            when {
                entrenamientosTotales.isEmpty() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Aún no has completado entrenamientos.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                entrenamientos.isEmpty() -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Aún no hay entrenamientos con ${rutinaSeleccionada?.nombre ?: "esta rutina"}.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(entrenamientos) { index, item ->
                            HistoricoItem(
                                item = item,
                                onClick = {
                                    onEntrenamientoClick(item.entrenamiento.id)
                                },
                                numero = entrenamientos.size - index
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Filtrar por rutina",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(rutinas) { _, rutina ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            FilterChip(
                                selected = selectedRutinaId == rutina.id,
                                onClick = {
                                    if (selectedRutinaId == rutina.id) {
                                        viewModel.limpiarFiltroRutina()
                                    } else {
                                        viewModel.seleccionarRutina(rutina.id)
                                    }
                                },
                                label = { Text(rutina.nombre) },
                                leadingIcon = if (selectedRutinaId == rutina.id) {
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { viewModel.limpiarFiltroRutina() }) {
                        Text("Limpiar")
                    }
                    Button(onClick = { showFilterSheet = false }) {
                        Text("Aplicar")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}