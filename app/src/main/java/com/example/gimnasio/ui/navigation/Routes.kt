package com.example.gimnasio.ui.navigation

sealed class Routes(val route: String) {
    object Main : Routes("main")
    object RutinaDetail : Routes("rutina_detail/{rutinaId}") {
        fun createRoute(rutinaId: Long) = "rutina_detail/$rutinaId"
    }
}
