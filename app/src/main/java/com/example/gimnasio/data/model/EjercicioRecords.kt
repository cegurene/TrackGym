package com.example.gimnasio.data.model

data class EjercicioRecords(
    val volumenMaxSerie: Float?,
    val fechaVolumenMaxSerie: Long?,
    val volumenTotal: Float?,
    val seriesTotales: Int,
    val repeticionesTotales: Int?,
    val entrenamientosConEjercicio: Int
)