package com.example.gimnasio.ui.historico

import android.app.Application
import android.app.DatePickerDialog
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
import com.example.gimnasio.ui.rutinas.RutinaViewModel
import com.example.gimnasio.ui.rutinas.RutinaViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    onEntrenamientoClick: (Long) -> Unit
) {

    val context = LocalContext.current

    val viewModel: HistoricoViewModel = viewModel(
        factory = HistoricoViewModelFactory(context)
    )

    // Obtener RutinaViewModel para sincronizar el ordenamiento
    val rutinaViewModel: RutinaViewModel = viewModel(
        factory = RutinaViewModelFactory(context.applicationContext as Application)
    )
    val rutinaOrder by rutinaViewModel.order.collectAsState()

    // Sincronizar el estado de orden con HistoricoViewModel
    LaunchedEffect(rutinaOrder) {
        viewModel.setRutinaOrder(rutinaOrder)
    }

    val entrenamientosTotales by viewModel.entrenamientos
        .collectAsState(initial = emptyList())

    val entrenamientos by viewModel.entrenamientosFiltrados
        .collectAsState(initial = emptyList())

    val rutinas by viewModel.rutinas.collectAsState()
    val selectedRutinaId by viewModel.selectedRutinaId.collectAsState()
    val selectedFechaDesde by viewModel.selectedFechaDesde.collectAsState()
    val selectedFechaHasta by viewModel.selectedFechaHasta.collectAsState()
    val rutinaSeleccionada = remember(rutinas, selectedRutinaId) {
        rutinas.firstOrNull { it.id == selectedRutinaId }
    }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val filtrosActivos = (if (selectedRutinaId != null) 1 else 0) +
        (if (selectedFechaDesde != null) 1 else 0) +
        (if (selectedFechaHasta != null) 1 else 0)

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
                if (filtrosActivos == 0) {
                    Text("Filtrar por rutina y fecha")
                } else {
                    Text("Filtrar ($filtrosActivos)")
                }
            }

            AnimatedVisibility(
                visible = filtrosActivos > 0,
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
                        if (selectedRutinaId != null) {
                            AssistChip(
                                onClick = { viewModel.limpiarFiltroRutina() },
                                label = { Text(rutinaSeleccionada?.nombre ?: "Rutina") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar filtro rutina"
                                    )
                                }
                            )
                        }

                        if (selectedFechaDesde != null) {
                            AssistChip(
                                onClick = { viewModel.seleccionarFechaDesde(null) },
                                label = { Text("Desde ${dateFormatter.format(Date(selectedFechaDesde!!))}") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar fecha desde"
                                    )
                                }
                            )
                        }

                        if (selectedFechaHasta != null) {
                            AssistChip(
                                onClick = { viewModel.seleccionarFechaHasta(null) },
                                label = { Text("Hasta ${dateFormatter.format(Date(selectedFechaHasta!!))}") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar fecha hasta"
                                    )
                                }
                            )
                        }
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
                            text = "Aún no hay entrenamientos con los filtros seleccionados.",
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
                    text = "Filtrar histórico",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rutina",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
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

                Text(
                    text = "Fecha",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            context.showDatePickerDialog(selectedFechaDesde) { millis ->
                                viewModel.seleccionarFechaDesde(millis)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (selectedFechaDesde == null) {
                                "Desde"
                            } else {
                                "Desde ${dateFormatter.format(Date(selectedFechaDesde!!))}"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            context.showDatePickerDialog(selectedFechaHasta) { millis ->
                                viewModel.seleccionarFechaHasta(millis)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (selectedFechaHasta == null) {
                                "Hasta"
                            } else {
                                "Hasta ${dateFormatter.format(Date(selectedFechaHasta!!))}"
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        viewModel.limpiarFiltroRutina()
                        viewModel.limpiarFiltroFechas()
                    }) {
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

private fun android.content.Context.showDatePickerDialog(
    initialMillis: Long?,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = initialMillis ?: System.currentTimeMillis()
    }

    DatePickerDialog(
        this,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selected.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

