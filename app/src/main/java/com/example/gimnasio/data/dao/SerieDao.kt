package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.DiaVolumen
import com.example.gimnasio.data.model.EjercicioRecords
import com.example.gimnasio.data.model.EjercicioRecordsCardio
import com.example.gimnasio.data.model.MejorCargaCardio
import com.example.gimnasio.data.model.MejorSesion
import com.example.gimnasio.data.model.PRRecord
import com.example.gimnasio.data.model.PuntoProgreso
import com.example.gimnasio.data.model.SerieRecord
import com.example.gimnasio.data.model.VolumenPorMusculo
import kotlinx.coroutines.flow.Flow

@Dao
interface SerieDao {

    @Insert
    suspend fun insert(serie: SerieEntity)

    @Query(
        """
        SELECT * FROM series
        WHERE entrenamientoEjercicioId = :ejercicioId
        ORDER BY id DESC
    """
    )
    fun getSeriesPorEjercicio(ejercicioId: Long): Flow<List<SerieEntity>>

    @Query(
        """
    SELECT e.musculos as musculo,
           SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumen
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee 
        ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN ejercicios e 
        ON e.id = ee.ejercicioId
    INNER JOIN entrenamientos en
        ON en.id = ee.entrenamientoId
    WHERE e.musculos != 'CARDIO'
      AND en.completado = 1
    GROUP BY e.musculos
    """
    )
    fun getVolumenPorMusculoRaw(): Flow<List<VolumenPorMusculo>>

    @Query(
        """
    SELECT 
        (COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumen,
        e.nombre as nombreEjercicio,
        e.musculos as musculo
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee 
        ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN ejercicios e 
        ON e.id = ee.ejercicioId
    INNER JOIN entrenamientos en
        ON en.id = ee.entrenamientoId
    WHERE en.completado = 1
    ORDER BY volumen DESC
    LIMIT 1
    """
    )
    suspend fun getSerieMasVolumen(): SerieRecord?

    @Query(
        """
    SELECT datetime(e.fechaInicio / 1000, 'unixepoch') as dia,
           SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumenTotal
    FROM series s
    INNER JOIN entrenamiento_ejercicio ee ON ee.id = s.entrenamientoEjercicioId
    INNER JOIN entrenamientos e ON e.id = ee.entrenamientoId
    WHERE e.completado = 1
    GROUP BY dia
    ORDER BY volumenTotal DESC
    LIMIT 1
    """
    )
    suspend fun getDiaMasVolumen(): DiaVolumen?

    @Query("UPDATE series SET tiempo = :tiempo WHERE id = :idSerie")
    suspend fun actualizarTiempo(idSerie: Long, tiempo: Int)

    @Query("UPDATE series SET intensidad = :intensidad WHERE id = :idSerie")
    suspend fun actualizarIntensidad(idSerie: Long, intensidad: Int)



    @Query(
        """
    SELECT SUM(COALESCE(s.tiempo,0))
    FROM series s
    JOIN entrenamiento_ejercicio ee
        ON ee.id = s.entrenamientoEjercicioId
    JOIN ejercicios e
        ON e.id = ee.ejercicioId
    JOIN entrenamientos en
        ON en.id = ee.entrenamientoId
    WHERE e.musculos = 'CARDIO'
    AND en.completado = 1
    """
    )
    suspend fun getTiempoTotalCardio(): Int?

    @Query(
        """
    SELECT
        e.fechaInicio as fecha,
        SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as valor,
        MAX(COALESCE(s.peso,0)) as pesoMax,
        CAST(
            SUM(
                CASE
                    WHEN COALESCE(s.peso,0) = maximos.pesoMax
                    THEN COALESCE(s.repeticiones,0)
                    ELSE 0
                END
            ) AS INTEGER
        ) as repeticionesPesoMax,
        GROUP_CONCAT(COALESCE(s.peso, 0) || 'x' || COALESCE(s.repeticiones, 0), ' | ') as seriesTexto
    FROM series s
    JOIN entrenamiento_ejercicio ee
        ON ee.id = s.entrenamientoEjercicioId
    JOIN entrenamientos e
        ON e.id = ee.entrenamientoId
    JOIN (
        SELECT
            ee2.entrenamientoId as entrenamientoId,
            MAX(COALESCE(s2.peso,0)) as pesoMax
        FROM series s2
        JOIN entrenamiento_ejercicio ee2
            ON ee2.id = s2.entrenamientoEjercicioId
        WHERE ee2.ejercicioId = :ejercicioId
        GROUP BY ee2.entrenamientoId
    ) as maximos
        ON maximos.entrenamientoId = e.id
    WHERE ee.ejercicioId = :ejercicioId
      AND e.completado = 1
    GROUP BY e.id
    ORDER BY e.fechaInicio ASC
    """
    )
    fun getProgresoEjercicio(ejercicioId: Long): Flow<List<PuntoProgreso>>

    @Query(
        """
    SELECT
        e.fechaInicio as fecha,
        SUM(COALESCE(s.tiempo,0) * COALESCE(s.intensidad,1)) as valor,
        SUM(COALESCE(s.tiempo,0)) as tiempo,
        CAST(
            SUM(
                CASE
                    WHEN COALESCE(s.tiempo,0) = maximos.tiempoMax
                    THEN COALESCE(s.intensidad,1)
                    ELSE 0
                END
            ) AS INTEGER
        ) as intensidadTiempoMax,
        GROUP_CONCAT(COALESCE(s.tiempo, 0) || 'm@' || COALESCE(s.intensidad, 1), ' | ') as seriesTexto
    FROM series s
    JOIN entrenamiento_ejercicio ee
        ON ee.id = s.entrenamientoEjercicioId
    JOIN entrenamientos e
        ON e.id = ee.entrenamientoId
    JOIN (
        SELECT
            ee2.entrenamientoId as entrenamientoId,
            MAX(COALESCE(s2.tiempo,0)) as tiempoMax
        FROM series s2
        JOIN entrenamiento_ejercicio ee2
            ON ee2.id = s2.entrenamientoEjercicioId
        WHERE ee2.ejercicioId = :ejercicioId
        GROUP BY ee2.entrenamientoId
    ) as maximos
        ON maximos.entrenamientoId = e.id
    WHERE ee.ejercicioId = :ejercicioId
      AND e.completado = 1
    GROUP BY e.id
    ORDER BY e.fechaInicio ASC
    """
    )
    fun getProgresoEjercicioCardio(ejercicioId: Long): Flow<List<PuntoProgreso>>

    @Query(
        """
    SELECT
        MAX(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumenMaxSerie,
        (SELECT en2.fechaInicio 
         FROM series s2 
         JOIN entrenamiento_ejercicio ee2 ON ee2.id = s2.entrenamientoEjercicioId
         JOIN entrenamientos en2 ON en2.id = ee2.entrenamientoId
         WHERE ee2.ejercicioId = :ejercicioId AND en2.completado = 1 
         ORDER BY (COALESCE(s2.peso,0) * COALESCE(s2.repeticiones,0)) DESC, en2.fechaInicio DESC LIMIT 1) as fechaVolumenMaxSerie,
        SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as volumenTotal,
        COUNT(*) as seriesTotales,
        SUM(COALESCE(s.repeticiones,0)) as repeticionesTotales,
        COUNT(DISTINCT ee.entrenamientoId) as entrenamientosConEjercicio
    FROM series s
    JOIN entrenamiento_ejercicio ee
        ON ee.id = s.entrenamientoEjercicioId
    JOIN entrenamientos en
        ON en.id = ee.entrenamientoId
    WHERE ee.ejercicioId = :ejercicioId
      AND en.completado = 1
    """
    )
    fun getRecordsEjercicio(ejercicioId: Long): Flow<EjercicioRecords>

    @Query(
        """
    SELECT
        MAX(COALESCE(s.tiempo,0) * COALESCE(s.intensidad,1)) as mejorTiempo,
        (SELECT en2.fechaInicio 
         FROM series s2 
         JOIN entrenamiento_ejercicio ee2 ON ee2.id = s2.entrenamientoEjercicioId
         JOIN entrenamientos en2 ON en2.id = ee2.entrenamientoId
         WHERE ee2.ejercicioId = :ejercicioId AND en2.completado = 1 
         ORDER BY (COALESCE(s2.tiempo,0) * COALESCE(s2.intensidad,1)) DESC, en2.fechaInicio DESC LIMIT 1) as fechaMejorCarga,
        SUM(COALESCE(s.tiempo,0) * COALESCE(s.intensidad,1)) as tiempoTotal,
        COUNT(*) as seriesTotales,
        COUNT(DISTINCT ee.entrenamientoId) as entrenamientosConEjercicio
    FROM series s
    JOIN entrenamiento_ejercicio ee
        ON ee.id = s.entrenamientoEjercicioId
    JOIN entrenamientos en
        ON en.id = ee.entrenamientoId
    WHERE ee.ejercicioId = :ejercicioId
      AND en.completado = 1
    """
    )
    fun getRecordsEjercicioCardio(ejercicioId: Long): Flow<EjercicioRecordsCardio>

    @Query(
        """
    SELECT ee.id
    FROM entrenamiento_ejercicio ee
    JOIN entrenamientos e
    ON e.id = ee.entrenamientoId
    WHERE ee.ejercicioId = :ejercicioId
      AND e.completado = 1
    ORDER BY e.fechaInicio DESC
    LIMIT 1
    """
    )
    suspend fun getUltimoEntrenamientoEjercicioId(ejercicioId: Long): Long?

    @Query(
        """
    SELECT e.fechaInicio
    FROM entrenamiento_ejercicio ee
    JOIN entrenamientos e
    ON e.id = ee.entrenamientoId
    WHERE ee.id = :entrenamientoEjercicioId
    """
    )
    suspend fun getFechaEntrenamiento(entrenamientoEjercicioId: Long): Long?

    @Query(
        """
    SELECT e.nombre
    FROM entrenamiento_ejercicio ee
    JOIN entrenamientos e
    ON e.id = ee.entrenamientoId
    WHERE ee.id = :entrenamientoEjercicioId
    """
    )
    suspend fun getNombreEntrenamiento(entrenamientoEjercicioId: Long): String?

    @Query(
        """
    SELECT *
    FROM series
    WHERE entrenamientoEjercicioId = :entrenamientoEjercicioId
    ORDER BY id ASC
    """
    )
    suspend fun getSeriesEntrenamiento(entrenamientoEjercicioId: Long): List<SerieEntity>

    @Query(
        """
    SELECT 
        s.peso as pr,
        en.fechaInicio as fecha
    FROM series s
    JOIN entrenamiento_ejercicio ee ON ee.id = s.entrenamientoEjercicioId
    JOIN entrenamientos en ON en.id = ee.entrenamientoId
    WHERE ee.ejercicioId = :ejercicioId AND en.completado = 1
    ORDER BY s.peso DESC, en.fechaInicio DESC
    LIMIT 1
    """
    )
    fun getPR(ejercicioId: Long): Flow<PRRecord?>

    @Query(
        """
    SELECT mejorSesion, fecha
    FROM (
        SELECT 
            SUM(COALESCE(s.peso,0) * COALESCE(s.repeticiones,0)) as mejorSesion,
            en.fechaInicio as fecha
        FROM series s
        JOIN entrenamiento_ejercicio ee 
            ON ee.id = s.entrenamientoEjercicioId
        JOIN entrenamientos en
            ON en.id = ee.entrenamientoId
        WHERE ee.ejercicioId = :ejercicioId
          AND en.completado = 1
        GROUP BY ee.entrenamientoId
    )
    ORDER BY mejorSesion DESC, fecha DESC
    LIMIT 1
    """
    )
    fun getMejorSesionFuerza(ejercicioId: Long): Flow<MejorSesion?>

    @Query(
        """
    SELECT mejorSesion, fecha
    FROM (
        SELECT 
            SUM(COALESCE(s.tiempo,0) * COALESCE(s.intensidad,1)) as mejorSesion,
            en.fechaInicio as fecha
        FROM series s
        JOIN entrenamiento_ejercicio ee 
            ON ee.id = s.entrenamientoEjercicioId
        JOIN entrenamientos en
            ON en.id = ee.entrenamientoId
        WHERE ee.ejercicioId = :ejercicioId
          AND en.completado = 1
        GROUP BY ee.entrenamientoId
    )
    ORDER BY mejorSesion DESC, fecha DESC
    LIMIT 1
    """
    )
    fun getMejorSesionCardio(ejercicioId: Long): Flow<MejorSesion?>

    @Query(
        """
    SELECT 
        s.tiempo as tiempo,
        COALESCE(s.intensidad,1) as intensidad,
        (COALESCE(s.tiempo,0) * COALESCE(s.intensidad,1)) as carga,
        en.fechaInicio as fecha
    FROM series s
    JOIN entrenamiento_ejercicio ee 
        ON ee.id = s.entrenamientoEjercicioId
    JOIN entrenamientos en
        ON en.id = ee.entrenamientoId
    WHERE ee.ejercicioId = :ejercicioId
      AND en.completado = 1
    ORDER BY carga DESC, en.fechaInicio DESC
    LIMIT 1
    """
    )
    fun getMejorCargaCardio(ejercicioId: Long): Flow<MejorCargaCardio?>
}
