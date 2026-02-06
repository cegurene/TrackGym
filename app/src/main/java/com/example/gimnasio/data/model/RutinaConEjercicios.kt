package com.example.gimnasio.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.entity.RutinaEjercicioEntity

data class RutinaConEjercicios(

    @Embedded
    val rutina: RutinaEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RutinaEjercicioEntity::class,
            parentColumn = "rutinaId",
            entityColumn = "ejercicioId"
        )
    )
    val ejercicios: List<EjercicioEntity>
)
