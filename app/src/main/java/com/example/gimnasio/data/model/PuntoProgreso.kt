package com.example.gimnasio.data.model

data class PuntoProgreso(
    val fecha: Long,
    val valor: Float,
    val pesoMax: Float? = null,
    val tiempo: Int? = null,
    val repeticionesPesoMax: Int? = null,
    val intensidadTiempoMax: Int? = null
)