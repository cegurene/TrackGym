package com.example.gimnasio.ui.ejercicios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.model.UltimaSesionEjercicio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjercicioDetailScreen(
    ejercicioId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }

    val viewModel: EjercicioViewModel = viewModel(
        factory = EjercicioViewModelFactory(database)
    )

    var showSettings by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showEditMusculosDialog by remember { mutableStateOf(false) }

    var nuevoNombre by remember { mutableStateOf("") }
    var musculosSeleccionados by remember { mutableStateOf(setOf<Musculo>()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ejercicio by viewModel.getEjercicio(ejercicioId).collectAsState()

    val progreso by viewModel
        .getProgresoEjercicio(ejercicioId)
        .collectAsState(initial = emptyList())

    val records by viewModel
        .getRecordsEjercicio(ejercicioId)
        .collectAsState(initial = null)

    var ultimaSesion by remember { mutableStateOf<UltimaSesionEjercicio?>(null) }

    LaunchedEffect(ejercicioId) {
        ultimaSesion = viewModel.getUltimaSesion(ejercicioId)
    }

    fun formatearFecha(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ejercicio?.nombre ?: "Ejercicio") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val ejercicioLocal = ejercicio
            if (ejercicioLocal == null) {
                CircularProgressIndicator()
            } else {

                // -------------------
                // CONTENIDO PRINCIPAL
                // -------------------
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // -------------------
                    //    ÚLTIMA SESIÓN
                    // -------------------
                    item{
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ){
                            Column(Modifier.padding(16.dp)) {

                                Text("Última sesión", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(12.dp))

                                if (ultimaSesion == null) {
                                    Text("Este ejercicio aún no se ha realizado.")
                                } else {

                                    Text("Fecha: ${formatearFecha(ultimaSesion!!.fecha)}")

                                    Spacer(Modifier.height(12.dp))

                                    ultimaSesion!!.series.forEachIndexed { index, serie ->

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {

                                                Text("Serie ${index + 1}:")

                                                Text("${serie.peso ?: 0f} kg")

                                                Text("${serie.repeticiones ?: 0} reps")

                                            }
                                        }

                                    }

                                }

                            }
                        }
                    }


                    // -------------------
                    //       ACTIVIDAD
                    // -------------------
                    item{
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ){
                            Column(Modifier.padding(16.dp)) {

                                Text("📈 Actividad", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(12.dp))

                                if (progreso.isEmpty()) {

                                    Text("Aún no hay datos de progreso.")

                                } else {

                                    val primerPeso = progreso.first().pesoMax
                                    val ultimoPeso = progreso.last().pesoMax
                                    val diferencia = ultimoPeso - primerPeso

                                    Text("Primer peso registrado: ${primerPeso} kg")
                                    Text("Último peso registrado: ${ultimoPeso} kg")

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = "Diferencia: ${if (diferencia >= 0) "+" else ""}$diferencia kg",
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Spacer(Modifier.height(16.dp))

                                    // Placeholder para la gráfica
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Gráfica de progreso (próximamente)")
                                        }
                                    }

                                }

                            }
                        }
                    }


                    // -------------------
                    //        RECORDS
                    // -------------------
                    item{
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ){
                            Column(Modifier.padding(16.dp)) {

                                Text("🏆 Records", style = MaterialTheme.typography.titleLarge)

                                Spacer(Modifier.height(12.dp))

                                if (records == null) {

                                    Text("Aún no hay estadísticas.")

                                } else {

                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Mayor volumen en 1 serie: ")
                                            Text("${records!!.volumenMaxSerie ?: 0f} kg")
                                        }
                                    }

                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Volumen total: ")
                                            Text("${records!!.volumenTotal ?: 0f} kg")
                                        }
                                    }

                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Series totales: ")
                                            Text("${records!!.seriesTotales}")
                                        }
                                    }

                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Repeticiones totales: ")
                                            Text("${records!!.repeticionesTotales ?: 0}")
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

    // 🔹 BottomSheet de ajustes
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState
        ) {
            EjercicioSettingsSheet(
                onRename = {
                    showSettings = false
                    nuevoNombre = ejercicio?.nombre ?: ""
                    showRenameDialog = true
                },
                onDelete = {
                    showSettings = false
                    showDeleteDialog = true
                },
                onEditMusculos = {
                    showSettings = false
                    musculosSeleccionados = ejercicio?.musculos?.toSet() ?: emptySet()
                    showEditMusculosDialog = true
                }
            )
        }
    }

    // ⚠️ Diálogo borrar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar '${ejercicio?.nombre ?: "Ejercicio"}'") },
            text = { Text("Esta acción no se puede deshacer. ¿Seguro que quieres eliminar este ejercicio?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.borrarEjercicio(ejercicioId)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ⚠️ Diálogo renombrar
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Cambiar nombre") },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nombre del ejercicio") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renombrarEjercicio(ejercicioId, nuevoNombre)
                    showRenameDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // ⚠️ Diálogo editar músculos
    if (showEditMusculosDialog) {
        AlertDialog(
            onDismissRequest = { showEditMusculosDialog = false },
            title = { Text("Selecciona músculos") },
            text = {
                Column {
                    Musculo.values().forEach { musculo ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = musculosSeleccionados.contains(musculo),
                                onCheckedChange = { checked ->
                                    musculosSeleccionados =
                                        if (checked) musculosSeleccionados + musculo
                                        else musculosSeleccionados - musculo
                                }
                            )
                            Text(musculo.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.actualizarMusculos(ejercicioId, musculosSeleccionados.toList())
                    showEditMusculosDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditMusculosDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

