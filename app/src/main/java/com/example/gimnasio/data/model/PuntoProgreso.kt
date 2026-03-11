package com.example.gimnasio.data.model

data class PuntoProgreso(
    val fecha: Long,
    val pesoMax: Float? = null,  // para fuerza
    val tiempo: Int? = null      // para cardio, en minutos
)