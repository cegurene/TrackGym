package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.EntrenamientoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntrenamientoDao {

    @Insert
    suspend fun insert(entrenamiento: EntrenamientoEntity)

    @Query("""
        SELECT * FROM entrenamientos 
        WHERE rutinaId = :rutinaId 
        ORDER BY fecha DESC
    """)
    fun getEntrenamientosDeRutina(rutinaId: Long): Flow<List<EntrenamientoEntity>>
}
