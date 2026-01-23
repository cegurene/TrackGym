package com.example.gimnasio.data.entity

@Entity(
    tableName = "series",
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
data class SerieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entrenamientoId: Long,
    val ejercicioId: Long,
    val peso: Float,
    val repeticiones: Int
)
