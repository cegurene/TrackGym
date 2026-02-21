package com.example.gimnasio.ui.estadisticas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.ui.ejercicios.EjercicioViewModel
import com.example.gimnasio.ui.ejercicios.EjercicioViewModelFactory

@Composable
fun EstadisticasScreen(
    onEstadisticasClick: () -> Unit
) {

    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }
    val viewModel: EjercicioViewModel = viewModel(
        factory = EjercicioViewModelFactory(database)
    )

    val stats by viewModel.estadisticasMusculos.collectAsState()

    val totalEjercicios = stats.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Estadísticas por músculo",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (totalEjercicios == 0) {
            Text("No hay ejercicios creados todavía.")
        } else {

            LazyColumn {
                items(
                    stats.toList()
                        .sortedByDescending { it.second }
                ) { (musculo, cantidad) ->

                    val porcentaje =
                        if (totalEjercicios > 0)
                            (cantidad * 100) / totalEjercicios
                        else 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        onClick = {
                            onEstadisticasClick()
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = musculo.name
                            )
                            Text(
                                text = "$cantidad ejercicios • $porcentaje%"
                            )
                        }
                    }
                }
            }
        }
    }
}