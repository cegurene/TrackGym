package com.example.gimnasio.ui.historico

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.EntrenamientoConEjerciciosYSeries
import com.example.gimnasio.data.sync.CloudSyncCoordinator
import kotlinx.coroutines.launch

class HistoricoDetailViewModel(context: Context) : ViewModel() {
    private val database = GymDatabase.getDatabase(context)
    private val cloudSyncCoordinator = CloudSyncCoordinator(context)
    private val dao = database.entrenamientoDao()
    private val serieDao = database.serieDao()

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

            entrenamiento.ejercicios
                .sortedBy { it.entrenamientoEjercicio.orden }
                .forEach { item ->
                    val entrenamientoEjercicioId = dao.insertEjercicioDeEntrenamiento(
                        EntrenamientoEjercicioEntity(
                            entrenamientoId = nuevoId,
                            ejercicioId = item.entrenamientoEjercicio.ejercicioId,
                            orden = item.entrenamientoEjercicio.orden
                        )
                    )

                    val esCardio = item.ejercicio.musculos.contains(Musculo.CARDIO)
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
                }

            onNavigate(nuevoId)
        }
    }

    fun renombrarEntrenamiento(id: Long, nuevoNombre: String) {
        viewModelScope.launch {
            dao.renombrarEntrenamiento(id, nuevoNombre)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun borrarEntrenamiento(id: Long) {
        viewModelScope.launch {
            dao.deleteEntrenamiento(id)
            cloudSyncCoordinator.syncNow()
        }
    }
}
