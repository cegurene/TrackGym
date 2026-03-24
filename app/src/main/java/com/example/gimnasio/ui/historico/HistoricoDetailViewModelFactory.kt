package com.example.gimnasio.ui.historico

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HistoricoDetailViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory  {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoricoDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoricoDetailViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
