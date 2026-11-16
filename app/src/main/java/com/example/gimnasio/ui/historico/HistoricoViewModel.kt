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

    private val _limiteVisible = MutableStateFlow(tamanioPagina)

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

                // Reiniciar la lista cuando cambien los filtros.
                _limiteVisible.value = tamanioPagina
                _entrenamientosFiltrados.value = emptyList()
                _totalEntrenamientosFiltrados.value = 0
                _hayMasEntrenamientos.value = false
                _cargandoMasEntrenamientos.value = false

                // Mantener observación continua de la página y del total.
                _limiteVisible.collectLatest { limite ->
                    _cargandoMasEntrenamientos.value = limite > tamanioPagina

                    try {
                        combine(
                            entrenamientoDao.getTotalEntrenamientosCompletadosFiltrados(
                                rutinaId = filtros.rutinaId,
                                fechaDesde = filtros.fechaDesde,
                                fechaHasta = filtros.fechaHasta
                            ),
                            entrenamientoDao.getEntrenamientosCompletadosPaginados(
                                rutinaId = filtros.rutinaId,
                                fechaDesde = filtros.fechaDesde,
                                fechaHasta = filtros.fechaHasta,
                                limite = limite,
                                desplazamiento = 0
                            )
                        ) { total, lista ->
                            total to lista
                        }.collect { (total, lista) ->
                            if (filtros == filtrosActuales) {
                                _totalEntrenamientosFiltrados.value = total
                                _entrenamientosFiltrados.value = lista
                                _hayMasEntrenamientos.value = lista.size < total
                                _cargandoMasEntrenamientos.value = false
                            }
                        }
                    } finally {
                        _cargandoMasEntrenamientos.value = false
                    }
                }
            }
        }
    }

    fun cargarMasEntrenamientos() {
        if (_cargandoMasEntrenamientos.value ||
            !_hayMasEntrenamientos.value
        ) {
            return
        }

        _limiteVisible.value += tamanioPagina
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
