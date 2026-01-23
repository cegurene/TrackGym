package com.example.gimnasio.data.model

data class Ejercicio(
    val nombre: String,
    val series: MutableList<Serie> = mutableListOf()
)