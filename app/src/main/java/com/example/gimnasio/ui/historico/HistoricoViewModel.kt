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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    private data class FiltrosHistorico(
        val rutinaId: Long?,
        val fechaDesde: Long?,
        val fechaHasta: Long?
    )

    private val tamanioPagina = 20

    private val filtrosHistorico = combine(
        selectedRutinaIdFlow,
        selectedFechaDesdeFlow,
        selectedFechaHastaFlow
    ) { rutinaId, fechaDesde, fechaHasta ->
        FiltrosHistorico(
            rutinaId = rutinaId,
            fechaDesde = fechaDesde?.let(::inicioDeDia),
            fechaHasta = fechaHasta?.let(::finDeDia)
        )
    }.distinctUntilChanged()

    private var filtrosActuales = FiltrosHistorico(
        rutinaId = null,
        fechaDesde = null,
        fechaHasta = null
    )

    private val _entrenamientosFiltrados =
        MutableStateFlow<List<EntrenamientoConRutinaYEjercicios>>(emptyList())

    val entrenamientosFiltrados: StateFlow<List<EntrenamientoConRutinaYEjercicios>> =
        _entrenamientosFiltrados

    private val _totalEntrenamientosFiltrados = MutableStateFlow(0)

    val totalEntrenamientosFiltrados: StateFlow<Int> =
        _totalEntrenamientosFiltrados

    private val _hayMasEntrenamientos = MutableStateFlow(false)

    val hayMasEntrenamientos: StateFlow<Boolean> =
        _hayMasEntrenamientos

    private val _cargandoMasEntrenamientos = MutableStateFlow(false)

    val cargandoMasEntrenamientos: StateFlow<Boolean> =
        _cargandoMasEntrenamientos

    val totalEntrenamientos =
        entrenamientoDao.getTotalEntrenamientosFlow()

    init {
        viewModelScope.launch {
            filtrosHistorico.collectLatest { filtros ->
                filtrosActuales = filtros

                _entrenamientosFiltrados.value = emptyList()
                _totalEntrenamientosFiltrados.value = 0
                _hayMasEntrenamientos.value = false
                _cargandoMasEntrenamientos.value = false

                val total = entrenamientoDao
                    .getTotalEntrenamientosCompletadosFiltrados(
                        rutinaId = filtros.rutinaId,
                        fechaDesde = filtros.fechaDesde,
                        fechaHasta = filtros.fechaHasta
                    )
                    .first()

                _totalEntrenamientosFiltrados.value = total

                val primeraPagina = entrenamientoDao
                    .getEntrenamientosCompletadosPaginados(
                        rutinaId = filtros.rutinaId,
                        fechaDesde = filtros.fechaDesde,
                        fechaHasta = filtros.fechaHasta,
                        limite = tamanioPagina,
                        desplazamiento = 0
                    )
                    .first()

                _entrenamientosFiltrados.value = primeraPagina
                _hayMasEntrenamientos.value =
                    primeraPagina.size < total
            }
        }
    }

    fun cargarMasEntrenamientos() {
        if (_cargandoMasEntrenamientos.value ||
            !_hayMasEntrenamientos.value
        ) {
            return
        }

        viewModelScope.launch {
            _cargandoMasEntrenamientos.value = true

            try {
                val filtros = filtrosActuales
                val desplazamiento = _entrenamientosFiltrados.value.size

                val siguientePagina = entrenamientoDao
                    .getEntrenamientosCompletadosPaginados(
                        rutinaId = filtros.rutinaId,
                        fechaDesde = filtros.fechaDesde,
                        fechaHasta = filtros.fechaHasta,
                        limite = tamanioPagina,
                        desplazamiento = desplazamiento
                    )
                    .first()

                // Evitar añadir resultados si los filtros han cambiado
                // mientras se estaba recuperando la página.
                if (filtros == filtrosActuales) {
                    val listaActual = _entrenamientosFiltrados.value

                    _entrenamientosFiltrados.value =
                        listaActual + siguientePagina

                    _hayMasEntrenamientos.value =
                        _entrenamientosFiltrados.value.size <
                                _totalEntrenamientosFiltrados.value
                }
            } finally {
                _cargandoMasEntrenamientos.value = false
            }
        }
    }

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
