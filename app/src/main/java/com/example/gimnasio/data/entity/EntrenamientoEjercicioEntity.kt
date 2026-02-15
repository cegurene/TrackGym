package com.example.gimnasio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entrenamiento_ejercicio",
    foreignKeys = [
        ForeignKey(
            entity = EntrenamientoEntity::class,
            parentColumns = ["id"],
            childColumns = ["entrenamientoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EjercicioEntity::class,
            parentColumns = ["id"],
            childColumns = ["ejercicioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("entrenamientoId"),
        Index("ejercicioId")
    ]
)
data class EntrenamientoEjercicioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entrenamientoId: Long,
    val ejercicioId: Long,
    val orden: Int
)
