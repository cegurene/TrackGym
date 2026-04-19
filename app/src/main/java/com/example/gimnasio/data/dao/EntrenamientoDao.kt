package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.EntrenamientoConEjerciciosYSeries
import com.example.gimnasio.data.model.EntrenamientoConRutina
import com.example.gimnasio.data.model.EntrenamientoConRutinaYEjercicios
import com.example.gimnasio.data.model.EntrenamientoEjercicioConSeries
import com.example.gimnasio.data.model.RutinaVeces
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

    @Query("SELECT MAX(orden) FROM entrenamiento_ejercicio WHERE entrenamientoId = :entrenamientoId")
    suspend fun getMaxOrden(entrenamientoId: Long): Int?

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

    @Query("DELETE FROM series WHERE id = :serieId")
    suspend fun deleteSerie(serieId: Long)

    @Query("SELECT entrenamientoEjercicioId FROM series WHERE id = :serieId")
    suspend fun getEntrenamientoEjercicioIdBySerieId(serieId: Long): Long?

    @Query("SELECT COUNT(*) FROM series WHERE entrenamientoEjercicioId = :entrenamientoEjercicioId")
    suspend fun countSeriesByEntrenamientoEjercicioId(entrenamientoEjercicioId: Long): Int

    @Query("""
    SELECT COUNT(*) FROM series
    WHERE entrenamientoEjercicioId = :entrenamientoEjercicioId
      AND completada = 0
""")
    suspend fun countSeriesSinCompletar(entrenamientoEjercicioId: Long): Int

    @Query("DELETE FROM entrenamiento_ejercicio WHERE id = :entrenamientoEjercicioId")
    suspend fun deleteEjercicioDeEntrenamiento(entrenamientoEjercicioId: Long)

    @Query("UPDATE entrenamiento_ejercicio SET orden = :nuevoOrden WHERE id = :entrenamientoEjercicioId")
    suspend fun updateOrden(entrenamientoEjercicioId: Long, nuevoOrden: Int)

    @Query("""
    UPDATE entrenamientos
    SET completado = 1,
        fechaFin = :fechaFin
    WHERE id = :entrenamientoId
""")
    suspend fun finalizarEntrenamiento(
        entrenamientoId: Long,
        fechaFin: Long
    )

    @Query("DELETE FROM entrenamientos WHERE id = :entrenamientoId")
    suspend fun deleteEntrenamiento(entrenamientoId: Long)

    @Insert
    suspend fun insertEjercicioDeEntrenamiento(
        ejercicio: EntrenamientoEjercicioEntity
    ): Long

    @Query("""
    UPDATE entrenamientos
    SET completado = 1,
        fechaFin = :fechaFin
    WHERE id = :entrenamientoId
""")
    suspend fun marcarComoCompletado(
        entrenamientoId: Long,
        fechaFin: Long
    )

    @Query("""
    SELECT * FROM entrenamientos
    WHERE completado = 1
    ORDER BY fechaInicio DESC
""")
    fun getEntrenamientosCompletados(): Flow<List<EntrenamientoEntity>>

    @Transaction
    @Query("""
    SELECT * FROM entrenamientos
    WHERE completado = 1
    ORDER BY fechaInicio DESC
""")
    fun getEntrenamientosCompletadosConRutina():
            Flow<List<EntrenamientoConRutinaYEjercicios>>

    @Transaction
    @Query("SELECT * FROM entrenamientos WHERE id = :id")
    fun getEntrenamientoConRutinaById(
        id: Long
    ): Flow<EntrenamientoConRutina?>

    @Transaction
    @Query("SELECT * FROM entrenamientos WHERE id = :id")
    fun getEntrenamientoCompleto(
        id: Long
    ): Flow<EntrenamientoConEjerciciosYSeries?>

    @Query("""
    UPDATE entrenamiento_ejercicio
    SET completado = :completado
    WHERE id = :id
""")
    suspend fun actualizarEstadoEjercicio(id: Long, completado: Boolean)

    @Query("UPDATE entrenamientos SET nombre = :nuevoNombre WHERE id = :id")
    suspend fun renombrarEntrenamiento(id: Long, nuevoNombre: String)

    @Query("SELECT COUNT(*) > 0 FROM entrenamientos WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre))")
    suspend fun existsByNombre(nombre: String): Boolean

    @Query("SELECT COUNT(*) > 0 FROM entrenamientos WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre)) AND id != :id")
    suspend fun existsByNombreExcludingId(nombre: String, id: Long): Boolean

    @Query("SELECT rutinaId FROM entrenamientos WHERE id = :entrenamientoId")
    suspend fun getRutinaIdByEntrenamientoId(entrenamientoId: Long): Long?

    @Query("SELECT nombre FROM rutinas WHERE id = :rutinaId")
    suspend fun getNombreRutinaById(rutinaId: Long): String?

    @Query("SELECT COUNT(*) FROM entrenamientos WHERE rutinaId = :rutinaId AND completado = 1")
    suspend fun countCompletadosByRutinaId(rutinaId: Long): Int

    @Query("SELECT COUNT(*) FROM entrenamientos WHERE completado = 1")
    fun getTotalEntrenamientosFlow(): Flow<Int>

    @Query("""
    SELECT MAX(fechaFin - fechaInicio)
    FROM entrenamientos
    WHERE fechaFin IS NOT NULL
      AND completado = 1
""")
    suspend fun getEntrenamientoMasLargo(): Long?

    @Query("""
    SELECT MIN(fechaFin - fechaInicio)
    FROM entrenamientos
    WHERE fechaFin IS NOT NULL
      AND completado = 1
""")
    suspend fun getEntrenamientoMasCorto(): Long?

    @Query("""
    SELECT COUNT(*) FROM entrenamientos
    WHERE completado = 1
    AND fechaInicio >= strftime('%s','now','-7 day') * 1000
    """)
    suspend fun getEntrenamientosUltimaSemana(): Int


    @Query("""
    SELECT COUNT(*) FROM entrenamientos
    WHERE completado = 1
    AND fechaInicio >= strftime('%s','now','-30 day') * 1000
    """)
    suspend fun getEntrenamientosUltimoMes(): Int

    @Query("""
    SELECT SUM(fechaFin - fechaInicio)
    FROM entrenamientos
    WHERE fechaFin IS NOT NULL
      AND completado = 1
    """)
    suspend fun getTiempoTotalEntrenado(): Long?

    @Query("""
    SELECT AVG(fechaFin - fechaInicio)
    FROM entrenamientos
    WHERE fechaFin IS NOT NULL
      AND completado = 1
    """)
    suspend fun getDuracionMedia(): Double?

    @Query("""
    SELECT r.nombre
    FROM entrenamientos e
    INNER JOIN rutinas r ON r.id = e.rutinaId
    WHERE e.completado = 1
    GROUP BY r.id
    ORDER BY COUNT(*) DESC
    LIMIT 1
    """)
    suspend fun getRutinaMasUsada(): String?

    @Query("""
    SELECT r.id, r.nombre, COUNT(e.id) as veces
    FROM entrenamientos e
    INNER JOIN rutinas r ON r.id = e.rutinaId
    WHERE e.completado = 1
    GROUP BY r.id
    ORDER BY veces DESC
    """)
    suspend fun getVecesRutinas(): List<RutinaVeces>

    @Query("""
    SELECT r.id, r.nombre, COUNT(e.id) as veces
    FROM rutinas r
    LEFT JOIN entrenamientos e ON e.rutinaId = r.id AND e.completado = 1
    GROUP BY r.id
""")
    fun getVecesRutinasFlow(): Flow<List<RutinaVeces>>
}
