package com.example.gimnasio.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity

data class EntrenamientoConEjerciciosYSeries(
    @Embedded val entrenamiento: EntrenamientoEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "entrenamientoId",
        entity = EntrenamientoEjercicioEntity::class
    )
    val ejercicios: List<EntrenamientoEjercicioConSeries>
)
