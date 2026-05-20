package com.example.gimnasio.data.prefs

import android.content.Context
import com.example.gimnasio.ui.ejercicios.EjercicioViewModel
import com.example.gimnasio.ui.rutinas.RutinaViewModel

class SortPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "gimnasio_sort_prefs"
        private const val KEY_RUTINA_ORDER = "rutina_order"
        private const val KEY_EJERCICIO_ORDER = "ejercicio_order"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveRutinaOrder(order: RutinaViewModel.RutinaOrder) {
        sharedPrefs.edit().putString(KEY_RUTINA_ORDER, order.name).apply()
    }

    fun getRutinaOrder(): RutinaViewModel.RutinaOrder {
        val orderName = sharedPrefs.getString(KEY_RUTINA_ORDER, RutinaViewModel.RutinaOrder.ALPHABETIC_ASC.name)
        return try {
            RutinaViewModel.RutinaOrder.valueOf(orderName!!)
        } catch (e: Exception) {
            RutinaViewModel.RutinaOrder.ALPHABETIC_ASC
        }
    }

    fun saveEjercicioOrder(order: EjercicioViewModel.EjercicioOrder) {
        sharedPrefs.edit().putString(KEY_EJERCICIO_ORDER, order.name).apply()
    }

    fun getEjercicioOrder(): EjercicioViewModel.EjercicioOrder {
        val orderName = sharedPrefs.getString(KEY_EJERCICIO_ORDER, EjercicioViewModel.EjercicioOrder.ALPHABETIC_ASC.name)
        return try {
            EjercicioViewModel.EjercicioOrder.valueOf(orderName!!)
        } catch (e: Exception) {
            EjercicioViewModel.EjercicioOrder.ALPHABETIC_ASC
        }
    }
}

