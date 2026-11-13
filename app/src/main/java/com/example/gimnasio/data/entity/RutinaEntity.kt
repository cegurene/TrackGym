package com.example.gimnasio.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "rutinas",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class RutinaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    @ColumnInfo(defaultValue = "0")
    val eliminado: Boolean = false
)
