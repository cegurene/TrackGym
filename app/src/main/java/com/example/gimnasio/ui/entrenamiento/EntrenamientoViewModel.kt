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
import androidx.room.withTransaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EntrenamientoViewModel(
    application: Application,
    private val entrenamientoId: Long
) : AndroidViewModel(application) {

    private val database = GymDatabase.getDatabase(application)
    private val cloudSyncCoordinator = CloudSyncCoordinator(application)
    private val entrenamientoDao = database.entrenamientoDao()
    private val ejercicioDao = database.ejercicioDao()
    private val serieDao = database.serieDao()

    private data class PendingSerieUpdate(
        var peso: Float? = null,
        var reps: Int? = null,
        var tiempo: Int? = null,
        var intensidad: Int? = null
    )

    private val pendingSerieUpdates = mutableMapOf<Long, PendingSerieUpdate>()
    private val pendingUpdateVersions = mutableMapOf<Long, Long>()
    private val pendingUpdatesMutex = Mutex()

    private val _validationErrorFlow = MutableSharedFlow<Pair<Long, String>>()
    val validationErrorFlow = _validationErrorFlow.asSharedFlow()

    val ejerciciosDelEntrenamiento =
        entrenamientoDao.getEjerciciosConSeries(entrenamientoId)

    val fechaInicio =
        entrenamientoDao.getFechaInicioFlow(entrenamientoId)

    private suspend fun sincronizarEstadoEjercicio(
        entrenamientoEjercicioId: Long
    ) {
        entrenamientoDao.recalcularCompletadoEjercicio(
            entrenamientoEjercicioId
        )
    }

    private fun programarGuardado(serieId: Long) {
        val version = (pendingUpdateVersions[serieId] ?: 0L) + 1L
        pendingUpdateVersions[serieId] = version

        viewModelScope.launch {
            delay(400)

            if (pendingUpdateVersions[serieId] == version) {
                guardarCambiosPendientes(serieId)
            }
        }
    }

    private suspend fun guardarCambiosPendientes(serieId: Long) {
        pendingUpdatesMutex.withLock {
            val cambios = pendingSerieUpdates.remove(serieId)
                ?: return@withLock

            database.withTransaction {
                cambios.peso?.let { peso ->
                    entrenamientoDao.updatePesoSerie(serieId, peso)
                }

                cambios.reps?.let { reps ->
                    entrenamientoDao.updateRepsSerie(serieId, reps)
                }

                cambios.tiempo?.let { tiempo ->
                    serieDao.actualizarTiempo(serieId, tiempo)
                }

                cambios.intensidad?.let { intensidad ->
                    serieDao.actualizarIntensidad(serieId, intensidad)
                }
            }
        }
    }

    private suspend fun guardarTodosLosCambiosPendientes() {
        while (pendingSerieUpdates.isNotEmpty()) {
            val ids = pendingSerieUpdates.keys.toList()

            ids.forEach { serieId ->
                val version = (pendingUpdateVersions[serieId] ?: 0L) + 1L
                pendingUpdateVersions[serieId] = version

                guardarCambiosPendientes(serieId)
            }
        }
    }

    private suspend fun guardarCambiosPendientesDeSerie(serieId: Long) {
        val version = (pendingUpdateVersions[serieId] ?: 0L) + 1L
        pendingUpdateVersions[serieId] = version

        guardarCambiosPendientes(serieId)
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
        val cambios = pendingSerieUpdates.getOrPut(serieId) {
            PendingSerieUpdate()
        }

        cambios.peso = peso
        programarGuardado(serieId)
    }

    fun actualizarRepsSerie(serieId: Long, reps: Int) {
        val cambios = pendingSerieUpdates.getOrPut(serieId) {
            PendingSerieUpdate()
        }

        cambios.reps = reps
        programarGuardado(serieId)
    }

    fun actualizarTiempoSerie(serieId: Long, tiempo: Int) {
        val cambios = pendingSerieUpdates.getOrPut(serieId) {
            PendingSerieUpdate()
        }

        cambios.tiempo = tiempo
        programarGuardado(serieId)
    }

    fun actualizarIntensidadSerie(serieId: Long, intensidad: Int) {
        val cambios = pendingSerieUpdates.getOrPut(serieId) {
            PendingSerieUpdate()
        }

        cambios.intensidad = intensidad
        programarGuardado(serieId)
    }

    fun eliminarSerie(serieId: Long) {
        viewModelScope.launch {
            guardarCambiosPendientesDeSerie(serieId)

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
            guardarTodosLosCambiosPendientes()

            entrenamientoDao.deleteEjercicioDeEntrenamiento(
                entrenamientoEjercicioId
            )

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

    @Suppress("UNUSED_PARAMETER")
    fun marcarSerieCompletada(
        serieId: Long,
        completada: Boolean,
        esCardio: Boolean,
        peso: Float?,
        reps: Int?,
        tiempo: Int?,
        intensidad: Int?
    ) {
        viewModelScope.launch {

            // Guardar primero cualquier modificación pendiente de esta serie
            guardarCambiosPendientesDeSerie(serieId)

            // Obtener los datos actualizados directamente de Room
            val ejercicios = ejerciciosDelEntrenamiento.first()
            val serie = ejercicios
                .flatMap { it.series }
                .firstOrNull { it.id == serieId }
                ?: return@launch

            // Validar utilizando los valores actuales de la base de datos
            if (completada) {
                val esValida = if (esCardio) {
                    (serie.tiempo ?: 0) > 0 &&
                            (serie.intensidad ?: 0) > 0
                } else {
                    (serie.peso ?: 0f) > 0f &&
                            (serie.repeticiones ?: 0) > 0
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

                    _validationErrorFlow.emit(
                        entrenamientoEjercicioId to errorMsg
                    )
                    return@launch
                }
            }

            val entrenamientoEjercicioId = entrenamientoDao
                .getEntrenamientoEjercicioIdBySerieId(serieId)
                ?: return@launch

            // Actualizar la serie y el ejercicio en una única transacción
            database.withTransaction {
                entrenamientoDao.updateSerieCompletada(
                    serieId,
                    completada
                )

                entrenamientoDao.recalcularCompletadoEjercicio(
                    entrenamientoEjercicioId
                )
            }
        }
    }

    fun guardarAntesDeSalir(onGuardado: () -> Unit) {
        viewModelScope.launch {
            guardarTodosLosCambiosPendientes()
            onGuardado()
        }
    }

    fun cancelarEntrenamiento(onCancelado: () -> Unit) {
        viewModelScope.launch {
            guardarTodosLosCambiosPendientes()

            entrenamientoDao.deleteEntrenamiento(entrenamientoId)
            cloudSyncCoordinator.syncNow()
            onCancelado()
        }
    }

    fun finalizarEntrenamiento(
        onFinalizado: () -> Unit,
        onEjerciciosSinSeries: (List<String>) -> Unit = {},
        onSinEjercicios: () -> Unit = {}
    ) {
        viewModelScope.launch {

            guardarTodosLosCambiosPendientes()

            // Comprobar el estado más reciente de los ejercicios y sus series
            val ejercicios = ejerciciosDelEntrenamiento.first()

            // Impedir finalizar un entrenamiento sin ejercicios
            if (ejercicios.isEmpty()) {
                onSinEjercicios()
                return@launch
            }

            // Comprobar si algún ejercicio no tiene ninguna serie
            val ejerciciosSinSeries = ejercicios
                .filter { it.series.isEmpty() }
                .map { it.ejercicio.nombre }

            // Si hay ejercicios sin series, impedir la finalización
            if (ejerciciosSinSeries.isNotEmpty()) {
                onEjerciciosSinSeries(ejerciciosSinSeries)
                return@launch
            }

            // Finalización normal
            val rutinaId = entrenamientoDao.getRutinaIdByEntrenamientoId(entrenamientoId)

            entrenamientoDao.marcarComoCompletado(
                entrenamientoId,
                System.currentTimeMillis()
            )

            if (rutinaId != null) {
                val nombreRutina = entrenamientoDao.getNombreRutinaById(rutinaId)
                    ?: "Entrenamiento"

                val numeroEntrenamiento =
                    entrenamientoDao.countCompletadosByRutinaId(rutinaId)

                entrenamientoDao.renombrarEntrenamiento(
                    entrenamientoId,
                    "${nombreRutina.trim()} -> $numeroEntrenamiento"
                )
            }

            cloudSyncCoordinator.syncNow()
            onFinalizado()
        }
    }
}
