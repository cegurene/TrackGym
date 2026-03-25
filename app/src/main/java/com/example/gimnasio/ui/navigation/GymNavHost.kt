package com.example.gimnasio.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.gimnasio.ui.historico.HistoricoDetailScreen
import com.example.gimnasio.ui.home.HomeScreen
import com.example.gimnasio.ui.rutinas.RutinaDetailScreen
import com.example.gimnasio.ui.theme.ThemeMode

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GymNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    selectedThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    val homeRoute = "home"
    val context = LocalContext.current
    val database = remember { GymDatabase.getDatabase(context) }
    val entrenamientoDao = remember { database.entrenamientoDao() }

    // Usamos collectAsStateWithLifecycle si estuviera disponible, sino collectAsState es correcto
    val entrenamientoActivo by entrenamientoDao
        .getEntrenamientoActivoFlow()
        .collectAsState(initial = null)

    var ultimoEntrenamientoAutoNavegadoId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun navigateToEntrenamiento(entrenamientoId: Long) {
        navController.navigate("entrenamiento/$entrenamientoId") {
            launchSingleTop = true
        }
    }

    LaunchedEffect(entrenamientoActivo) {
        val activoId = entrenamientoActivo?.id

        if (activoId == null) {
            // Reinicia el control para permitir auto-navegación en el próximo entrenamiento nuevo.
            ultimoEntrenamientoAutoNavegadoId = null
            return@LaunchedEffect
        }

        val routeActual = navController.currentBackStackEntry?.destination?.route
        val yaAutoNavegadoEsteEntrenamiento =
            ultimoEntrenamientoAutoNavegadoId == activoId

        if (routeActual == homeRoute && !yaAutoNavegadoEsteEntrenamiento) {
            ultimoEntrenamientoAutoNavegadoId = activoId
            navigateToEntrenamiento(activoId)
        }
    }


    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            NavHost(
                navController = navController,
                startDestination = homeRoute,
                modifier = Modifier.weight(1f)
            ) {

                composable(homeRoute) {
                    HomeScreen(
                        selectedThemeMode = selectedThemeMode,
                        onThemeModeSelected = onThemeModeSelected
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
                            navigateToEntrenamiento(entrenamientoId)
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
                        onBack = {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate("ejercicios") {
                                    popUpTo("home")
                                }
                            }
                        },
                        onOpenSettings = { },
                        onGraficaScrollChange = {}
                    )
                }

                composable("entrenamiento/{entrenamientoId}") { backStackEntry ->
                    val entrenamientoId =
                        backStackEntry.arguments
                            ?.getString("entrenamientoId")
                            ?.toLong() ?: 0L

                    EntrenamientoScreen(
                        entrenamientoId = entrenamientoId,
                        onBack = {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate(homeRoute) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable("historicoDetail/{entrenamientoId}") { backStackEntry ->
                    val entrenamientoId =
                        backStackEntry.arguments
                            ?.getString("entrenamientoId")
                            ?.toLong() ?: return@composable

                    HistoricoDetailScreen(
                        entrenamientoId = entrenamientoId,
                        onBack = { navController.popBackStack() },
                        onNavigateToEntrenamiento = { nuevoId ->
                            navigateToEntrenamiento(nuevoId)
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
                        navigateToEntrenamiento(entrenamientoActivo!!.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Entrenamiento en curso — Tocar para volver",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
