package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gimnasio.data.entity.SerieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SerieDao {

    @Insert
    suspend fun insert(serie: SerieEntity)

    @Query("""
        SELECT * FROM series
        WHERE entrenamientoEjercicioId = :ejercicioId
        ORDER BY id DESC
    """)
    fun getSeriesPorEjercicio(ejercicioId: Long): Flow<List<SerieEntity>>
}
