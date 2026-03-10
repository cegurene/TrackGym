package com.example.gimnasio.data.model

import com.example.gimnasio.data.entity.SerieEntity

data class UltimaSesionEjercicio(
    val fecha: Long,
    val series: List<SerieEntity>
)