package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.gimnasio.data.entity.RutinaEjercicioEntity
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
}
