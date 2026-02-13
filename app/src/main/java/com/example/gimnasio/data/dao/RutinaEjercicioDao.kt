package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.RutinaEjercicioEntity
import com.example.gimnasio.data.model.EjercicioConOrden
import com.example.gimnasio.data.model.RutinaConEjercicios
import kotlinx.coroutines.flow.Flow

@Dao
interface RutinaEjercicioDao {

    // Añadir un ejercicio a una rutina
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: RutinaEjercicioEntity)

    // Quitar un ejercicio de una rutina
    @Query("""
        DELETE FROM rutina_ejercicio
        WHERE rutinaId = :rutinaId AND ejercicioId = :ejercicioId
    """)
    suspend fun delete(rutinaId: Long, ejercicioId: Long)

    // Borrar todos los ejercicios de una rutina
    @Query("DELETE FROM rutina_ejercicio WHERE rutinaId = :rutinaId")
    suspend fun deleteByRutina(rutinaId: Long)

    @Query("SELECT MAX(orden) FROM rutina_ejercicio WHERE rutinaId = :rutinaId")
    suspend fun getMaxOrden(rutinaId: Long): Int?

    @Query("""
    SELECT e.* FROM ejercicios e
    INNER JOIN rutina_ejercicio re
        ON e.id = re.ejercicioId
    WHERE re.rutinaId = :rutinaId
    ORDER BY re.orden ASC
    """)
    fun getEjerciciosOrdenados(rutinaId: Long): Flow<List<EjercicioEntity>>

    @Query("""
    UPDATE rutina_ejercicio
    SET orden = :nuevoOrden
    WHERE rutinaId = :rutinaId
    AND ejercicioId = :ejercicioId
""")
    suspend fun updateOrden(
        rutinaId: Long,
        ejercicioId: Long,
        nuevoOrden: Int
    )

    @Query("""
    SELECT e.*, re.orden FROM ejercicios e
    INNER JOIN rutina_ejercicio re
        ON e.id = re.ejercicioId
    WHERE re.rutinaId = :rutinaId
    ORDER BY re.orden ASC
""")
    fun getEjerciciosConOrden(rutinaId: Long): Flow<List<EjercicioConOrden>>


}
