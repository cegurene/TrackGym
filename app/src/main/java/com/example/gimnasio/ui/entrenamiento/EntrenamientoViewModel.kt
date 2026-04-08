package com.example.gimnasio.ui.entrenamiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.EntrenamientoEjercicioConSeries
import com.example.gimnasio.data.sync.CloudSyncCoordinator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EntrenamientoViewModel(
    application: Application,
    private val entrenamientoId: Long
) : AndroidViewModel(application) {

    private val database = GymDatabase.getDatabase(application)
    private val cloudSyncCoordinator = CloudSyncCoordinator(application)
    private val entrenamientoDao = database.entrenamientoDao()
    private val ejercicioDao = database.ejercicioDao()
    private val serieDao = database.serieDao()

    private val _validationErrorFlow = MutableSharedFlow<Pair<Long, String>>()
    val validationErrorFlow = _validationErrorFlow.asSharedFlow()

    val ejerciciosDelEntrenamiento =
        entrenamientoDao.getEjerciciosConSeries(entrenamientoId)

    val entrenamiento =
        entrenamientoDao.getEntrenamientoCompleto(entrenamientoId)

    private suspend fun sincronizarEstadoEjercicio(entrenamientoEjercicioId: Long) {
        val totalSeries = entrenamientoDao.countSeriesByEntrenamientoEjercicioId(entrenamientoEjercicioId)
        val seriesSinCompletar = entrenamientoDao.countSeriesSinCompletar(entrenamientoEjercicioId)
        val ejercicioCompletado = totalSeries > 0 && seriesSinCompletar == 0

        entrenamientoDao.actualizarEstadoEjercicio(
            entrenamientoEjercicioId,
            ejercicioCompletado
        )
    }

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

            sincronizarEstadoEjercicio(entrenamientoEjercicioId)
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
        viewModelScope.launch {
            val entrenamientoEjercicioId = entrenamientoDao
                .getEntrenamientoEjercicioIdBySerieId(serieId)

            entrenamientoDao.deleteSerie(serieId)

            if (entrenamientoEjercicioId != null) {
                sincronizarEstadoEjercicio(entrenamientoEjercicioId)
            }
        }
    }

    fun eliminarEjercicio(entrenamientoEjercicioId: Long) {
        viewModelScope.launch {
            entrenamientoDao.deleteEjercicioDeEntrenamiento(entrenamientoEjercicioId)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun añadirEjercicioAlEntrenamiento(ejercicioId: Long) {
        viewModelScope.launch {
            val nuevoOrden = (entrenamientoDao.getMaxOrden(entrenamientoId) ?: -1) + 1

            val esCardio = ejercicioDao
                .getById(ejercicioId)
                ?.musculos
                ?.contains(Musculo.CARDIO) == true

            val entrenamientoEjercicioId = entrenamientoDao.insertEjercicioDeEntrenamiento(
                EntrenamientoEjercicioEntity(
                    entrenamientoId = entrenamientoId,
                    ejercicioId = ejercicioId,
                    orden = nuevoOrden
                )
            )

            val serieInicial = if (esCardio) {
                SerieEntity(
                    entrenamientoEjercicioId = entrenamientoEjercicioId,
                    tiempo = 0,
                    intensidad = 0
                )
            } else {
                SerieEntity(
                    entrenamientoEjercicioId = entrenamientoEjercicioId,
                    peso = 0f,
                    repeticiones = 0
                )
            }

            serieDao.insert(serieInicial)
            sincronizarEstadoEjercicio(entrenamientoEjercicioId)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun moverEjercicio(
        actual: EntrenamientoEjercicioConSeries,
        otro: EntrenamientoEjercicioConSeries
    ) {
        viewModelScope.launch {
            entrenamientoDao.updateOrden(actual.entrenamientoEjercicio.id, otro.entrenamientoEjercicio.orden)
            entrenamientoDao.updateOrden(otro.entrenamientoEjercicio.id, actual.entrenamientoEjercicio.orden)
            cloudSyncCoordinator.syncNow()
        }
    }

     fun marcarSerieCompletada(serieId: Long, completada: Boolean, esCardio: Boolean, peso: Float?, reps: Int?, tiempo: Int?, intensidad: Int?) {
         viewModelScope.launch {
             if (completada) {
                 val esValida = if (esCardio) {
                     (tiempo ?: 0) > 0 && (intensidad ?: 0) > 0
                 } else {
                     (peso ?: 0f) > 0f && (reps ?: 0) > 0
                 }
 
                 if (!esValida) {
                     val entrenamientoEjercicioId = entrenamientoDao
                         .getEntrenamientoEjercicioIdBySerieId(serieId)
                         ?: return@launch
 
                     val errorMsg = if (esCardio) {
                         "Min e Intensidad deben ser > 0"
                     } else {
                         "Kg y Reps deben ser > 0"
                     }
 
                     _validationErrorFlow.emit(entrenamientoEjercicioId to errorMsg)
                     return@launch
                 }
             }
 
             entrenamientoDao.updateSerieCompletada(serieId, completada)
 
             val entrenamientoEjercicioId = entrenamientoDao
                 .getEntrenamientoEjercicioIdBySerieId(serieId)
                 ?: return@launch
 
             sincronizarEstadoEjercicio(entrenamientoEjercicioId)
         }
     }

    fun cancelarEntrenamiento(onCancelado: () -> Unit) {
        viewModelScope.launch {
            entrenamientoDao.deleteEntrenamiento(entrenamientoId)
            cloudSyncCoordinator.syncNow()
            onCancelado()
        }
    }

    fun finalizarEntrenamiento(onFinalizado: () -> Unit) {
        viewModelScope.launch {
            entrenamientoDao.marcarComoCompletado(entrenamientoId, System.currentTimeMillis())
            cloudSyncCoordinator.syncNow()
            onFinalizado()
        }
    }
}
