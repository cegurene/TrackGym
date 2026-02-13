package com.example.gimnasio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gimnasio.data.dao.*
import com.example.gimnasio.data.entity.*

@Database(
    entities = [
        RutinaEntity::class,
        EjercicioEntity::class,
        RutinaEjercicioEntity::class,
        EntrenamientoEntity::class,
        SerieEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {

    abstract fun rutinaDao(): RutinaDao
    abstract fun ejercicioDao(): EjercicioDao
    abstract fun rutinaEjercicioDao(): RutinaEjercicioDao
    abstract fun entrenamientoDao(): EntrenamientoDao
    abstract fun serieDao(): SerieDao

    companion object {

        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
