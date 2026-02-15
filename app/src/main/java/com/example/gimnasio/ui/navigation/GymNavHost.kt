package com.example.gimnasio.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.ui.ejercicios.EjercicioDetailScreen
import com.example.gimnasio.ui.ejercicios.EjerciciosScreen
import com.example.gimnasio.ui.entrenamiento.EntrenamientoScreen
import com.example.gimnasio.ui.rutinas.RutinaDetailScreen
import com.example.gimnasio.ui.rutinas.RutinasScreen

@Composable
fun GymNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }
    val entrenamientoDao = remember { database.entrenamientoDao() }

    val entrenamientoActivo by entrenamientoDao
        .getEntrenamientoActivoFlow()
        .collectAsState(initial = null)

    // 🔥 Comprobación automática al arrancar
    LaunchedEffect(Unit) {
        val activo = entrenamientoDao.getEntrenamientoActivo()
        if (activo != null) {
            navController.navigate("entrenamiento/${activo.id}") {
                popUpTo("rutinas") { inclusive = false }
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Column(modifier = modifier.fillMaxSize()) {

        NavHost(
            navController = navController,
            startDestination = "rutinas",
            modifier = Modifier.weight(1f)
        ) {
            composable("rutinas") {
                RutinasScreen(
                    onRutinaClick = { rutinaId ->
                        navController.navigate("rutinaDetail/$rutinaId")
                    },
                    onVerEjerciciosClick = {
                        navController.navigate("ejercicios")
                    }
                )
            }

            composable("ejercicios") {
                EjerciciosScreen(
                    onBack = { navController.popBackStack() },
                    onEjercicioClick = { ejercicioId ->
                        navController.navigate("ejercicioDetail/$ejercicioId")
                    }
                )
            }

            composable("rutinaDetail/{rutinaId}") { backStackEntry ->
                val rutinaId = backStackEntry.arguments
                    ?.getString("rutinaId")
                    ?.toLong() ?: return@composable

                RutinaDetailScreen(
                    rutinaId = rutinaId,
                    onBack = { navController.popBackStack() },
                    onStartEntrenamiento = { entrenamientoId ->
                        navController.navigate("entrenamiento/$entrenamientoId")
                    }
                )
            }

            composable("ejercicioDetail/{ejercicioId}") { backStackEntry ->
                val ejercicioId = backStackEntry.arguments
                    ?.getString("ejercicioId")
                    ?.toLong() ?: return@composable

                EjercicioDetailScreen(
                    ejercicioId = ejercicioId,
                    onBack = { navController.popBackStack() },
                    onOpenSettings = {
                        // aquí abriremos el BottomSheet en el siguiente paso
                    }
                )

            }

            composable(
                "entrenamiento/{entrenamientoId}"
            ) { backStackEntry ->

                val entrenamientoId =
                    backStackEntry.arguments?.getString("entrenamientoId")?.toLong() ?: 0L

                EntrenamientoScreen(
                    entrenamientoId = entrenamientoId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (
            entrenamientoActivo != null &&
            currentRoute?.startsWith("entrenamiento") != true
        ) {
            Card(
                onClick = {
                    navController.navigate("entrenamiento/${entrenamientoActivo!!.id}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    //.padding(8.dp)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 25.dp)
            ) {
                Text(
                    text = "Entrenamiento en curso — Tocar para volver",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

}

