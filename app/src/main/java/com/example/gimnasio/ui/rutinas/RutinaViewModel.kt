package com.example.gimnasio.ui.rutinas

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gimnasio.data.GymDatabase
import com.example.gimnasio.data.entity.RutinaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class RutinaViewModel(application: Application) : ViewModel() {

    private val rutinaDao = GymDatabase.getDatabase(application).rutinaDao()

    val rutinas: Flow<List<RutinaEntity>> = rutinaDao.getAll()

    fun insertar(nombre: String) {
        viewModelScope.launch {
            rutinaDao.insert(RutinaEntity(nombre = nombre))
        }
    }
}

class RutinaViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RutinaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RutinaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
