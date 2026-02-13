package com.example.gimnasio.data.model

import androidx.room.Embedded
import com.example.gimnasio.data.entity.EjercicioEntity

data class EjercicioConOrden(
    @Embedded val ejercicio: EjercicioEntity,
    val orden: Int
)
