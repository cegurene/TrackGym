package com.example.gimnasio.data.model

data class MejorCargaCardio(
    val tiempo: Int,
    val intensidad: Int,
    val carga: Float,
    val fecha: Long? = null
)
