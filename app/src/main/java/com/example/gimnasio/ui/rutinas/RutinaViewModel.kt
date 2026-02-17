package com.example.gimnasio.ui.rutinas

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEjercicioEntity
import com.example.gimnasio.data.entity.EntrenamientoEntity
import com.example.gimnasio.data.entity.RutinaEjercicioEntity
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.model.EjercicioConOrden
import com.example.gimnasio.data.model.RutinaConEjercicios
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


class RutinaViewModel(application: Application) : ViewModel() {

    private val database = GymDatabase.getDatabase(application)
    private val rutinaDao = database.rutinaDao()
    private val ejercicioDao = database.ejercicioDao()
    private val rutinaEjercicioDao = database.rutinaEjercicioDao()
    private val entrenamientoDao = database.entrenamientoDao()
    private val entrenamientoEjercicioDao = database.entrenamientoEjercicioDao()


    val rutinas: Flow<List<RutinaConEjercicios>> =
        rutinaDao.getRutinasConEjercicios()

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
                    fechaInicio = System.currentTimeMillis(),
                    fechaFin = null,
                    completado = false
                )
            )

            // 3️⃣ Obtener ejercicios de la rutina
            val ejerciciosRutina =
                rutinaEjercicioDao.getEjerciciosDeRutinaOnce(rutinaId)

            // 4️⃣ Copiar a EntrenamientoEjercicio
            val lista = ejerciciosRutina.map {
                EntrenamientoEjercicioEntity(
                    entrenamientoId = nuevoId,
                    ejercicioId = it.ejercicioId,
                    orden = it.orden
                )
            }

            entrenamientoEjercicioDao.insertAll(lista)

            // 5️⃣ Navegar
            onNavigate(nuevoId)
        }
    }

}

