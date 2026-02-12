package com.example.gimnasio.ui.rutinas

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.EjercicioEntity
import com.example.gimnasio.data.entity.RutinaEjercicioEntity
import com.example.gimnasio.data.entity.RutinaEntity
import com.example.gimnasio.data.model.RutinaConEjercicios
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


class RutinaViewModel(application: Application) : ViewModel() {

    private val database = GymDatabase.getDatabase(application)

    private val rutinaDao = database.rutinaDao()
    private val ejercicioDao = database.ejercicioDao()
    private val rutinaEjercicioDao = database.rutinaEjercicioDao()

    val rutinas: Flow<List<RutinaEntity>> = rutinaDao.getAllRutinas()

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
            rutinaEjercicioDao.insert(
                RutinaEjercicioEntity(
                    rutinaId = rutinaId,
                    ejercicioId = ejercicioId
                )
            )
        }
    }
}

