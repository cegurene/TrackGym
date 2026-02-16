package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.EntrenamientoEjercicioConSeries
import kotlinx.coroutines.flow.Flow

@Dao
interface EntrenamientoDao {

    @Insert
    suspend fun insert(entrenamiento: EntrenamientoEntity): Long

    @Query("""
        SELECT * FROM entrenamientos
        WHERE rutinaId = :rutinaId
        ORDER BY fechaInicio DESC
    """)
    fun getByRutina(rutinaId: Long): Flow<List<EntrenamientoEntity>>

    @Query("SELECT * FROM entrenamientos WHERE completado = 0 LIMIT 1")
    suspend fun getEntrenamientoActivo(): EntrenamientoEntity?

    @Query("SELECT * FROM entrenamientos WHERE completado = 0 LIMIT 1")
    fun getEntrenamientoActivoFlow(): Flow<EntrenamientoEntity?>

    @Query("""
    SELECT * FROM entrenamiento_ejercicio
    WHERE entrenamientoId = :entrenamientoId
    ORDER BY orden ASC
""")
    fun getEjerciciosDeEntrenamiento(
        entrenamientoId: Long
    ): Flow<List<EntrenamientoEjercicioEntity>>

    @Transaction
    @Query("""
    SELECT * FROM entrenamiento_ejercicio
    WHERE entrenamientoId = :entrenamientoId
    ORDER BY orden ASC
""")
    fun getEjerciciosConSeries(
        entrenamientoId: Long
    ): Flow<List<EntrenamientoEjercicioConSeries>>

    @Insert
    suspend fun insertSerie(serie: SerieEntity)

    @Query("""
    UPDATE series
    SET completada = :completada
    WHERE id = :serieId
""")
    suspend fun updateSerieCompletada(
        serieId: Long,
        completada: Boolean
    )

    @Query("""
    UPDATE series
    SET peso = :peso
    WHERE id = :serieId
""")
    suspend fun updatePesoSerie(serieId: Long, peso: Float)

    @Query("""
    UPDATE series
    SET repeticiones = :reps
    WHERE id = :serieId
""")
    suspend fun updateRepsSerie(serieId: Long, reps: Int)


}
