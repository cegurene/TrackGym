package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.DiaVolumen
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
           SUM(s.peso * s.repeticiones) as volumen
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee 
        ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN ejercicios e 
        ON e.id = ee.ejercicioId
    GROUP BY e.musculos
""")
    fun getVolumenPorMusculoRaw(): Flow<List<VolumenPorMusculo>>

    @Query("""
    SELECT MAX(peso * repeticiones)
    FROM series
    WHERE completada = 1
""")
    suspend fun getSerieMasVolumen(): Double?

    @Query("""
    SELECT datetime(e.fechaInicio / 1000, 'unixepoch') as dia,
           SUM(s.peso * s.repeticiones) as volumenTotal
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN entrenamientos e ON e.id = ee.entrenamientoId
    GROUP BY dia
    ORDER BY volumenTotal DESC
    LIMIT 1
""")
    suspend fun getDiaMasVolumen(): DiaVolumen?
}
