package com.example.gimnasio.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.gimnasio.ui.entrenamiento.EntrenamientoDetailScreen
import com.example.gimnasio.ui.entrenamiento.EntrenamientoScreen
import com.example.gimnasio.ui.home.HomeScreen
import com.example.gimnasio.ui.rutinas.RutinaDetailScreen

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

    // Comprobación automática al arrancar
    LaunchedEffect(Unit) {
        val activo = entrenamientoDao.getEntrenamientoActivo()
        if (activo != null) {
            navController.navigate("entrenamiento/${activo.id}") {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Column(modifier = modifier.fillMaxSize()) {

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.weight(1f)
        ) {

            composable("home") {
                HomeScreen(
                    onRutinaClick = { rutinaId ->
                        navController.navigate("rutinaDetail/$rutinaId")
                    },
                    onEjercicioClick = { ejercicioId ->
                        navController.navigate("ejercicioDetail/$ejercicioId")
                    },
                    onEntrenamientoClick = { entrenamientoId ->
                        navController.navigate("entrenamientoDetail/$entrenamientoId")
                    },
                    onEstadisticasClick = {
                        // No navega porque es una TAB dentro de Home
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
                    },
                    onNavigateToEjercicio = { ejercicioId ->
                        navController.navigate("ejercicioDetail/$ejercicioId")
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
                    onOpenSettings = { }
                )
            }

            composable("entrenamiento/{entrenamientoId}") { backStackEntry ->
                val entrenamientoId =
                    backStackEntry.arguments
                        ?.getString("entrenamientoId")
                        ?.toLong() ?: 0L

                EntrenamientoScreen(
                    entrenamientoId = entrenamientoId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("entrenamientoDetail/{entrenamientoId}") { backStackEntry ->
                val entrenamientoId =
                    backStackEntry.arguments
                        ?.getString("entrenamientoId")
                        ?.toLong() ?: return@composable

                EntrenamientoDetailScreen(
                    entrenamientoId = entrenamientoId,
                    onBack = { navController.popBackStack() },
                    onNavigateToEntrenamiento = { nuevoId ->
                        navController.navigate("entrenamiento/$nuevoId")
                    },
                    onNavigateToEjercicio = { ejercicioId ->
                        navController.navigate("ejercicioDetail/$ejercicioId")
                    }
                )
            }
        }

        // Banner entrenamiento activo
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