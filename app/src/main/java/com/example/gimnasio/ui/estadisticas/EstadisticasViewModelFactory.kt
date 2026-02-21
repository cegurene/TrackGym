package com.example.gimnasio.ui.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gimnasio.data.GymDatabase

class EstadisticasViewModelFactory(
    private val database: GymDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EstadisticasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EstadisticasViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}