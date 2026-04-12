package com.example.gimnasio.ui.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.model.DiaVolumen
import com.example.gimnasio.data.model.RutinaVeces
import com.example.gimnasio.data.model.SerieRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EstadisticasViewModel(
    database: GymDatabase
) : ViewModel() {

    private val ejercicioDao = database.ejercicioDao()
    private val rutinaDao = database.rutinaDao()
    private val entrenamientoDao = database.entrenamientoDao()
    private val serieDao = database.serieDao()

    private val _tiempoCardioTotal = MutableStateFlow("")
    val tiempoCardioTotal: StateFlow<String> = _tiempoCardioTotal

    // 1️⃣ Distribución por músculo
    val distribucionMusculos: StateFlow<Map<Musculo, Int>> =
        ejercicioDao.getConteoPorMusculo()
            .map { lista ->
                val mapa = mutableMapOf<Musculo, Int>()
                lista.forEach { item ->
                    item.musculo.split(",").forEach { musculoStr ->
                        val musculo = Musculo.valueOf(musculoStr.trim())
                        mapa[musculo] = (mapa[musculo] ?: 0) + item.total
                    }
                }
                mapa
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 2️⃣ Volumen por músculo
    val volumenPorMusculo: StateFlow<Map<Musculo, Double>> =
        serieDao.getVolumenPorMusculoRaw()
            .map { lista ->
                val mapa = mutableMapOf<Musculo, Double>()
                lista.forEach { item ->
                    item.musculo.split(",").forEach { musculoStr ->
                        val musculo = Musculo.valueOf(musculoStr.trim())
                        mapa[musculo] = (mapa[musculo] ?: 0.0) + (item.volumen ?: 0.0)
                    }
                }
                mapa
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 3️⃣ Resumen general
    val resumenGeneral: StateFlow<ResumenGeneral> =
        combine(
            ejercicioDao.getTotalEjerciciosFlow(),
            rutinaDao.getTotalRutinasFlow(),
            entrenamientoDao.getTotalEntrenamientosFlow(),
            volumenPorMusculo
        ) { totalEj, totalRut, totalEnt, volumenMap ->
            val musculoTop = volumenMap.maxByOrNull { it.value }?.key?.name
            ResumenGeneral(
                totalEjercicios = totalEj,
                totalRutinas = totalRut,
                totalEntrenamientos = totalEnt,
                musculoMasEntrenado = musculoTop
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResumenGeneral())

    // 4️⃣ Récords personales
    private val _records = MutableStateFlow(RecordPersonal())
    val records: StateFlow<RecordPersonal> =
        entrenamientoDao.getEntrenamientosCompletados()
            .flatMapLatest {

                flow {

                    val diaMax: DiaVolumen? = serieDao.getDiaMasVolumen()
                    val serieMax: SerieRecord? = serieDao.getSerieMasVolumen()
                    val entrenamientoMaxMs: Long? = entrenamientoDao.getEntrenamientoMasLargo()
                    val entrenamientoMinMs: Long? = entrenamientoDao.getEntrenamientoMasCorto()

                    fun format(ms: Long): String {

                        val totalSeg = ms / 1000

                        val h = totalSeg / 3600
                        val m = (totalSeg % 3600) / 60
                        val s = totalSeg % 60

                        return "${h}h ${m}m ${s}s"
                    }

                    val entrenamientoStr = entrenamientoMaxMs?.let { format(it) }

                    val entrenamientoMinStr = entrenamientoMinMs?.let { format(it) }

                    emit(
                        RecordPersonal(
                            diaMasVolumen = diaMax?.dia,
                            volumenDiaMasVolumen = diaMax?.volumenTotal,
                            serieMasVolumen = serieMax,
                            entrenamientoMasLargo = entrenamientoStr,
                            entrenamientoMasCorto = entrenamientoMinStr
                        )
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                RecordPersonal()
            )

    fun refreshStats() {

        viewModelScope.launch {

            val semana = entrenamientoDao.getEntrenamientosUltimaSemana()
            val mes = entrenamientoDao.getEntrenamientosUltimoMes()

            _actividad.value = ActividadStats(
                entrenamientosSemana = semana,
                entrenamientosMes = mes
            )

            val totalTiempo = entrenamientoDao.getTiempoTotalEntrenado() ?: 0
            val media = entrenamientoDao.getDuracionMedia() ?: 0.0

            val tiempoCardioMin = serieDao.getTiempoTotalCardio() ?: 0
            _tiempoCardioTotal.value = "$tiempoCardioMin min"

            fun format(ms: Long): String {

                val totalSeg = ms / 1000

                val h = totalSeg / 3600
                val m = (totalSeg % 3600) / 60
                val s = totalSeg % 60

                return "${h}h ${m}m ${s}s"
            }

            fun formatSinSegundos(ms: Long): String {

                val totalMin = ms / 1000 / 60

                val h = totalMin / 60
                val m = totalMin % 60

                return "${h}h ${m}m"
            }

            _tiempo.value = TiempoStats(
                tiempoTotal = format(totalTiempo),
                duracionMedia = format(media.toLong())
            )

            val rutinaTop = entrenamientoDao.getRutinaMasUsada()

            _rutinas.value = RutinaStats(
                rutinaMasUsada = rutinaTop
            )
        }
    }

    // Actividad
    private val _actividad = MutableStateFlow(ActividadStats())
    val actividad: StateFlow<ActividadStats> = _actividad

    // Tiempo
    private val _tiempo = MutableStateFlow(TiempoStats())
    val tiempo: StateFlow<TiempoStats> = _tiempo

    // Rutinas
    private val _rutinas = MutableStateFlow(RutinaStats())
    val rutinas: StateFlow<RutinaStats> = _rutinas
    private val _rutinasFrecuencia = MutableStateFlow<List<RutinaVeces>>(emptyList())
    val rutinasFrecuencia: StateFlow<List<RutinaVeces>> =
        entrenamientoDao.getVecesRutinasFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshStats()
    }
}

data class ResumenGeneral(
    val totalEjercicios: Int = 0,
    val totalRutinas: Int = 0,
    val totalEntrenamientos: Int = 0,
    val musculoMasEntrenado: String? = null
)

data class RecordPersonal(
    val diaMasVolumen: String? = null,
    val volumenDiaMasVolumen: Double? = null,
    val serieMasVolumen: SerieRecord? = null,
    val entrenamientoMasLargo: String? = null,
    val entrenamientoMasCorto: String? = null
)

data class ActividadStats(
    val entrenamientosSemana: Int = 0,
    val entrenamientosMes: Int = 0
)

data class TiempoStats(
    val tiempoTotal: String = "",
    val duracionMedia: String = ""
)

data class RutinaStats(
    val rutinaMasUsada: String? = null
)