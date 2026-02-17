package com.example.gimnasio.ui.historico

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EntrenamientoEntity
import kotlinx.coroutines.flow.Flow

class HistoricoViewModel(context: Context) : ViewModel() {

    private val entrenamientoDao =
        GymDatabase.getDatabase(context).entrenamientoDao()

    val entrenamientos =
        entrenamientoDao.getEntrenamientosCompletadosConRutina()
}
