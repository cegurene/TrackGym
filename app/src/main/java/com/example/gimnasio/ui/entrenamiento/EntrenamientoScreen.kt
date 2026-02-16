package com.example.gimnasio.ui.entrenamiento

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import com.example.gimnasio.ui.rutinas.RutinaViewModel
import com.example.gimnasio.ui.rutinas.RutinaViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientoScreen(
    entrenamientoId: Long,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val viewModel: EntrenamientoViewModel = viewModel(
        factory = EntrenamientoViewModelFactory(
            context.applicationContext as Application,
            entrenamientoId
        )
    )

    val ejercicios by viewModel
        .ejerciciosDelEntrenamiento
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamiento en curso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(ejercicios) { ejercicioConSeries ->

                Text(
                    text = ejercicioConSeries.ejercicio.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                ejercicioConSeries.series.forEachIndexed { index, serie ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {

                        Checkbox(
                            checked = serie.completada,
                            onCheckedChange = { checked ->
                                viewModel.marcarSerieCompletada(
                                    serieId = serie.id,
                                    completada = checked
                                )
                            }
                        )

                        Text(
                            text = "Serie ${index + 1}",
                            modifier = Modifier.width(80.dp)
                        )

                        OutlinedTextField(
                            value = serie.peso.toString(),
                            onValueChange = { newValue ->
                                val peso = newValue.toFloatOrNull() ?: 0f
                                viewModel.actualizarPesoSerie(serie.id, peso)
                            },
                            label = { Text("Kg") },
                            modifier = Modifier.width(90.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = serie.repeticiones.toString(),
                            onValueChange = { newValue ->
                                val reps = newValue.toIntOrNull() ?: 0
                                viewModel.actualizarRepsSerie(serie.id, reps)
                            },
                            label = { Text("Reps") },
                            modifier = Modifier.width(90.dp),
                            singleLine = true
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.añadirSerie(
                            entrenamientoEjercicioId =
                                ejercicioConSeries.entrenamientoEjercicio.id,
                            peso = 0f,
                            repeticiones = 0
                        )
                    },
                    modifier = Modifier
                        .padding(start = 32.dp, bottom = 16.dp)
                ) {
                    Text("+ Añadir serie")
                }

            }
        }

    }
}
