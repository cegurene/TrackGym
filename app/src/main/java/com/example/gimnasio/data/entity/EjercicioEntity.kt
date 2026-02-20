package com.example.gimnasio.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ejercicios",
    indices = [Index(value = ["nombre"], unique = true)]
)

data class EjercicioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val musculos: List<Musculo>
)
