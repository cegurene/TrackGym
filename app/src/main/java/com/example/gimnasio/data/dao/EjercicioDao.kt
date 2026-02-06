package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gimnasio.data.entity.EjercicioEntity
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
}
