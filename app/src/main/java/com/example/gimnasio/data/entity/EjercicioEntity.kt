package com.example.gimnasio.data.entity

@Entity(
    tableName = "ejercicios",
    foreignKeys = [
        ForeignKey(
            entity = RutinaEntity::class,
            parentColumns = ["id"],
            childColumns = ["rutinaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("rutinaId"),
        Index(value = ["rutinaId", "nombre"], unique = true)
    ]
)
data class EjercicioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val rutinaId: Long
)
