package com.example.gimnasio.ui.historico

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SerieHistoricaBorrador(
    val serie: SerieEntity,
    val original: SerieEntity?,
    val esNueva: Boolean = false,
    val eliminada: Boolean = false
)

class HistoricoDetailViewModel(context: Context) : ViewModel() {
    enum class NombreOperacionResultado {
        OK,
        VACIO,
        DUPLICADO
    }

    private val database = GymDatabase.getDatabase(context)
    private val cloudSyncCoordinator = CloudSyncCoordinator(context)
    private val dao = database.entrenamientoDao()
    private val serieDao = database.serieDao()
    private val rutinaDao = database.rutinaDao()
    private val ejercicioDao = database.ejercicioDao()

    private val _borradoresSeries =
        MutableStateFlow<Map<Long, SerieHistoricaBorrador>>(emptyMap())

    val borradoresSeries: StateFlow<Map<Long, SerieHistoricaBorrador>> =
        _borradoresSeries.asStateFlow()

    private var siguienteIdTemporal = -1L

    val ejerciciosDisponibles = ejercicioDao.getAll()

    val rutinas = rutinaDao.getAllRutinas()

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

            // Replica la secuencia real del histórico, aunque difiera de la rutina base.
            val ejerciciosOrdenados = entrenamiento.ejercicios
                .sortedBy { it.entrenamientoEjercicio.orden }

            ejerciciosOrdenados.forEachIndexed { index, item ->
                val entrenamientoEjercicioId = dao.insertEjercicioDeEntrenamiento(
                    EntrenamientoEjercicioEntity(
                        entrenamientoId = nuevoId,
                        ejercicioId = item.entrenamientoEjercicio.ejercicioId,
                        orden = index
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

    fun renombrarEntrenamiento(
        id: Long,
        nuevoNombre: String,
        onResult: (NombreOperacionResultado) -> Unit = {}
    ) {
        val nombreNormalizado = nuevoNombre.trim()
        if (nombreNormalizado.isBlank()) {
            onResult(NombreOperacionResultado.VACIO)
            return
        }

        viewModelScope.launch {
            try {
                if (dao.existsByNombreExcludingId(nombreNormalizado, id)) {
                    onResult(NombreOperacionResultado.DUPLICADO)
                    return@launch
                }

                dao.renombrarEntrenamiento(id, nombreNormalizado)
                cloudSyncCoordinator.syncNow()
                onResult(NombreOperacionResultado.OK)
            } catch (_: SQLiteConstraintException) {
                onResult(NombreOperacionResultado.DUPLICADO)
            }
        }
    }

    fun borrarEntrenamiento(id: Long) {
        viewModelScope.launch {
            dao.deleteEntrenamiento(id)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun cambiarRutina(
        entrenamientoId: Long,
        nuevaRutinaId: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val entrenamiento = dao.getEntrenamientoEntityById(entrenamientoId)

                if (entrenamiento == null || !entrenamiento.completado) {
                    onResult(false)
                    return@launch
                }

                val rutinaAnteriorId = entrenamiento.rutinaId

                if (rutinaAnteriorId == nuevaRutinaId) {
                    onResult(true)
                    return@launch
                }

                // La existencia de la rutina se comprueba mediante su nombre.
                val nuevoNombreRutina = dao.getNombreRutinaById(nuevaRutinaId)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

                if (nuevoNombreRutina == null) {
                    onResult(false)
                    return@launch
                }

                database.withTransaction {
                    // Solo se modifica la asociación del entrenamiento.
                    // No se tocan los ejercicios ni las series.
                    dao.updateRutinaId(
                        entrenamientoId = entrenamientoId,
                        nuevaRutinaId = nuevaRutinaId
                    )

                    // Renumerar la rutina anterior.
                    val nombreRutinaAnterior = dao.getNombreRutinaById(rutinaAnteriorId)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Entrenamiento"

                    val entrenamientosAnteriores =
                        dao.getCompletadosByRutinaId(rutinaAnteriorId)

                    entrenamientosAnteriores.forEachIndexed { index, item ->
                        dao.renombrarEntrenamiento(
                            item.id,
                            "$nombreRutinaAnterior -> ${index + 1}"
                        )
                    }

                    // Renumerar la rutina nueva.
                    val entrenamientosNuevos =
                        dao.getCompletadosByRutinaId(nuevaRutinaId)

                    entrenamientosNuevos.forEachIndexed { index, item ->
                        dao.renombrarEntrenamiento(
                            item.id,
                            "$nuevoNombreRutina -> ${index + 1}"
                        )
                    }
                }

                cloudSyncCoordinator.syncNow()
                onResult(true)

            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun actualizarDuracionEntrenamiento(
        entrenamientoId: Long,
        horas: Long,
        minutos: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (horas < 0 || minutos !in 0..59) {
                    onResult(false)
                    return@launch
                }

                val entrenamiento = dao.getEntrenamientoEntityById(entrenamientoId)

                if (entrenamiento == null || !entrenamiento.completado) {
                    onResult(false)
                    return@launch
                }

                val milisegundosPorHora = 3_600_000L
                val milisegundosPorMinuto = 60_000L

                if (horas > Long.MAX_VALUE / milisegundosPorHora) {
                    onResult(false)
                    return@launch
                }

                val horasMillis = horas * milisegundosPorHora
                val minutosMillis = minutos * milisegundosPorMinuto

                if (horasMillis > Long.MAX_VALUE - minutosMillis) {
                    onResult(false)
                    return@launch
                }

                val duracionMillis = horasMillis + minutosMillis

                if (entrenamiento.fechaInicio > Long.MAX_VALUE - duracionMillis) {
                    onResult(false)
                    return@launch
                }

                val nuevaFechaFin = entrenamiento.fechaInicio + duracionMillis

                dao.updateFechaFin(
                    entrenamientoId = entrenamientoId,
                    nuevaFechaFin = nuevaFechaFin
                )

                cloudSyncCoordinator.syncNow()
                onResult(true)

            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun anadirEjercicioHistorico(
        entrenamientoId: Long,
        ejercicioId: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val entrenamiento = dao.getEntrenamientoEntityById(entrenamientoId)

                if (entrenamiento == null || !entrenamiento.completado) {
                    onResult(false)
                    return@launch
                }

                val ejercicio = ejercicioDao.getById(ejercicioId)

                if (ejercicio == null || ejercicio.eliminado) {
                    onResult(false)
                    return@launch
                }

                val entrenamientoCompleto =
                    dao.getEntrenamientoCompleto(entrenamientoId).first()

                if (entrenamientoCompleto == null ||
                    entrenamientoCompleto.ejercicios.any {
                        it.entrenamientoEjercicio.ejercicioId == ejercicioId
                    }
                ) {
                    onResult(false)
                    return@launch
                }

                database.withTransaction {
                    val nuevoOrden = (dao.getMaxOrden(entrenamientoId) ?: -1) + 1

                    val nuevoEjercicioId = dao.insertEjercicioDeEntrenamiento(
                        EntrenamientoEjercicioEntity(
                            entrenamientoId = entrenamientoId,
                            ejercicioId = ejercicioId,
                            orden = nuevoOrden,
                            completado = true,
                            anadidoPosteriormente = true
                        )
                    )

                    val esCardio = ejercicio.musculos.contains(Musculo.CARDIO)

                    serieDao.insert(
                        SerieEntity(
                            entrenamientoEjercicioId = nuevoEjercicioId,
                            peso = if (esCardio) null else 0f,
                            repeticiones = if (esCardio) null else 0,
                            tiempo = if (esCardio) 0 else null,
                            intensidad = if (esCardio) 0 else null,
                            completada = true
                        )
                    )
                }

                cloudSyncCoordinator.syncNow()
                onResult(true)

            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun eliminarEjercicioHistorico(
        entrenamientoId: Long,
        entrenamientoEjercicioId: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val entrenamiento = dao.getEntrenamientoEntityById(entrenamientoId)

                if (entrenamiento == null || !entrenamiento.completado) {
                    onResult(false)
                    return@launch
                }

                val entrenamientoCompleto =
                    dao.getEntrenamientoCompleto(entrenamientoId).first()

                val perteneceAlEntrenamiento =
                    entrenamientoCompleto?.ejercicios?.any {
                        it.entrenamientoEjercicio.id == entrenamientoEjercicioId
                    } == true

                if (!perteneceAlEntrenamiento) {
                    onResult(false)
                    return@launch
                }

                dao.deleteEjercicioDeEntrenamiento(entrenamientoEjercicioId)

                cloudSyncCoordinator.syncNow()
                onResult(true)

            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    private suspend fun seriePerteneceAlEntrenamiento(
        entrenamientoId: Long,
        serieId: Long
    ): Boolean {
        val entrenamiento = dao.getEntrenamientoEntityById(entrenamientoId)

        if (entrenamiento == null || !entrenamiento.completado) {
            return false
        }

        val entrenamientoEjercicioId =
            dao.getEntrenamientoEjercicioIdBySerieId(serieId)
                ?: return false

        val entrenamientoCompleto =
            dao.getEntrenamientoCompleto(entrenamientoId).first()
                ?: return false

        return entrenamientoCompleto.ejercicios.any { ejercicio ->
            ejercicio.entrenamientoEjercicio.id == entrenamientoEjercicioId
        }
    }

    private suspend fun obtenerSerieBorrador(
        entrenamientoId: Long,
        serieId: Long
    ): SerieHistoricaBorrador? {
        val borradorExistente = _borradoresSeries.value[serieId]

        if (borradorExistente != null) {
            return borradorExistente
        }

        if (!seriePerteneceAlEntrenamiento(entrenamientoId, serieId)) {
            return null
        }

        val serieOriginal = serieDao.getSerieById(serieId)
            ?: return null

        return SerieHistoricaBorrador(
            serie = serieOriginal,
            original = serieOriginal
        )
    }

    private suspend fun actualizarSeriePendiente(
        entrenamientoId: Long,
        serieId: Long,
        cambio: (SerieEntity) -> SerieEntity,
        onResult: (Boolean) -> Unit
    ) {
        try {
            val borrador = obtenerSerieBorrador(
                entrenamientoId = entrenamientoId,
                serieId = serieId
            )

            if (borrador == null || borrador.eliminada) {
                onResult(false)
                return
            }

            val nuevaSerie = cambio(borrador.serie)

            _borradoresSeries.value =
                _borradoresSeries.value + (
                        serieId to borrador.copy(serie = nuevaSerie)
                        )

            onResult(true)

        } catch (_: Exception) {
            onResult(false)
        }
    }

    fun anadirSerieHistorico(
        entrenamientoId: Long,
        entrenamientoEjercicioId: Long,
        esCardio: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val entrenamiento =
                    dao.getEntrenamientoEntityById(entrenamientoId)

                if (entrenamiento == null || !entrenamiento.completado) {
                    onResult(false)
                    return@launch
                }

                val entrenamientoCompleto =
                    dao.getEntrenamientoCompleto(entrenamientoId).first()

                val ejercicioPertenece = entrenamientoCompleto
                    ?.ejercicios
                    ?.any {
                        it.entrenamientoEjercicio.id == entrenamientoEjercicioId
                    } == true

                if (!ejercicioPertenece) {
                    onResult(false)
                    return@launch
                }

                val idTemporal = siguienteIdTemporal--

                val serie = if (esCardio) {
                    SerieEntity(
                        id = idTemporal,
                        entrenamientoEjercicioId = entrenamientoEjercicioId,
                        tiempo = 0,
                        intensidad = 0,
                        completada = true
                    )
                } else {
                    SerieEntity(
                        id = idTemporal,
                        entrenamientoEjercicioId = entrenamientoEjercicioId,
                        peso = 0f,
                        repeticiones = 0,
                        completada = true
                    )
                }

                _borradoresSeries.value =
                    _borradoresSeries.value + (
                            idTemporal to SerieHistoricaBorrador(
                                serie = serie,
                                original = null,
                                esNueva = true
                            )
                            )

                onResult(true)

            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun actualizarPesoSerieHistorica(
        entrenamientoId: Long,
        serieId: Long,
        peso: Float,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (peso < 0f || !peso.isFinite()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            actualizarSeriePendiente(
                entrenamientoId,
                serieId,
                { it.copy(peso = peso) },
                onResult
            )
        }
    }

    fun actualizarRepsSerieHistorica(
        entrenamientoId: Long,
        serieId: Long,
        repeticiones: Int,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (repeticiones < 0) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            actualizarSeriePendiente(
                entrenamientoId,
                serieId,
                { it.copy(repeticiones = repeticiones) },
                onResult
            )
        }
    }

    fun actualizarTiempoSerieHistorica(
        entrenamientoId: Long,
        serieId: Long,
        tiempo: Int,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (tiempo < 0) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            actualizarSeriePendiente(
                entrenamientoId,
                serieId,
                { it.copy(tiempo = tiempo) },
                onResult
            )
        }
    }

    fun actualizarIntensidadSerieHistorica(
        entrenamientoId: Long,
        serieId: Long,
        intensidad: Int,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (intensidad < 0) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            actualizarSeriePendiente(
                entrenamientoId,
                serieId,
                { it.copy(intensidad = intensidad) },
                onResult
            )
        }
    }

    fun eliminarSerieHistorica(
        entrenamientoId: Long,
        serieId: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val borrador = _borradoresSeries.value[serieId]
                    ?: obtenerSerieBorrador(entrenamientoId, serieId)

                if (borrador == null || borrador.eliminada) {
                    onResult(false)
                    return@launch
                }

                val entrenamientoEjercicioId =
                    borrador.serie.entrenamientoEjercicioId

                val entrenamientoCompleto =
                    dao.getEntrenamientoCompleto(entrenamientoId).first()

                val seriesOriginales = entrenamientoCompleto
                    ?.ejercicios
                    ?.firstOrNull {
                        it.entrenamientoEjercicio.id == entrenamientoEjercicioId
                    }
                    ?.series
                    .orEmpty()

                val borradoresEjercicio = _borradoresSeries.value.values
                    .filter {
                        it.serie.entrenamientoEjercicioId == entrenamientoEjercicioId
                    }

                val eliminadasExistentes = borradoresEjercicio.count {
                    !it.esNueva && it.eliminada
                }

                val nuevasNoEliminadas = borradoresEjercicio.count {
                    it.esNueva && !it.eliminada
                }

                val eliminandoNueva = borrador.esNueva
                val eliminandoExistente =
                    !borrador.esNueva && !borrador.eliminada

                val totalFinal =
                    seriesOriginales.size -
                            eliminadasExistentes -
                            (if (eliminandoExistente) 1 else 0) +
                            nuevasNoEliminadas -
                            (if (eliminandoNueva) 1 else 0)

                if (totalFinal < 1) {
                    onResult(false)
                    return@launch
                }

                if (borrador.esNueva) {
                    _borradoresSeries.value -= serieId
                } else {
                    _borradoresSeries.value += (
                                                    serieId to borrador.copy(eliminada = true)
                                                    )
                }

                onResult(true)

            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun descartarModificacionesSeries() {
        _borradoresSeries.value = emptyMap()
    }

    fun guardarModificacionesSeries(
        entrenamientoId: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val borradores = _borradoresSeries.value.values.toList()

                if (borradores.isEmpty()) {
                    onResult(true)
                    return@launch
                }

                val entrenamiento =
                    dao.getEntrenamientoEntityById(entrenamientoId)

                if (entrenamiento == null || !entrenamiento.completado) {
                    onResult(false)
                    return@launch
                }

                val entrenamientoCompleto =
                    dao.getEntrenamientoCompleto(entrenamientoId).first()
                        ?: run {
                            onResult(false)
                            return@launch
                        }

                val idsEjercicios = entrenamientoCompleto.ejercicios.map {
                    it.entrenamientoEjercicio.id
                }.toSet()

                // Comprobar que todas las series pertenecen a este entrenamiento.
                if (borradores.any {
                        it.serie.entrenamientoEjercicioId !in idsEjercicios
                    }
                ) {
                    onResult(false)
                    return@launch
                }

                // Validar los valores numéricos.
                val valoresValidos = borradores.all { borrador ->
                    val serie = borrador.serie

                    (serie.peso == null ||
                            (serie.peso >= 0f && serie.peso.isFinite())) &&
                            (serie.repeticiones == null || serie.repeticiones >= 0) &&
                            (serie.tiempo == null || serie.tiempo >= 0) &&
                            (serie.intensidad == null || serie.intensidad >= 0)
                }

                if (!valoresValidos) {
                    onResult(false)
                    return@launch
                }

                // Evitar que un ejercicio se quede sin series.
                val quedanSeriesEnTodos = entrenamientoCompleto.ejercicios.all { ejercicio ->
                    val idEjercicio = ejercicio.entrenamientoEjercicio.id

                    val cambiosEjercicio = borradores.filter {
                        it.serie.entrenamientoEjercicioId == idEjercicio
                    }

                    val eliminadas = cambiosEjercicio.count {
                        !it.esNueva && it.eliminada
                    }

                    val nuevas = cambiosEjercicio.count {
                        it.esNueva && !it.eliminada
                    }

                    ejercicio.series.size - eliminadas + nuevas >= 1
                }

                if (!quedanSeriesEnTodos) {
                    onResult(false)
                    return@launch
                }

                database.withTransaction {
                    val entrenamientoActual =
                        dao.getEntrenamientoEntityById(entrenamientoId)

                    if (entrenamientoActual == null ||
                        !entrenamientoActual.completado
                    ) {
                        throw IllegalStateException(
                            "El entrenamiento ya no está disponible."
                        )
                    }

                    borradores.forEach { borrador ->
                        val serie = borrador.serie

                        when {
                            borrador.esNueva && !borrador.eliminada -> {
                                serieDao.insert(
                                    serie.copy(id = 0)
                                )
                            }

                            !borrador.esNueva && borrador.eliminada -> {
                                dao.deleteSerie(serie.id)
                            }

                            !borrador.esNueva -> {
                                serie.peso?.let {
                                    dao.updatePesoSerie(serie.id, it)
                                }

                                serie.repeticiones?.let {
                                    dao.updateRepsSerie(serie.id, it)
                                }

                                serie.tiempo?.let {
                                    serieDao.actualizarTiempo(serie.id, it)
                                }

                                serie.intensidad?.let {
                                    serieDao.actualizarIntensidad(serie.id, it)
                                }
                            }
                        }
                    }
                }

                cloudSyncCoordinator.syncNow()
                _borradoresSeries.value = emptyMap()

                onResult(true)

            } catch (_: Exception) {
                onResult(false)
            }
        }
    }
}
