package com.example.gimnasio.ui.rutinas

import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.entity.EjercicioEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinaDetailScreen(
    rutinaId: Long,
    onBack: () -> Unit,
    onStartEntrenamiento: (Long) -> Unit,
    onNavigateToEjercicio: (Long) -> Unit
) {
    val context = LocalContext.current
    val viewModel: RutinaViewModel = viewModel(
        factory = RutinaViewModelFactory(context.applicationContext as android.app.Application)
    )

    // Solo obtenemos la rutina (para nombre, etc.)
    val rutinaConEjercicios by viewModel
        .getRutinaConEjercicios(rutinaId)
        .collectAsState(initial = null)

    val rutina = rutinaConEjercicios?.rutina

    // 🔥 Ahora los ejercicios vienen ordenados
    val ejercicios by viewModel
        .getEjerciciosConOrden(rutinaId)
        .collectAsState(initial = emptyList())


    var showSettings by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var showAddDialog by remember { mutableStateOf(false) }
    val todosLosEjercicios by viewModel
        .getAllEjercicios()
        .collectAsState(initial = emptyList())

    var ejercicioAEliminar by remember { mutableStateOf<EjercicioEntity?>(null) }

    val entrenamientoActivo by viewModel
        .entrenamientoActivo
        .collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(rutina?.nombre ?: "Rutina")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir ejercicio")
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            if (rutinaConEjercicios == null) {
                // Mientras carga
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🗂️ Detalle de rutina",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${ejercicios.size} ejercicios",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = {
                                    viewModel.iniciarEntrenamiento(rutinaId) { entrenamientoId ->
                                        onStartEntrenamiento(entrenamientoId)
                                    }
                                },
                                enabled = entrenamientoActivo == null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Iniciar entrenamiento")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "💪 Ejercicios",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (ejercicios.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "Esta rutina aún no tiene ejercicios.",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        ejercicios.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {

                                    Text(
                                        text = item.ejercicio.nombre,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                onNavigateToEjercicio(item.ejercicio.id)
                                            },
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.moverEjercicio(
                                                rutinaId,
                                                item,
                                                ejercicios[index - 1]
                                            )
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Subir"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.moverEjercicio(
                                                rutinaId,
                                                item,
                                                ejercicios[index + 1]
                                            )
                                        },
                                        enabled = index < ejercicios.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Bajar"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            ejercicioAEliminar = item.ejercicio
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar ejercicio"
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    // 🔽 BottomSheet de ajustes
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            RutinaSettingsSheet(
                onRename = {
                    showSettings = false
                    nuevoNombre = rutina?.nombre ?: ""
                    showRenameDialog = true
                },
                onDelete = {
                    showSettings = false
                    showDeleteDialog = true
                }
            )
        }
    }

    // ⚠️ Diálogo de confirmación de borrar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar '${rutina?.nombre ?: "Rutina"}'") },
            text = {
                Text("Esta acción no se puede deshacer. ¿Seguro que quieres eliminar esta rutina?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.borrarRutina(rutinaId)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Cambiar nombre") },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nombre de la rutina") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renombrarRutina(rutinaId, nuevoNombre)
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Añadir ejercicio") },
            text = {
                Column {
                    todosLosEjercicios.forEach { ejercicio ->
                        TextButton(
                            onClick = {
                                viewModel.añadirEjercicioARutina(
                                    rutinaId,
                                    ejercicio.id
                                )
                                showAddDialog = false
                            }
                        ) {
                            Text(ejercicio.nombre)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (ejercicioAEliminar != null) {
        AlertDialog(
            onDismissRequest = { ejercicioAEliminar = null },
            title = { Text("Eliminar ejercicio") },
            text = {
                Text("¿Quitar '${ejercicioAEliminar!!.nombre}' de esta rutina?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.quitarEjercicioDeRutina(
                            rutinaId,
                            ejercicioAEliminar!!.id
                        )
                        ejercicioAEliminar = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { ejercicioAEliminar = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

}

