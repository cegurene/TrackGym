package com.example.gimnasio.data.entity

@Entity(
    tableName = "rutinas",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class RutinaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String
)
