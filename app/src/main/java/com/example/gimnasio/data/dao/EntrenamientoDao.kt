package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.EntrenamientoEntity
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

}
