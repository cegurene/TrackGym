package com.example.gimnasio.ui.entrenamiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.SerieEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EntrenamientoViewModel(
    application: Application,
    private val entrenamientoId: Long
) : AndroidViewModel(application) {

    private val database = GymDatabase.getDatabase(application)
    private val entrenamientoDao = database.entrenamientoDao()

    val ejerciciosDelEntrenamiento =
        entrenamientoDao.getEjerciciosConSeries(entrenamientoId)

    fun añadirSerie(entrenamientoEjercicioId: Long, peso: Float, repeticiones: Int) {
        viewModelScope.launch {
            entrenamientoDao.insertSerie(
                SerieEntity(
                    entrenamientoEjercicioId = entrenamientoEjercicioId,
                    peso = peso,
                    repeticiones = repeticiones,
                    completada = false
                )
            )
        }
    }

    fun actualizarPesoSerie(serieId: Long, peso: Float) {
        viewModelScope.launch { entrenamientoDao.updatePesoSerie(serieId, peso) }
    }

    fun actualizarRepsSerie(serieId: Long, reps: Int) {
        viewModelScope.launch { entrenamientoDao.updateRepsSerie(serieId, reps) }
    }

    fun eliminarSerie(serieId: Long) {
        viewModelScope.launch { entrenamientoDao.deleteSerie(serieId) }
    }

    fun eliminarEjercicio(entrenamientoEjercicioId: Long) {
        viewModelScope.launch { entrenamientoDao.deleteEjercicioDeEntrenamiento(entrenamientoEjercicioId) }
    }

    fun añadirEjercicioAlEntrenamiento(ejercicioId: Long) {
        viewModelScope.launch {
            val ejerciciosActuales = ejerciciosDelEntrenamiento.first()
            val nuevoOrden = ejerciciosActuales.size
            entrenamientoDao.insertEjercicioDeEntrenamiento(
                EntrenamientoEjercicioEntity(
                    entrenamientoId = entrenamientoId,
                    ejercicioId = ejercicioId,
                    orden = nuevoOrden
                )
            )
        }
    }

    fun marcarEjercicioCompletado(id: Long, completado: Boolean) {
        viewModelScope.launch { entrenamientoDao.actualizarEstadoEjercicio(id, completado) }
    }

    fun cancelarEntrenamiento(onCancelado: () -> Unit) {
        viewModelScope.launch {
            entrenamientoDao.deleteEntrenamiento(entrenamientoId)
            onCancelado()
        }
    }

    fun finalizarEntrenamiento(onFinalizado: () -> Unit) {
        viewModelScope.launch {
            entrenamientoDao.marcarComoCompletado(entrenamientoId, System.currentTimeMillis())
            onFinalizado()
        }
    }
}
