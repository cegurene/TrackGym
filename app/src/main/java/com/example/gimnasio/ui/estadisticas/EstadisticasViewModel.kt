package com.example.gimnasio.ui.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.model.DiaVolumen
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
    val records: StateFlow<RecordPersonal> = _records

    init {
        viewModelScope.launch {
            // Día con más volumen
            val diaMax: DiaVolumen? = serieDao.getDiaMasVolumen()

            // Serie con más volumen
            val serieMax: SerieRecord? = serieDao.getSerieMasVolumen()

            // Entrenamiento más largo (en milisegundos)
            val entrenamientoMaxMs: Long? = entrenamientoDao.getEntrenamientoMasLargo()

            // Entrenamiento más corto (en milisegundos)
            val entrenamientoMinMs: Long? = entrenamientoDao.getEntrenamientoMasCorto()

            // Convertir ms a horas:minutos
            val entrenamientoStr = entrenamientoMaxMs?.let {
                val horas = (it / 1000 / 60 / 60)
                val minutos = (it / 1000 / 60) % 60
                "${horas}h ${minutos}m"
            }

            val entrenamientoMinStr = entrenamientoMinMs?.let {
                val horas = (it / 1000 / 60 / 60)
                val minutos = (it / 1000 / 60) % 60
                "${horas}h ${minutos}m"
            }

            // Actualizar records
            _records.value = RecordPersonal(
                diaMasVolumen = diaMax?.dia,
                volumenDiaMasVolumen = diaMax?.volumenTotal,
                serieMasVolumen = serieMax,
                entrenamientoMasLargo = entrenamientoStr,
                entrenamientoMasCorto = entrenamientoMinStr
            )
        }
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