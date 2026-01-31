package com.example.gimnasio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gimnasio.data.dao.RutinaDao
import com.example.gimnasio.data.entity.RutinaEntity

@Database(entities = [RutinaEntity::class], version = 1, exportSchema = false)
abstract class GymDatabase : RoomDatabase() {

    abstract fun rutinaDao(): RutinaDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
