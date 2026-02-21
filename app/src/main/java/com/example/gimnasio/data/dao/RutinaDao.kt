package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.model.RutinaConEjercicios
import kotlinx.coroutines.flow.Flow

@Dao
interface RutinaDao {

    // Crear rutina
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rutina: RutinaEntity): Long

    // Lista simple de rutinas (sin ejercicios)
    @Query("SELECT * FROM rutinas ORDER BY nombre ASC")
    fun getAllRutinas(): Flow<List<RutinaEntity>>

    // 🔥 Rutinas con sus ejercicios (N–M)
    @Transaction
    @Query("SELECT * FROM rutinas ORDER BY nombre ASC")
    fun getRutinasConEjercicios(): Flow<List<RutinaConEjercicios>>

    @Query("SELECT * FROM rutinas WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<RutinaEntity?>

    @Query("DELETE FROM rutinas WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE rutinas SET nombre = :nuevoNombre WHERE id = :id")
    suspend fun updateNombre(id: Long, nuevoNombre: String)

    @Transaction
    @Query("SELECT * FROM rutinas WHERE id = :id")
    fun getRutinaConEjercicios(id: Long): Flow<RutinaConEjercicios?>

    @Query("SELECT COUNT(*) FROM rutinas")
    fun getTotalRutinasFlow(): Flow<Int>
}
