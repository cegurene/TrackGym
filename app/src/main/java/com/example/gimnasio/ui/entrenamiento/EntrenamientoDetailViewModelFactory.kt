package com.example.gimnasio.ui.entrenamiento

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EntrenamientoDetailViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory  {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntrenamientoDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EntrenamientoDetailViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}