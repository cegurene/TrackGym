package com.example.gimnasio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gimnasio.data.dao.EjercicioDao
import com.example.gimnasio.data.dao.EntrenamientoDao
import com.example.gimnasio.data.dao.RutinaDao
import com.example.gimnasio.data.dao.SerieDao
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.entity.SerieEntity


@Database(
    entities = [
        RutinaEntity::class,
        EjercicioEntity::class,
        EntrenamientoEntity::class,
        SerieEntity::class
    ],
    version = 2,
    exportSchema = false
)

abstract class GymDatabase : RoomDatabase() {

    abstract fun rutinaDao(): RutinaDao
    abstract fun ejercicioDao(): EjercicioDao
    abstract fun entrenamientoDao(): EntrenamientoDao
    abstract fun serieDao(): SerieDao


    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: Context): GymDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
