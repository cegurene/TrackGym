package com.example.gimnasio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.gimnasio.ui.ejercicios.EjercicioDetailScreen
import com.example.gimnasio.ui.ejercicios.EjerciciosScreen
import com.example.gimnasio.ui.rutinas.RutinaDetailScreen
import com.example.gimnasio.ui.rutinas.RutinasScreen

@Composable
fun GymNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "rutinas",
        modifier = modifier
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
                onBack = { navController.popBackStack() }
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

        composable("ejercicioDetail/{ejercicioId}") { backStackEntry ->
            val ejercicioId = backStackEntry.arguments
                ?.getString("ejercicioId")
                ?.toLong() ?: return@composable

            EjercicioDetailScreen(
                ejercicioId = ejercicioId,
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    // aquí irá el bottom sheet (paso siguiente)
                }
            )
        }

    }
}
