package com.example.gimnasio.data.converter

import androidx.room.TypeConverter
import com.example.gimnasio.data.entity.Musculo

class MusculoConverter {

    @TypeConverter
    fun fromMusculoList(musculos: List<Musculo>): String {
        return musculos.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toMusculoList(data: String): List<Musculo> {
        if (data.isEmpty()) return emptyList()
        return data.split(",").map { Musculo.valueOf(it) }
    }
}