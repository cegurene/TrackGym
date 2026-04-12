package com.example.gimnasio.ui.rutinas

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.components.labelWithEmoji

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RutinasScreen(
    onRutinaClick: (Long) -> Unit,
    onVerEjerciciosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: RutinaViewModel = viewModel(
        factory = RutinaViewModelFactory(context.applicationContext as Application)
    )
    val rutinas by viewModel.rutinas.collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var nombreRutina by remember { mutableStateOf("") }

    // Para la búsqueda y filtros
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMusculos by viewModel.selectedMusculos.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("🗂️ Rutinas") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear rutina")
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
                label = { Text("Buscar rutina") },
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
                                        Text(musculo.labelWithEmoji())
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

            if (rutinas.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Aún no hay rutinas. Crea tu primera rutina con el botón +",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rutinas) { rutinaConEjercicios ->
                        RutinaItem(
                            rutinaConEjercicios = rutinaConEjercicios,
                            onClick = {
                                onRutinaClick(rutinaConEjercicios.rutina.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // 🔹 Diálogo crear rutina
    if (showDialog) {
        var mostrarErrorNombre by remember { mutableStateOf(false) }
        var mostrarErrorDuplicado by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva rutina") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreRutina,
                        onValueChange = {
                            nombreRutina = it
                            mostrarErrorNombre = false
                            mostrarErrorDuplicado = false
                        },
                        label = { Text("Nombre de la rutina") },
                        singleLine = true
                    )

                    if (mostrarErrorNombre) {
                        Text(
                            text = "El nombre de la rutina no puede estar vacío",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (mostrarErrorDuplicado) {
                        Text(
                            text = "Ya existe una rutina con ese nombre. Cambialo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.insertar(nombreRutina) { resultado ->
                            when (resultado) {
                                RutinaViewModel.NombreOperacionResultado.OK -> {
                                    nombreRutina = ""
                                    showDialog = false
                                    mostrarErrorNombre = false
                                    mostrarErrorDuplicado = false
                                }
                                RutinaViewModel.NombreOperacionResultado.VACIO -> {
                                    mostrarErrorNombre = true
                                    mostrarErrorDuplicado = false
                                }
                                RutinaViewModel.NombreOperacionResultado.DUPLICADO -> {
                                    mostrarErrorDuplicado = true
                                    mostrarErrorNombre = false
                                }
                            }
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

    // Ventana de filtros de músculos
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

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Musculo.entries.forEach { musculo ->
                        FilterChip(
                            selected = selectedMusculos.contains(musculo),
                            onClick = { viewModel.toggleMusculo(musculo) },
                            label = { Text(musculo.labelWithEmoji()) },
                            leadingIcon = if (selectedMusculos.contains(musculo)) {
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
        }
    }
}
