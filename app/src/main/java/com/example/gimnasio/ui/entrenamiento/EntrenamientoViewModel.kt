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
    private val serieDao = database.serieDao()

    val ejerciciosDelEntrenamiento =
        entrenamientoDao.getEjerciciosConSeries(entrenamientoId)

    val entrenamiento =
        entrenamientoDao.getEntrenamientoCompleto(entrenamientoId)

    fun añadirSerie(entrenamientoEjercicioId: Long, esCardio: Boolean) {
        viewModelScope.launch {
            if (esCardio) {
                serieDao.insert(
                    SerieEntity(
                        entrenamientoEjercicioId = entrenamientoEjercicioId,
                        tiempo = 0,
                        intensidad = 0
                    )
                )
            } else {
                serieDao.insert(
                    SerieEntity(
                        entrenamientoEjercicioId = entrenamientoEjercicioId,
                        peso = 0f,
                        repeticiones = 0
                    )
                )
            }
        }
    }

    fun actualizarPesoSerie(serieId: Long, peso: Float) {
        viewModelScope.launch { entrenamientoDao.updatePesoSerie(serieId, peso) }
    }

    fun actualizarRepsSerie(serieId: Long, reps: Int) {
        viewModelScope.launch { entrenamientoDao.updateRepsSerie(serieId, reps) }
    }

    fun actualizarTiempoSerie(idSerie: Long, tiempo: Int) {
        viewModelScope.launch {
            serieDao.actualizarTiempo(idSerie, tiempo)
        }
    }

    fun actualizarIntensidadSerie(idSerie: Long, intensidad: Int) {
        viewModelScope.launch {
            serieDao.actualizarIntensidad(idSerie, intensidad)
        }
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
