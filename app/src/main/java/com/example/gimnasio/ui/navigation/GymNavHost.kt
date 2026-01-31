package com.example.gimnasio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.gimnasio.ui.main.MainScreen
import com.example.gimnasio.ui.rutina.RutinaDetailScreen

@Composable
fun GymNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Main.route
    ) {

        composable(Routes.Main.route) {
            MainScreen(
                onRutinaClick = { rutinaId ->
                    navController.navigate(
                        Routes.RutinaDetail.createRoute(rutinaId)
                    )
                }
            )
        }

        composable(
            route = Routes.RutinaDetail.route,
            arguments = listOf(
                navArgument("rutinaId") { type = NavType.LongType }
            )
        ) {
            RutinaDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
