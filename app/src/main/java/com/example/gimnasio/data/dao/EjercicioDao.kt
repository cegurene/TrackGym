package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.EjercicioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EjercicioDao {

    @Insert
    suspend fun insert(ejercicio: EjercicioEntity)

    @Query("""
        SELECT * FROM ejercicios 
        WHERE rutinaId = :rutinaId 
        ORDER BY nombre ASC
    """)
    fun getEjerciciosDeRutina(rutinaId: Long): Flow<List<EjercicioEntity>>
}
