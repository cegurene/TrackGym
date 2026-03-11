package com.example.gimnasio.ui.ejercicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.dao.EjercicioDao
import com.example.gimnasio.data.dao.SerieDao
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.model.UltimaSesionEjercicio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyMap

class EjercicioViewModel(
    private val ejercicioDao: EjercicioDao,
    private val serieDao: SerieDao
) : ViewModel() {

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

        viewModelScope.launch {
            ejercicioDao.insert(
                EjercicioEntity(
                    nombre = nombre.trim(),
                    musculos = musculos
                )
            )
        }
    }

    fun borrarEjercicio(id: Long) {
        viewModelScope.launch {
            ejercicioDao.delete(id)
        }
    }

    fun renombrarEjercicio(id: Long, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return

        viewModelScope.launch {
            ejercicioDao.updateNombre(
                id = id,
                nuevoNombre = nuevoNombre.trim()
            )
        }
    }

    fun actualizarMusculos(id: Long, musculos: List<Musculo>) {
        viewModelScope.launch {
            val musculosStr = musculos.joinToString(",") { it.name }
            ejercicioDao.updateMusculos(id, musculosStr)
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
}