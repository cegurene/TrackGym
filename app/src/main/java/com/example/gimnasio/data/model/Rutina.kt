package com.example.gimnasio.data.model

data class Rutina(
    val nombre: String,
    val ejercicios: MutableList<Ejercicio> = mutableListOf()
)