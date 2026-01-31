package com.example.gimnasio.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.RutinaEntity

data class RutinaConEjercicios(

    @Embedded
    val rutina: RutinaEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "rutinaId"
    )
    val ejercicios: List<EjercicioEntity>
)
