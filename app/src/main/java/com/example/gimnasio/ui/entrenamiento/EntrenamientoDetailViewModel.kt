package com.example.gimnasio.ui.entrenamiento

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.model.EntrenamientoConEjerciciosYSeries
import kotlinx.coroutines.launch

class EntrenamientoDetailViewModel (context: Context) : ViewModel(){
    private val dao =
        GymDatabase.getDatabase(context).entrenamientoDao()

    fun getEntrenamiento(id: Long) =
        dao.getEntrenamientoConRutinaById(id)

    val entrenamientoActivo =
        dao.getEntrenamientoActivoFlow()

    fun getEntrenamientoCompleto(id: Long) =
        dao.getEntrenamientoCompleto(id)

    fun repetirEntrenamiento(
        entrenamiento: EntrenamientoConEjerciciosYSeries,
        onNavigate: (Long) -> Unit
    ) {
        viewModelScope.launch {

            val activo = dao.getEntrenamientoActivo()
            if (activo != null) return@launch

            val nuevoId = dao.insert(
                EntrenamientoEntity(
                    rutinaId = entrenamiento.entrenamiento.rutinaId,
                    fechaInicio = System.currentTimeMillis(),
                    nombre = entrenamiento.entrenamiento.nombre + " (Repetición)",
                    fechaFin = null,
                    completado = false
                )
            )

            onNavigate(nuevoId)
        }
    }

    fun renombrarEntrenamiento(id: Long, nuevoNombre: String) {
        viewModelScope.launch {
            dao.renombrarEntrenamiento(id, nuevoNombre)
        }
    }

    fun borrarEntrenamiento(id: Long) {
        viewModelScope.launch {
            dao.deleteEntrenamiento(id)
        }
    }
}