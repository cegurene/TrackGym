package com.example.gimnasio.ui.ejercicios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.dao.EjercicioDao
import com.example.gimnasio.data.dao.SerieDao
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.model.UltimaSesionEjercicio
import com.example.gimnasio.data.sync.CloudSyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyMap

class EjercicioViewModel(
    application: Application,
    private val ejercicioDao: EjercicioDao,
    private val serieDao: SerieDao
) : AndroidViewModel(application) {

    private val cloudSyncCoordinator = CloudSyncCoordinator(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val ejerciciosOriginal = ejercicioDao.getAll()

    private val _selectedMusculos = MutableStateFlow<Set<Musculo>>(emptySet())
    val selectedMusculos = _selectedMusculos

    // Lista reactiva filtrada
    val ejercicios = combine(
        ejerciciosOriginal,
        _searchQuery,
        _selectedMusculos
    ) { lista, query, musculosSeleccionados ->

        lista.filter { ejercicio ->

            val coincideNombre =
                query.isBlank() || ejercicio.nombre.contains(query, ignoreCase = true)

            val coincideMusculo =
                musculosSeleccionados.isEmpty() ||
                        ejercicio.musculos.any { it in musculosSeleccionados }

            coincideNombre && coincideMusculo
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleMusculo(musculo: Musculo) {
        _selectedMusculos.value =
            if (_selectedMusculos.value.contains(musculo)) {
                _selectedMusculos.value - musculo
            } else {
                _selectedMusculos.value + musculo
            }
    }

    fun clearMusculos() {
        _selectedMusculos.value = emptySet()
    }

    // ---------- TU CÓDIGO ORIGINAL ----------

    fun crearEjercicio(nombre: String, musculos: List<Musculo>) {
        if (nombre.isBlank()) return

        val musculoPrincipal = musculos.firstOrNull() ?: return

        viewModelScope.launch {
            ejercicioDao.insert(
                EjercicioEntity(
                    nombre = nombre.trim(),
                    musculos = listOf(musculoPrincipal)
                )
            )
            cloudSyncCoordinator.syncNow()
        }
    }

    fun borrarEjercicio(id: Long) {
        viewModelScope.launch {
            ejercicioDao.delete(id)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun renombrarEjercicio(id: Long, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return

        viewModelScope.launch {
            ejercicioDao.updateNombre(
                id = id,
                nuevoNombre = nuevoNombre.trim()
            )
            cloudSyncCoordinator.syncNow()
        }
    }

    fun actualizarMusculos(id: Long, musculos: List<Musculo>) {
        val musculoPrincipal = musculos.firstOrNull() ?: return

        viewModelScope.launch {
            ejercicioDao.updateMusculos(id, musculoPrincipal.name)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun getEjercicio(id: Long) = ejercicioDao
        .getByIdFlow(id)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val estadisticasMusculos = ejercicios
        .map { lista ->
            Musculo.values().associateWith { musculo ->
                lista.count { ejercicio ->
                    ejercicio.musculos.contains(musculo)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    fun getProgresoEjercicio(ejercicioId: Long) =
        serieDao.getProgresoEjercicio(ejercicioId)

    fun getProgresoEjercicioCardio(ejercicioId: Long) =
        serieDao.getProgresoEjercicioCardio(ejercicioId)

    fun getRecordsEjercicio(ejercicioId: Long) =
        serieDao.getRecordsEjercicio(ejercicioId)

    fun getRecordsEjercicioCardio(ejercicioId: Long) =
        serieDao.getRecordsEjercicioCardio(ejercicioId)

    suspend fun getUltimaSesion(ejercicioId: Long): UltimaSesionEjercicio? {

        val entrenamientoEjercicioId =
            serieDao.getUltimoEntrenamientoEjercicioId(ejercicioId)
                ?: return null

        val fecha =
            serieDao.getFechaEntrenamiento(entrenamientoEjercicioId)
                ?: return null

        val series =
            serieDao.getSeriesEntrenamiento(entrenamientoEjercicioId)

        return UltimaSesionEjercicio(
            fecha = fecha,
            series = series
        )
    }

    fun getUltimaSesionFlow(ejercicioId: Long) =
        kotlinx.coroutines.flow.flow {

            val entrenamientoEjercicioId =
                serieDao.getUltimoEntrenamientoEjercicioId(ejercicioId)

            if (entrenamientoEjercicioId == null) {
                emit(null)
                return@flow
            }

            val fecha =
                serieDao.getFechaEntrenamiento(entrenamientoEjercicioId)
                    ?: return@flow emit(null)

            val series =
                serieDao.getSeriesEntrenamiento(entrenamientoEjercicioId)

            emit(
                UltimaSesionEjercicio(
                    fecha = fecha,
                    series = series
                )
            )
        }

    fun actualizarComentario(ejercicioId: Long, comentario: String?) {
        viewModelScope.launch {
            ejercicioDao.actualizarComentario(ejercicioId, comentario ?: "")
            cloudSyncCoordinator.syncNow()
        }
    }

    // Nuevo helper para obtener comentario actual de un ejercicio
    fun getComentario(ejercicioId: Long) =
        ejercicioDao.getByIdFlow(ejercicioId)
            .map { it?.comentario ?: "" }

    fun getPR(ejercicioId: Long) = serieDao.getPR(ejercicioId)

    fun getMejorSesionFuerza(ejercicioId: Long) = serieDao.getMejorSesionFuerza(ejercicioId)

    fun getMejorSesionCardio(ejercicioId: Long) = serieDao.getMejorSesionCardio(ejercicioId)

    fun getMejorCargaCardio(ejercicioId: Long) = serieDao.getMejorCargaCardio(ejercicioId)
}