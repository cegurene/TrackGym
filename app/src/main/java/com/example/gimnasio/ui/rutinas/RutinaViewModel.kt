package com.example.gimnasio.ui.rutinas

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.entity.Musculo
import com.example.gimnasio.data.entity.RutinaEjercicioEntity
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.entity.SerieEntity
import com.example.gimnasio.data.model.EjercicioConOrden
import com.example.gimnasio.data.model.RutinaConEjercicios
import com.example.gimnasio.data.prefs.SortPreferences
import com.example.gimnasio.data.sync.CloudSyncCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class RutinaViewModel(application: Application) : ViewModel() {

    enum class NombreOperacionResultado {
        OK,
        VACIO,
        DUPLICADO
    }

    enum class RutinaOrder {
        ALPHABETIC_ASC,
        ALPHABETIC_DESC,
        TIMES_DONE_DESC,
        TIMES_DONE_ASC
    }

    enum class RutinaDetailExerciseOrder {
        ALPHABETIC_ASC,
        ALPHABETIC_DESC,
        TIMES_DONE_DESC,
        TIMES_DONE_ASC
    }

    private val database = GymDatabase.getDatabase(application)
    private val cloudSyncCoordinator = CloudSyncCoordinator(application)
    private val rutinaDao = database.rutinaDao()
    private val ejercicioDao = database.ejercicioDao()
    private val rutinaEjercicioDao = database.rutinaEjercicioDao()
    private val entrenamientoDao = database.entrenamientoDao()
    private val entrenamientoEjercicioDao = database.entrenamientoEjercicioDao()
    private val serieDao = database.serieDao()

    private val sortPreferences = SortPreferences(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedMusculos = MutableStateFlow<Set<Musculo>>(emptySet())
    val selectedMusculos = _selectedMusculos.asStateFlow()

    private val _order = MutableStateFlow(sortPreferences.getRutinaOrder())
    val order = _order.asStateFlow()

    val rutinas: Flow<List<RutinaConEjercicios>> = combine(
        _searchQuery,
        _selectedMusculos,
        _order,
        rutinaDao.getRutinasConEjercicios(),
        entrenamientoDao.getVecesRutinasFlow()
    ) { query, musculos, orden, todasRutinas, vecesRealizadas ->
        val vecesMap = vecesRealizadas.associate { it.id to it.veces }

        todasRutinas
            .filter { rutina ->
                rutina.rutina.nombre.contains(query, ignoreCase = true)
            }
            .filter { rutina ->
                if (musculos.isEmpty()) {
                    true
                } else {
                    rutina.ejercicios.any { ejercicio ->
                        ejercicio.musculos.any { musculos.contains(it) }
                    }
                }
            }
            .let { filtrados ->
                when (orden) {
                    RutinaOrder.ALPHABETIC_ASC -> filtrados.sortedBy { it.rutina.nombre }
                    RutinaOrder.ALPHABETIC_DESC -> filtrados.sortedByDescending { it.rutina.nombre }
                    RutinaOrder.TIMES_DONE_DESC -> filtrados.sortedByDescending { vecesMap[it.rutina.id] ?: 0 }
                    RutinaOrder.TIMES_DONE_ASC -> filtrados.sortedBy { vecesMap[it.rutina.id] ?: 0 }
                }
            }
    }

    val entrenamientoActivo =
        entrenamientoDao.getEntrenamientoActivoFlow()

    fun insertar(
        nombre: String,
        onResult: (NombreOperacionResultado) -> Unit = {}
    ) {
        val nombreNormalizado = nombre.trim()
        if (nombreNormalizado.isBlank()) {
            onResult(NombreOperacionResultado.VACIO)
            return
        }

        viewModelScope.launch {
            try {
                if (rutinaDao.existsByNombre(nombreNormalizado)) {
                    onResult(NombreOperacionResultado.DUPLICADO)
                    return@launch
                }

                rutinaDao.insert(RutinaEntity(nombre = nombreNormalizado))
                cloudSyncCoordinator.syncNow()
                onResult(NombreOperacionResultado.OK)
            } catch (_: SQLiteConstraintException) {
                onResult(NombreOperacionResultado.DUPLICADO)
            }
        }
    }

    fun getRutina(id: Long): Flow<RutinaEntity?> {
        return rutinaDao.getByIdFlow(id)
    }

    fun borrarRutina(id: Long) {
        viewModelScope.launch {
            rutinaDao.deleteById(id)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun renombrarRutina(
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
                if (rutinaDao.existsByNombreExcludingId(nombreNormalizado, id)) {
                    onResult(NombreOperacionResultado.DUPLICADO)
                    return@launch
                }

                rutinaDao.updateNombre(id, nombreNormalizado)
                cloudSyncCoordinator.syncNow()
                onResult(NombreOperacionResultado.OK)
            } catch (_: SQLiteConstraintException) {
                onResult(NombreOperacionResultado.DUPLICADO)
            }
        }
    }

    fun getRutinaConEjercicios(id: Long): Flow<RutinaConEjercicios?> {
        return rutinaDao.getRutinaConEjercicios(id)
    }

    fun getAllEjercicios(): Flow<List<EjercicioEntity>> {
        return ejercicioDao.getAll()
    }

    fun añadirEjercicioARutina(rutinaId: Long, ejercicioId: Long) {
        viewModelScope.launch {

            val maxOrden = rutinaEjercicioDao.getMaxOrden(rutinaId) ?: -1

            rutinaEjercicioDao.insert(
                RutinaEjercicioEntity(
                    rutinaId = rutinaId,
                    ejercicioId = ejercicioId,
                    orden = maxOrden + 1
                )
            )
            cloudSyncCoordinator.syncNow()
        }
    }

    fun quitarEjercicioDeRutina(rutinaId: Long, ejercicioId: Long) {
        viewModelScope.launch {
            rutinaEjercicioDao.delete(rutinaId, ejercicioId)
            cloudSyncCoordinator.syncNow()
        }
    }

    fun getEjerciciosConOrden(rutinaId: Long) =
        rutinaEjercicioDao.getEjerciciosConOrden(rutinaId)

    fun moverEjercicio(
        rutinaId: Long,
        actual: EjercicioConOrden,
        otro: EjercicioConOrden
    ) {
        viewModelScope.launch {

            rutinaEjercicioDao.updateOrden(
                rutinaId,
                actual.ejercicio.id,
                otro.orden
            )

            rutinaEjercicioDao.updateOrden(
                rutinaId,
                otro.ejercicio.id,
                actual.orden
            )
            cloudSyncCoordinator.syncNow()
        }
    }

    fun iniciarEntrenamiento(
        rutinaId: Long,
        onNavigate: (Long) -> Unit
    ) {
        viewModelScope.launch {

            val ejerciciosRutina = rutinaEjercicioDao.getEjerciciosDeRutinaOnce(rutinaId)

            if (ejerciciosRutina.isEmpty()) {
                return@launch
            }

            // 1️⃣ Comprobar si ya existe entrenamiento activo
            val activo = entrenamientoDao.getEntrenamientoActivo()

            if (activo != null) {
                onNavigate(activo.id)
                return@launch
            }

            val nombreRutina = entrenamientoDao.getNombreRutinaById(rutinaId)?.trim().orEmpty()
            val completadosRutina = entrenamientoDao.countCompletadosByRutinaId(rutinaId)
            val nombreEntrenamiento = if (nombreRutina.isNotBlank()) {
                "$nombreRutina -> ${completadosRutina + 1}"
            } else {
                "Entrenamiento"
            }

            // 2️⃣ Crear nuevo entrenamiento
            val nuevoId = entrenamientoDao.insert(
                EntrenamientoEntity(
                    rutinaId = rutinaId,
                    nombre = nombreEntrenamiento,
                    fechaInicio = System.currentTimeMillis(),
                    fechaFin = null,
                    completado = false
                )
            )
            // 3️⃣ Copiar ejercicios y crear 1 serie inicial por cada uno
            ejerciciosRutina.forEach { ejercicioRutina ->
                val entrenamientoEjercicioId = entrenamientoDao.insertEjercicioDeEntrenamiento(
                    EntrenamientoEjercicioEntity(
                        entrenamientoId = nuevoId,
                        ejercicioId = ejercicioRutina.ejercicioId,
                        orden = ejercicioRutina.orden
                    )
                )

                val esCardio = ejercicioDao
                    .getById(ejercicioRutina.ejercicioId)
                    ?.musculos
                    ?.contains(Musculo.CARDIO) == true

                val serieInicial = if (esCardio) {
                    SerieEntity(
                        entrenamientoEjercicioId = entrenamientoEjercicioId,
                        tiempo = 0,
                        intensidad = 0
                    )
                } else {
                    SerieEntity(
                        entrenamientoEjercicioId = entrenamientoEjercicioId,
                        peso = 0f,
                        repeticiones = 0
                    )
                }

                serieDao.insert(serieInicial)
            }

            // 4️⃣ Navegar
            onNavigate(nuevoId)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleMusculo(musculo: Musculo) {
        val current = _selectedMusculos.value.toMutableSet()
        if (current.contains(musculo)) {
            current.remove(musculo)
        } else {
            current.add(musculo)
        }
        _selectedMusculos.value = current
    }

    fun clearMusculos() {
        _selectedMusculos.value = emptySet()
    }

    fun onOrderChange(newOrder: RutinaOrder) {
        _order.value = newOrder
        sortPreferences.saveRutinaOrder(newOrder)
    }
}
