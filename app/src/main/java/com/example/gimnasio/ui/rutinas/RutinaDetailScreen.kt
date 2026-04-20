package com.example.gimnasio.ui.rutinas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.components.EjercicioSelectionCard
import com.example.gimnasio.ui.components.displayLabel
import com.example.gimnasio.ui.components.emojiSummary
import com.example.gimnasio.ui.components.imageRes

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
    var mostrarErrorNombreRutinaDuplicado by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var showAddDialog by remember { mutableStateOf(false) }
    var musculoFiltro by remember { mutableStateOf<Musculo?>(null) }
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
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
                                    enabled = entrenamientoActivo == null && ejercicios.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Iniciar entrenamiento")
                                }

                                if (ejercicios.isEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Añade al menos un ejercicio para poder empezar.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "💪 Ejercicios",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (ejercicios.isEmpty()) {
                        item {
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
                        }
                    } else {
                        itemsIndexed(ejercicios) { index, item ->
                            val musculosEmoji = item.ejercicio.musculos.emojiSummary()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.ejercicio.nombre,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onNavigateToEjercicio(item.ejercicio.id) },
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = musculosEmoji,
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row {
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
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        IconButton(
                                            onClick = { ejercicioAEliminar = item.ejercicio }
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
                    mostrarErrorNombreRutinaDuplicado = false
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
                    onValueChange = {
                        nuevoNombre = it
                        mostrarErrorNombreRutinaDuplicado = false
                    },
                    label = { Text("Nombre de la rutina") },
                    singleLine = true
                )

                if (mostrarErrorNombreRutinaDuplicado) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ya existe una rutina con ese nombre. Cambialo.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renombrarRutina(rutinaId, nuevoNombre) { resultado ->
                            when (resultado) {
                                RutinaViewModel.NombreOperacionResultado.OK -> {
                                    showRenameDialog = false
                                    mostrarErrorNombreRutinaDuplicado = false
                                }
                                RutinaViewModel.NombreOperacionResultado.DUPLICADO -> {
                                    mostrarErrorNombreRutinaDuplicado = true
                                }
                                RutinaViewModel.NombreOperacionResultado.VACIO -> {
                                    mostrarErrorNombreRutinaDuplicado = false
                                }
                            }
                        }
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
        val ejerciciosNoEnRutina = todosLosEjercicios.filter { ejercicio ->
            ejercicio.id !in ejercicios.map { it.ejercicio.id }
        }
        val ejerciciosNoEnRutinaFiltrados = remember(ejerciciosNoEnRutina, musculoFiltro) {
            ejerciciosNoEnRutina.filter { ejercicio ->
                musculoFiltro == null || ejercicio.musculos.contains(musculoFiltro)
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Añadir ejercicio") },
            text = {
                if (ejerciciosNoEnRutina.isEmpty()) {
                    Text("Todos los ejercicios ya están en esta rutina.")
                } else {
                    var filtroExpandido by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = filtroExpandido,
                            onExpandedChange = { filtroExpandido = !filtroExpandido }
                        ) {
                            OutlinedTextField(
                                value = musculoFiltro?.displayLabel() ?: "Todos",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Filtrar por músculo") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = filtroExpandido)
                                },
                                modifier = Modifier
                                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )
                        }

                        if (filtroExpandido) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gridItems(Musculo.entries.toList()) { musculo ->
                                    val seleccionado = musculoFiltro == musculo
                                    Surface(
                                        onClick = {
                                            musculoFiltro = if (seleccionado) null else musculo
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (seleccionado) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (seleccionado) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            }
                                        )
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = musculo.imageRes()),
                                                contentDescription = musculo.displayLabel(),
                                                modifier = Modifier.height(65.dp),
                                                contentScale = ContentScale.Fit
                                            )

                                            Text(
                                                text = musculo.displayLabel(),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(ejerciciosNoEnRutinaFiltrados) { ejercicio ->
                                EjercicioSelectionCard(
                                    nombre = ejercicio.nombre,
                                    musculos = ejercicio.musculos,
                                    onClick = {
                                        viewModel.añadirEjercicioARutina(
                                            rutinaId,
                                            ejercicio.id
                                        )
                                        showAddDialog = false
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cerrar")
                }
            },
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

