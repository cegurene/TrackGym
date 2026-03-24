package com.example.gimnasio.ui.ejercicios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EjerciciosScreen(
    onBack: () -> Unit,
    onEjercicioClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }

    val viewModel: EjercicioViewModel = viewModel(
        factory = EjercicioViewModelFactory(database)
    )

    val ejercicios by viewModel.ejercicios.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var nombreEjercicio by remember { mutableStateOf("") }
    var mostrarErrorMusculo by remember { mutableStateOf(false) }
    var mostrarErrorNombre by remember { mutableStateOf(false) }

    // Para la busqueda y filtros
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMusculos by viewModel.selectedMusculos.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false // permite partial + full
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💪 Ejercicios") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear ejercicio")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                label = { Text("Buscar ejercicio") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            )

            FilledTonalButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("🧩", modifier = Modifier.padding(end = 8.dp))
                if (selectedMusculos.isEmpty()) {
                    Text("Filtrar por músculo")
                } else {
                    Text("Filtrar (${selectedMusculos.size})")
                }
            }

            AnimatedVisibility(
                visible = selectedMusculos.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            selectedMusculos.forEach { musculo ->

                                AssistChip(
                                    onClick = { viewModel.toggleMusculo(musculo) },
                                    label = {
                                        Text(
                                            musculo.name
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar filtro"
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AssistChip(
                            onClick = { viewModel.clearMusculos() },
                            label = { Text("Limpiar todo") }
                        )
                    }
                }
            }

            if (ejercicios.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Aún no hay ejercicios. Añade el primero con el botón +",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ejercicios) { ejercicio ->
                        EjercicioItem(
                            nombre = ejercicio.nombre,
                            onClick = { onEjercicioClick(ejercicio.id) }
                        )
                    }
                }
            }
        }
    }

    // 🔹 Diálogo crear ejercicio
    if (showDialog) {
        var musculoSeleccionado by remember { mutableStateOf<Musculo?>(null) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuevo ejercicio") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreEjercicio,
                        onValueChange = {
                            nombreEjercicio = it
                            mostrarErrorNombre = false
                        },
                        label = { Text("Nombre del ejercicio") },
                        singleLine = true
                    )

                    if (mostrarErrorNombre) {
                        Text(
                            text = "El nombre del ejercicio no puede estar vacío",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Músculos trabajados")

                    if (mostrarErrorMusculo) {
                        Text(
                            text = "Debes seleccionar al menos un músculo",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Musculo.values().forEach { musculo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = musculoSeleccionado == musculo,
                                onCheckedChange = { checked ->
                                    musculoSeleccionado = if (checked) musculo else null
                                    mostrarErrorMusculo = false
                                }
                            )
                            Text(musculo.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        var hayError = false

                        if (nombreEjercicio.isBlank()) {
                            mostrarErrorNombre = true
                            hayError = true
                        } else {
                            mostrarErrorNombre = false
                        }

                        if (musculoSeleccionado == null) {
                            mostrarErrorMusculo = true
                            hayError = true
                        } else {
                            mostrarErrorMusculo = false
                        }

                        if (!hayError) {
                            viewModel.crearEjercicio(
                                nombreEjercicio.trim(),
                                listOf(musculoSeleccionado!!)
                            )
                            nombreEjercicio = ""
                            musculoSeleccionado = null
                            showDialog = false
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Ventana de filtros de musculos
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
                    text = "Filtrar por músculo",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                Musculo.values().forEach { musculo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleMusculo(musculo) }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = selectedMusculos.contains(musculo),
                            onCheckedChange = { viewModel.toggleMusculo(musculo) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(musculo.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { viewModel.clearMusculos() }) {
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
