package com.example.gimnasio.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.model.RutinaConEjercicios
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(context: Context) : ViewModel() {

    private val rutinaDao = GymDatabase
        .getDatabase(context)
        .rutinaDao()

    private val database = GymDatabase.getDatabase(context)
    private val entrenamientoDao = database.entrenamientoDao()
    val entrenamientoActivo =
        entrenamientoDao.getEntrenamientoActivoFlow()


    val rutinas: StateFlow<List<RutinaConEjercicios>> =
        rutinaDao.getRutinasConEjercicios()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun crearRutina(nombre: String) {
        viewModelScope.launch {
            rutinaDao.insert(RutinaEntity(nombre = nombre))
        }
    }
}