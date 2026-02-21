package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.model.MusculoCount
import kotlinx.coroutines.flow.Flow

@Dao
interface EjercicioDao {

    // Crear un ejercicio (global, no ligado a rutina)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ejercicio: EjercicioEntity)

    // Listar TODOS los ejercicios
    @Query("SELECT * FROM ejercicios ORDER BY nombre ASC")
    fun getAll(): Flow<List<EjercicioEntity>>

    // Obtener un ejercicio por id (útil para detalles / progreso)
    @Query("SELECT * FROM ejercicios WHERE id = :id")
    suspend fun getById(id: Long): EjercicioEntity?

    // Borrar ejercicio (se borran relaciones por FK)
    @Query("DELETE FROM ejercicios WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE ejercicios SET nombre = :nuevoNombre WHERE id = :id")
    suspend fun updateNombre(id: Long, nuevoNombre: String)

    @Query("SELECT * FROM ejercicios WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<EjercicioEntity?>

    @Query("SELECT * FROM ejercicios WHERE musculos = :musculo ORDER BY nombre ASC")
    fun getEjerciciosPorMusculo(musculo: String): Flow<List<EjercicioEntity>>

    @Query("UPDATE ejercicios SET musculos = :musculos WHERE id = :id")
    suspend fun updateMusculos(id: Long, musculos: String)

    // 🔹 Total ejercicios
    @Query("SELECT COUNT(*) FROM ejercicios")
    fun getTotalEjerciciosFlow(): Flow<Int>

    // 🔹 Conteo por músculo
    @Query("""
    SELECT musculos as musculo, COUNT(*) as total
    FROM ejercicios
    GROUP BY musculos
""")
    fun getConteoPorMusculo(): Flow<List<MusculoCount>>
}
