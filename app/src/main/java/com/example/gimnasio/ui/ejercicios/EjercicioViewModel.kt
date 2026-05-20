package com.example.gimnasio.ui.ejercicios

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.dao.EjercicioDao
import com.example.gimnasio.data.dao.SerieDao
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.model.UltimaSesionEjercicio
import com.example.gimnasio.data.prefs.SortPreferences
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

    enum class NombreOperacionResultado {
        OK,
        VACIO,
        DUPLICADO
    }

    enum class EjercicioOrder {
        ALPHABETIC_ASC,
        ALPHABETIC_DESC,
        MUSCLE_ASC,
        MUSCLE_DESC
    }

    enum class DetailExerciseOrder {
        ALPHABETIC_ASC,
        ALPHABETIC_DESC,
        MUSCLE_ASC,
        MUSCLE_DESC
    }

    private val cloudSyncCoordinator = CloudSyncCoordinator(application)
    private val sortPreferences = SortPreferences(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val ejerciciosOriginal = ejercicioDao.getAll()

    private val _selectedMusculos = MutableStateFlow<Set<Musculo>>(emptySet())
    val selectedMusculos = _selectedMusculos

    private val _order = MutableStateFlow(sortPreferences.getEjercicioOrder())
    val order = _order

    // Helper privado para aplicar ordenamiento
    private fun sortEjercicios(lista: List<EjercicioEntity>, orden: EjercicioOrder): List<EjercicioEntity> {
        return when (orden) {
            EjercicioOrder.ALPHABETIC_ASC -> lista.sortedBy { it.nombre }
            EjercicioOrder.ALPHABETIC_DESC -> lista.sortedByDescending { it.nombre }
            EjercicioOrder.MUSCLE_ASC -> {
                lista.sortedWith(
                    compareBy<EjercicioEntity> { it.musculos.firstOrNull()?.ordinal ?: Int.MAX_VALUE }
                        .thenBy { it.nombre }
                )
            }
            EjercicioOrder.MUSCLE_DESC -> {
                lista.sortedWith(
                    compareByDescending<EjercicioEntity> { it.musculos.firstOrNull()?.ordinal ?: -1 }
                        .thenBy { it.nombre }
                )
            }
        }
    }

    // Lista reactiva filtrada
    val ejercicios = combine(
        ejerciciosOriginal,
        _searchQuery,
        _selectedMusculos,
        _order
    ) { lista, query, musculosSeleccionados, orden ->

        lista.filter { ejercicio ->

            val coincideNombre =
                query.isBlank() || ejercicio.nombre.contains(query, ignoreCase = true)

            val coincideMusculo =
                musculosSeleccionados.isEmpty() ||
                        ejercicio.musculos.any { it in musculosSeleccionados }

            coincideNombre && coincideMusculo
        }.let { filtrados ->
            sortEjercicios(filtrados, orden)
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Método para obtener ejercicios sin filtros de búsqueda/músculos pero ordenados según estado actual
    fun getEjerciciosOrdenados(ejercicios: List<EjercicioEntity>): List<EjercicioEntity> {
        return sortEjercicios(ejercicios, _order.value)
    }

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

    fun onOrderChange(newOrder: EjercicioOrder) {
        _order.value = newOrder
        sortPreferences.saveEjercicioOrder(newOrder)
    }

    // ---------- TU CÓDIGO ORIGINAL ----------

    fun crearEjercicio(
        nombre: String,
        musculos: List<Musculo>,
        onResult: (NombreOperacionResultado) -> Unit = {}
    ) {
        val nombreNormalizado = nombre.trim()
        if (nombreNormalizado.isBlank()) {
            onResult(NombreOperacionResultado.VACIO)
            return
        }

        val musculoPrincipal = musculos.firstOrNull() ?: return

        viewModelScope.launch {
            try {
                if (ejercicioDao.existsByNombre(nombreNormalizado)) {
                    onResult(NombreOperacionResultado.DUPLICADO)
                    return@launch
                }

                ejercicioDao.insert(
                    EjercicioEntity(
                        nombre = nombreNormalizado,
                        musculos = listOf(musculoPrincipal)
                    )
                )
                cloudSyncCoordinator.syncNow()
                onResult(NombreOperacionResultado.OK)
            } catch (_: SQLiteConstraintException) {
                onResult(NombreOperacionResultado.DUPLICADO)
            }
        }
    }

    fun borrarEjercicio(id: Long) {
        viewModelScope.launch {
            ejercicioDao.delete(id)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun renombrarEjercicio(
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
                if (ejercicioDao.existsByNombreExcludingId(nombreNormalizado, id)) {
                    onResult(NombreOperacionResultado.DUPLICADO)
                    return@launch
                }

                ejercicioDao.updateNombre(
                    id = id,
                    nuevoNombre = nombreNormalizado
                )
                cloudSyncCoordinator.syncNow()
                onResult(NombreOperacionResultado.OK)
            } catch (_: SQLiteConstraintException) {
                onResult(NombreOperacionResultado.DUPLICADO)
            }
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

        val nombreEntrenamiento =
            serieDao.getNombreEntrenamiento(entrenamientoEjercicioId)
                ?: "Entrenamiento"

        val series =
            serieDao.getSeriesEntrenamiento(entrenamientoEjercicioId)

        return UltimaSesionEjercicio(
            fecha = fecha,
            nombreEntrenamiento = nombreEntrenamiento,
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

            val nombreEntrenamiento =
                serieDao.getNombreEntrenamiento(entrenamientoEjercicioId)
                    ?: "Entrenamiento"

            val series =
                serieDao.getSeriesEntrenamiento(entrenamientoEjercicioId)

            emit(
                UltimaSesionEjercicio(
                    fecha = fecha,
                    nombreEntrenamiento = nombreEntrenamiento,
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