package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.RutinaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RutinaDao {
    @Insert
    suspend fun insert(rutina: RutinaEntity)

    @Query("SELECT * FROM rutinas ORDER BY nombre ASC")
    fun getAll(): Flow<List<RutinaEntity>>
}
