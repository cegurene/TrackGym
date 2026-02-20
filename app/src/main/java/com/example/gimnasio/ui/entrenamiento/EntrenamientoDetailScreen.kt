package com.example.gimnasio.ui.entrenamiento

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.entity.SerieEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientoDetailScreen(
    entrenamientoId: Long,
    onBack: () -> Unit,
    onNavigateToEntrenamiento: (Long) -> Unit,
    onNavigateToEjercicio: (Long) -> Unit
) {

    val context = LocalContext.current

    val viewModel: EntrenamientoDetailViewModel = viewModel(
        factory = EntrenamientoDetailViewModelFactory(context)
    )

    val entrenamiento by viewModel
        .getEntrenamientoCompleto(entrenamientoId)
        .collectAsState(initial = null)

    val entrenamientoConRutina by viewModel
        .getEntrenamiento(entrenamientoId)
        .collectAsState(initial = null)

    val entrenamientoActivo by viewModel
        .entrenamientoActivo
        .collectAsState(initial = null)

    var showSettings by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
                    }
                }

            )
        }
    ) { padding ->

        if (entrenamiento == null || entrenamientoConRutina == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando...")
            }
        } else {

            val data = entrenamiento!!
            val rutina = entrenamientoConRutina!!.rutina

            fun calcularVolumen(series: List<SerieEntity>): Int {
                return series.sumOf { (it.peso * it.repeticiones).toInt() }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = rutina.nombre,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Button(
                            onClick = {
                                viewModel.repetirEntrenamiento(data) { nuevoId ->
                                    onNavigateToEntrenamiento(nuevoId)
                                }
                            },
                            enabled = entrenamientoActivo == null
                        ) {
                            Text("Repetir")
                        }
                    }
                }

                items(data.ejercicios) { ejercicioConSeries ->

                    val volumen =
                        calcularVolumen(ejercicioConSeries.series)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        onClick = {
                            onNavigateToEjercicio(ejercicioConSeries.ejercicio.id)
                        }
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = ejercicioConSeries.ejercicio.nombre,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ejercicioConSeries.series.forEachIndexed { index, serie ->

                                Text(
                                    text = "Serie ${index + 1}: " +
                                            "${serie.peso}kg x ${serie.repeticiones}"
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Volumen total: $volumen kg",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            EntrenamientoSettingsSheet(
                onRename = {
                    showSettings = false
                    nuevoNombre = entrenamiento?.entrenamiento?.nombre ?: ""
                    showRenameDialog = true
                }
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Cambiar nombre") },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nombre del entrenamiento") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renombrarEntrenamiento(
                            entrenamientoId,
                            nuevoNombre
                        )
                        showRenameDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

}
