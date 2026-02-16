package com.example.gimnasio.ui.entrenamiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.SerieEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class EntrenamientoViewModel(
    application: Application,
    private val entrenamientoId: Long
) : AndroidViewModel(application) {

    private val database = GymDatabase.getDatabase(application)
    private val entrenamientoDao = database.entrenamientoDao()

    val ejerciciosDelEntrenamiento =
        entrenamientoDao.getEjerciciosConSeries(entrenamientoId)

    fun añadirSerie(
        entrenamientoEjercicioId: Long,
        peso: Float,
        repeticiones: Int
    ) {
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

    fun marcarSerieCompletada(
        serieId: Long,
        completada: Boolean
    ) {
        viewModelScope.launch {
            entrenamientoDao.updateSerieCompletada(
                serieId = serieId,
                completada = completada
            )
        }
    }

    fun actualizarPesoSerie(serieId: Long, peso: Float) {
        viewModelScope.launch {
            entrenamientoDao.updatePesoSerie(serieId, peso)
        }
    }

    fun actualizarRepsSerie(serieId: Long, reps: Int) {
        viewModelScope.launch {
            entrenamientoDao.updateRepsSerie(serieId, reps)
        }
    }


}
