package com.example.gimnasio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity

@Dao
interface EntrenamientoEjercicioDao {

    @Insert
    suspend fun insertAll(lista: List<EntrenamientoEjercicioEntity>)
}
