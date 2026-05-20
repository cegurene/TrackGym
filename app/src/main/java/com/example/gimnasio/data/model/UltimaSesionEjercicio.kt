package com.example.gimnasio.data.model

import com.example.gimnasio.data.entity.SerieEntity

data class UltimaSesionEjercicio(
    val fecha: Long,
    val nombreEntrenamiento: String,
    val series: List<SerieEntity>
)