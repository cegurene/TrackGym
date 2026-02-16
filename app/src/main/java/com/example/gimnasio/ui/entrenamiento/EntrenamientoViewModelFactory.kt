package com.example.gimnasio.ui.entrenamiento

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EntrenamientoViewModelFactory(
    private val application: Application,
    private val entrenamientoId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntrenamientoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EntrenamientoViewModel(application, entrenamientoId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
