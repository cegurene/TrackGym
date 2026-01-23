package com.example.gimnasio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rutinaId: Long,
    val fecha: Long // timestamp
)
