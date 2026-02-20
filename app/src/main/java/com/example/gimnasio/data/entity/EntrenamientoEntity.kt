package com.example.gimnasio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entrenamientos",
    foreignKeys = [
        ForeignKey(
            entity = RutinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["rutinaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rutinaId")]
)
data class EntrenamientoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rutinaId: Long,
    val nombre: String,
    val fechaInicio: Long,
    val fechaFin: Long? = null,
    val completado: Boolean = false
)
