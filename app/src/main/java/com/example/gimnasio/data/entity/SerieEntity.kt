package com.example.gimnasio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity

@Entity(
    tableName = "series",
    foreignKeys = [
        ForeignKey(
            entity = EntrenamientoEjercicioEntity::class,
            parentColumns = ["id"],
            childColumns = ["entrenamientoEjercicioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("entrenamientoEjercicioId")
    ]
)
data class SerieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entrenamientoEjercicioId: Long,

    val peso: Float? = null,
    val repeticiones: Int? = null,
    val tiempo: Int? = null,   // 🔥 NUEVO

    val completada: Boolean = false
)
