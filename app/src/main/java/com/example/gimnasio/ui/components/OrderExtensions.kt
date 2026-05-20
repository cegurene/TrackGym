package com.example.gimnasio.ui.components

import com.example.gimnasio.ui.ejercicios.EjercicioViewModel
import com.example.gimnasio.ui.rutinas.RutinaViewModel

fun RutinaViewModel.RutinaOrder.getLabel(): String = when (this) {
    RutinaViewModel.RutinaOrder.ALPHABETIC_ASC -> "A-Z ▲"
    RutinaViewModel.RutinaOrder.ALPHABETIC_DESC -> "A-Z ▼"
    RutinaViewModel.RutinaOrder.TIMES_DONE_ASC -> "Veces ▲"
    RutinaViewModel.RutinaOrder.TIMES_DONE_DESC -> "Veces ▼"
}

fun EjercicioViewModel.EjercicioOrder.getLabel(): String = when (this) {
    EjercicioViewModel.EjercicioOrder.ALPHABETIC_ASC -> "A-Z ▲"
    EjercicioViewModel.EjercicioOrder.ALPHABETIC_DESC -> "A-Z ▼"
    EjercicioViewModel.EjercicioOrder.MUSCLE_ASC -> "Mús. ▲"
    EjercicioViewModel.EjercicioOrder.MUSCLE_DESC -> "Mús. ▼"
}

