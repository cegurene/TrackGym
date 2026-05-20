package com.example.gimnasio.ui.historico

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.model.EntrenamientoConRutinaYEjercicios
import com.example.gimnasio.data.prefs.SortPreferences
import com.example.gimnasio.ui.rutinas.RutinaViewModel
import java.util.Calendar
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
    private val selectedFechaDesdeFlow = MutableStateFlow<Long?>(null)
    private val selectedFechaHastaFlow = MutableStateFlow<Long?>(null)

    private val sortPreferences = SortPreferences(context)
    private val _rutinaOrder = MutableStateFlow(sortPreferences.getRutinaOrder())

    val selectedRutinaId: StateFlow<Long?> = selectedRutinaIdFlow
    val selectedFechaDesde: StateFlow<Long?> = selectedFechaDesdeFlow
    val selectedFechaHasta: StateFlow<Long?> = selectedFechaHastaFlow

    // Helper privado para aplicar ordenamiento
    private fun sortRutinas(
        lista: List<RutinaEntity>,
        orden: RutinaViewModel.RutinaOrder,
        vecesMap: Map<Long, Int>
    ): List<RutinaEntity> {
        return when (orden) {
            RutinaViewModel.RutinaOrder.ALPHABETIC_ASC -> lista.sortedBy { it.nombre }
            RutinaViewModel.RutinaOrder.ALPHABETIC_DESC -> lista.sortedByDescending { it.nombre }
            RutinaViewModel.RutinaOrder.TIMES_DONE_DESC -> lista.sortedByDescending { vecesMap[it.id] ?: 0 }
            RutinaViewModel.RutinaOrder.TIMES_DONE_ASC -> lista.sortedBy { vecesMap[it.id] ?: 0 }
        }
    }

    // Obtener rutinas ordenadas según el estado actual
    val rutinas: StateFlow<List<RutinaEntity>> = combine(
        rutinaDao.getAllRutinas(),
        entrenamientoDao.getVecesRutinasFlow(),
        _rutinaOrder
    ) { todas, vecesRealizadas, orden ->
        val vecesMap = vecesRealizadas.associate { it.id to it.veces }
        sortRutinas(todas, orden, vecesMap)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val entrenamientos =
        entrenamientoDao.getEntrenamientosCompletadosConRutina()

    val entrenamientosFiltrados = combine(
        entrenamientos,
        selectedRutinaIdFlow,
        selectedFechaDesdeFlow,
        selectedFechaHastaFlow
    ) { lista, rutinaIdSeleccionada, fechaDesde, fechaHasta ->
        val inicioRango = fechaDesde?.let(::inicioDeDia)
        val finRango = fechaHasta?.let(::finDeDia)

        lista.filter { item ->
            val coincideRutina = rutinaIdSeleccionada == null ||
                item.entrenamiento.rutinaId == rutinaIdSeleccionada

            val fechaEntrenamiento = item.entrenamiento.fechaInicio
            val coincideFechaDesde = inicioRango == null || fechaEntrenamiento >= inicioRango
            val coincideFechaHasta = finRango == null || fechaEntrenamiento <= finRango

            coincideRutina && coincideFechaDesde && coincideFechaHasta
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList<EntrenamientoConRutinaYEjercicios>()
    )

    fun setRutinaOrder(orden: RutinaViewModel.RutinaOrder) {
        _rutinaOrder.value = orden
        sortPreferences.saveRutinaOrder(orden)
    }

    fun seleccionarRutina(rutinaId: Long?) {
        selectedRutinaIdFlow.value = rutinaId
    }

    fun limpiarFiltroRutina() {
        selectedRutinaIdFlow.value = null
    }

    fun seleccionarFechaDesde(millis: Long?) {
        selectedFechaDesdeFlow.value = millis
        val desde = millis
        val hasta = selectedFechaHastaFlow.value
        if (desde != null && hasta != null && desde > hasta) {
            selectedFechaHastaFlow.value = null
        }
    }

    fun seleccionarFechaHasta(millis: Long?) {
        selectedFechaHastaFlow.value = millis
        val desde = selectedFechaDesdeFlow.value
        val hasta = millis
        if (desde != null && hasta != null && hasta < desde) {
            selectedFechaDesdeFlow.value = null
        }
    }

    fun limpiarFiltroFechas() {
        selectedFechaDesdeFlow.value = null
        selectedFechaHastaFlow.value = null
    }

    private fun inicioDeDia(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun finDeDia(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
