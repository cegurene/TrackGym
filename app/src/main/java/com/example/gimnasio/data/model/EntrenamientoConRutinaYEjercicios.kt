package com.example.gimnasio.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.entity.RutinaEntity

data class EntrenamientoConRutinaYEjercicios(
    @Embedded val entrenamiento: EntrenamientoEntity,

    @Relation(
        parentColumn = "rutinaId",
        entityColumn = "id"
    )
    val rutina: RutinaEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "entrenamientoId",
        entity = EntrenamientoEjercicioEntity::class
    )
    val ejercicios: List<EntrenamientoEjercicioConSeries>
)



