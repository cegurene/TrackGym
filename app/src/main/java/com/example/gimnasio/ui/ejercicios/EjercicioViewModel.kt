package com.example.gimnasio.ui.ejercicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.dao.EjercicioDao
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.Musculo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EjercicioViewModel(
    private val ejercicioDao: EjercicioDao
) : ViewModel() {

    // Lista reactiva de ejercicios
    val ejercicios = ejercicioDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Crear ejercicio
    fun crearEjercicio(nombre: String, musculos: List<Musculo>) {
        if (nombre.isBlank()) return

        viewModelScope.launch {
            ejercicioDao.insert(
                EjercicioEntity(
                    nombre = nombre.trim(),
                    musculos = musculos,
                )
            )
        }
    }

    // Borrar ejercicio
    fun borrarEjercicio(id: Long) {
        viewModelScope.launch {
            ejercicioDao.delete(id)
        }
    }

    fun renombrarEjercicio(id: Long, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return

        viewModelScope.launch {
            ejercicioDao.updateNombre(
                id = id,
                nuevoNombre = nuevoNombre.trim()
            )
        }
    }

    fun getEjercicio(id: Long) = ejercicioDao
        .getByIdFlow(id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

}
