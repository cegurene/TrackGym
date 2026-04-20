package com.example.gimnasio.ui.rutinas

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.components.displayLabel
import com.example.gimnasio.ui.components.imageRes
import com.example.gimnasio.ui.components.labelWithEmoji
import kotlinx.coroutines.launch

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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(Musculo.entries.toList()) { musculo ->
                        val seleccionado = selectedMusculos.contains(musculo)

                        Surface(
                            onClick = { viewModel.toggleMusculo(musculo) },
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
