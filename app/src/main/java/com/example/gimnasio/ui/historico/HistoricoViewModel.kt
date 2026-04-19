package com.example.gimnasio.ui.historico

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.model.EntrenamientoConRutinaYEjercicios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HistoricoViewModel(context: Context) : ViewModel() {

    private val database = GymDatabase.getDatabase(context)

    private val entrenamientoDao =
        database.entrenamientoDao()

    private val rutinaDao =
        database.rutinaDao()

    private val selectedRutinaIdFlow = MutableStateFlow<Long?>(null)

    val selectedRutinaId: StateFlow<Long?> = selectedRutinaIdFlow

    val rutinas = rutinaDao.getAllRutinas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val entrenamientos =
        entrenamientoDao.getEntrenamientosCompletadosConRutina()

    val entrenamientosFiltrados = combine(
        entrenamientos,
        selectedRutinaIdFlow
    ) { lista, rutinaIdSeleccionada ->
        if (rutinaIdSeleccionada == null) {
            lista
        } else {
            lista.filter { it.entrenamiento.rutinaId == rutinaIdSeleccionada }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<EntrenamientoConRutinaYEjercicios>()
    )

    fun seleccionarRutina(rutinaId: Long?) {
        selectedRutinaIdFlow.value = rutinaId
    }

    fun limpiarFiltroRutina() {
        selectedRutinaIdFlow.value = null
    }
}
