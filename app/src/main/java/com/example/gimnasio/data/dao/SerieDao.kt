package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.DiaVolumen
import com.example.gimnasio.data.model.EjercicioRecords
import com.example.gimnasio.data.model.PuntoProgreso
import com.example.gimnasio.data.model.SerieRecord
import com.example.gimnasio.data.model.VolumenPorMusculo
import kotlinx.coroutines.flow.Flow

@Dao
interface SerieDao {

    @Insert
    suspend fun insert(serie: SerieEntity)

    @Query("""
        SELECT * FROM series
        WHERE entrenamientoEjercicioId = :ejercicioId
        ORDER BY id DESC
    """)
    fun getSeriesPorEjercicio(ejercicioId: Long): Flow<List<SerieEntity>>

    @Query("""
    SELECT e.musculos as musculo,
           SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumen
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee 
        ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN ejercicios e 
        ON e.id = ee.ejercicioId
    WHERE e.musculos != 'CARDIO'
    GROUP BY e.musculos
""")
    fun getVolumenPorMusculoRaw(): Flow<List<VolumenPorMusculo>>

    @Query("""
    SELECT 
        (COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumen,
        e.nombre as nombreEjercicio,
        e.musculos as musculo
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee 
        ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN ejercicios e 
        ON e.id = ee.ejercicioId
    ORDER BY volumen DESC
    LIMIT 1
""")
    suspend fun getSerieMasVolumen(): SerieRecord?

    @Query("""
    SELECT datetime(e.fechaInicio / 1000, 'unixepoch') as dia,
           SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumenTotal
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN entrenamientos e ON e.id = ee.entrenamientoId
    GROUP BY dia
    ORDER BY volumenTotal DESC
    LIMIT 1
""")
    suspend fun getDiaMasVolumen(): DiaVolumen?

    @Query("UPDATE series SET tiempo = :tiempo WHERE id = :idSerie")
    suspend fun actualizarTiempo(idSerie: Long, tiempo: Int)

    @Query("""
    SELECT 
        e.fechaInicio as fecha,
        MAX(COALESCE(s.peso,0)) as pesoMax
    FROM series s
    JOIN entrenamiento_ejercicio ee
        ON ee.id = s.entrenamientoEjercicioId
    JOIN entrenamientos e
        ON e.id = ee.entrenamientoId
    WHERE ee.ejercicioId = :ejercicioId
    GROUP BY e.id
    ORDER BY e.fechaInicio ASC
    """)
    fun getProgresoEjercicio(ejercicioId: Long): Flow<List<PuntoProgreso>>

    @Query("""
    SELECT
        MAX(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumenMaxSerie,
        SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumenTotal,
        COUNT(*) as seriesTotales,
        SUM(COALESCE(s.repeticiones,0)) as repeticionesTotales
    FROM series s
    JOIN entrenamiento_ejercicio ee
        ON ee.id = s.entrenamientoEjercicioId
    WHERE ee.ejercicioId = :ejercicioId
    """)
    fun getRecordsEjercicio(ejercicioId: Long): Flow<EjercicioRecords>

    @Query("""
    SELECT ee.id
    FROM entrenamiento_ejercicio ee
    JOIN entrenamientos e
    ON e.id = ee.entrenamientoId
    WHERE ee.ejercicioId = :ejercicioId
    ORDER BY e.fechaInicio DESC
    LIMIT 1
    """)
    suspend fun getUltimoEntrenamientoEjercicioId(ejercicioId: Long): Long?

    @Query("""
    SELECT e.fechaInicio
    FROM entrenamiento_ejercicio ee
    JOIN entrenamientos e
    ON e.id = ee.entrenamientoId
    WHERE ee.id = :entrenamientoEjercicioId
    """)
    suspend fun getFechaEntrenamiento(entrenamientoEjercicioId: Long): Long?

    @Query("""
    SELECT *
    FROM series
    WHERE entrenamientoEjercicioId = :entrenamientoEjercicioId
    ORDER BY id ASC
    """)
    suspend fun getSeriesEntrenamiento(entrenamientoEjercicioId: Long): List<SerieEntity>
}
