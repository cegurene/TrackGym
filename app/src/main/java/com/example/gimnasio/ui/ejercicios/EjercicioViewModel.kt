package com.example.gimnasio.ui.ejercicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.dao.EjercicioDao
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.Musculo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyMap

class EjercicioViewModel(
    private val ejercicioDao: EjercicioDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val ejerciciosOriginal = ejercicioDao.getAll()

    // Lista reactiva filtrada
    val ejercicios = combine(
        ejerciciosOriginal,
        _searchQuery
    ) { lista, query ->
        if (query.isBlank()) {
            lista
        } else {
            lista.filter {
                it.nombre.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
}