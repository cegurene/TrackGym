package com.example.gimnasio.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.SerieEntity

data class EntrenamientoEjercicioConSeries(

    @Embedded
    val entrenamientoEjercicio: EntrenamientoEjercicioEntity,

    @Relation(
        parentColumn = "ejercicioId",
        entityColumn = "id"
    )
    val ejercicio: EjercicioEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "entrenamientoEjercicioId"
    )
    val series: List<SerieEntity>
)
