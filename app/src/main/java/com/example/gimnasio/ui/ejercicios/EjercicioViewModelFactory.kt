package com.example.gimnasio.ui.ejercicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gimnasio.data.GymDatabase

class EjercicioViewModelFactory(
    private val database: GymDatabase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EjercicioViewModel::class.java)) {
            return EjercicioViewModel(
                ejercicioDao = database.ejercicioDao(),
                serieDao = database.serieDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
