package com.example.gimnasio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "rutina_ejercicio",
    primaryKeys = ["rutinaId", "ejercicioId"],
    foreignKeys = [
        ForeignKey(
            entity = RutinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["rutinaId"],
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
        Index("rutinaId"),
        Index("ejercicioId")
    ]
)
data class RutinaEjercicioEntity(
    val rutinaId: Long,
    val ejercicioId: Long
)
