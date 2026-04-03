package com.example.gimnasio.ui.rutinas

import android.app.Application
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class RutinaViewModel(application: Application) : ViewModel() {

    private val database = GymDatabase.getDatabase(application)
    private val rutinaDao = database.rutinaDao()
    private val ejercicioDao = database.ejercicioDao()
    private val rutinaEjercicioDao = database.rutinaEjercicioDao()
    private val entrenamientoDao = database.entrenamientoDao()
    private val entrenamientoEjercicioDao = database.entrenamientoEjercicioDao()
    private val serieDao = database.serieDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedMusculos = MutableStateFlow<Set<Musculo>>(emptySet())
    val selectedMusculos = _selectedMusculos.asStateFlow()

    val rutinas: Flow<List<RutinaConEjercicios>> = combine(
        _searchQuery,
        _selectedMusculos,
        rutinaDao.getRutinasConEjercicios()
    ) { query, musculos, todasRutinas ->
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
    }

    val entrenamientoActivo =
        entrenamientoDao.getEntrenamientoActivoFlow()

    fun insertar(nombre: String) {
        viewModelScope.launch {
            rutinaDao.insert(RutinaEntity(nombre = nombre))
        }
    }

    fun getRutina(id: Long): Flow<RutinaEntity?> {
        return rutinaDao.getByIdFlow(id)
    }

    fun borrarRutina(id: Long) {
        viewModelScope.launch {
            rutinaDao.deleteById(id)
        }
    }

    fun renombrarRutina(id: Long, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        viewModelScope.launch {
            rutinaDao.updateNombre(id, nuevoNombre.trim())
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
        }
    }

    fun quitarEjercicioDeRutina(rutinaId: Long, ejercicioId: Long) {
        viewModelScope.launch {
            rutinaEjercicioDao.delete(rutinaId, ejercicioId)
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

            // 2️⃣ Crear nuevo entrenamiento
            val nuevoId = entrenamientoDao.insert(
                EntrenamientoEntity(
                    rutinaId = rutinaId,
                    nombre = "Entrenamiento",
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
}
