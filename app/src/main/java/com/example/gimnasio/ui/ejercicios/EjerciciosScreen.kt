package com.example.gimnasio.ui.ejercicios

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.components.labelWithEmoji
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EjerciciosScreen(
    onBack: () -> Unit,
    onEjercicioClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }

    val viewModel: EjercicioViewModel = viewModel(
        factory = EjercicioViewModelFactory(context.applicationContext as Application, database)
    )

    val ejercicios by viewModel.ejercicios.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var nombreEjercicio by remember { mutableStateOf("") }
    var mostrarErrorMusculo by remember { mutableStateOf(false) }
    var mostrarErrorNombre by remember { mutableStateOf(false) }
    var mostrarErrorNombreDuplicado by remember { mutableStateOf(false) }

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
                val filtrosScrollState = rememberScrollState()
                val filtrosScrollScope = rememberCoroutineScope()

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(filtrosScrollState)
                                    .padding(horizontal = 28.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
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

                            if (filtrosScrollState.value > 0) {
                                IconButton(
                                    onClick = {
                                        filtrosScrollScope.launch {
                                            filtrosScrollState.animateScrollTo(0)
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Ver filtros anteriores"
                                    )
                                }
                            }

                            if (filtrosScrollState.value < filtrosScrollState.maxValue) {
                                IconButton(
                                    onClick = {
                                        filtrosScrollScope.launch {
                                            filtrosScrollState.animateScrollTo(filtrosScrollState.maxValue)
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Ver más filtros"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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
                    contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ejercicios) { ejercicio ->
                        EjercicioItem(
                            nombre = ejercicio.nombre,
                            musculos = ejercicio.musculos,
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
            title = { Text("💪 Nuevo ejercicio") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = nombreEjercicio,
                        onValueChange = {
                            nombreEjercicio = it
                            mostrarErrorNombre = false
                            mostrarErrorNombreDuplicado = false
                        },
                        modifier = Modifier.fillMaxWidth(),
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

                    if (mostrarErrorNombreDuplicado) {
                        Text(
                            text = "Ya existe un ejercicio con ese nombre. Cambialo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Músculo principal",
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (mostrarErrorMusculo) {
                        Text(
                            text = "Debes seleccionar al menos un músculo",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Musculo.entries.forEach { musculo ->
                            FilterChip(
                                selected = musculoSeleccionado == musculo,
                                onClick = {
                                    musculoSeleccionado =
                                        if (musculoSeleccionado == musculo) null else musculo
                                    mostrarErrorMusculo = false
                                },
                                label = { Text(musculo.labelWithEmoji()) },
                                leadingIcon = if (musculoSeleccionado == musculo) {
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
                                nombreEjercicio,
                                listOf(musculoSeleccionado!!)
                            ) { resultado ->
                                when (resultado) {
                                    EjercicioViewModel.NombreOperacionResultado.OK -> {
                                        nombreEjercicio = ""
                                        musculoSeleccionado = null
                                        mostrarErrorNombreDuplicado = false
                                        showDialog = false
                                    }
                                    EjercicioViewModel.NombreOperacionResultado.DUPLICADO -> {
                                        mostrarErrorNombreDuplicado = true
                                    }
                                    EjercicioViewModel.NombreOperacionResultado.VACIO -> {
                                        mostrarErrorNombre = true
                                        mostrarErrorNombreDuplicado = false
                                    }
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

                Musculo.entries
                    .toList()
                    .chunked(3)
                    .forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            fila.forEach { musculo ->
                                FilterChip(
                                    selected = selectedMusculos.contains(musculo),
                                    onClick = { viewModel.toggleMusculo(musculo) },
                                    label = {
                                        Text(
                                            musculo.labelWithEmoji(),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
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
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                )
                            }

                            repeat(3 - fila.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                //Spacer(modifier = Modifier.height(14.dp))

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
