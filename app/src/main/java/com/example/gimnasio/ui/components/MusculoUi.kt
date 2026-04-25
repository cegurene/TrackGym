package com.example.gimnasio.ui.components

import com.example.gimnasio.R
import com.example.gimnasio.data.entity.Musculo

fun Musculo.displayLabel(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

fun Musculo.emoji(): String = when (this) {
    Musculo.BICEPS, Musculo.TRICEPS, Musculo.ANTEBRAZOS -> "💪"
    Musculo.PECHO -> "🏋️"
    Musculo.ESPALDA -> "🔙"
    Musculo.CUÁDRICEPS, Musculo.FEMORAL, Musculo.ADUCTOR, Musculo.ABDUCTOR, Musculo.GEMELOS -> "🦵"
    Musculo.HOMBROS -> "🤸"
    Musculo.ABDOMINALES -> "🧱"
    Musculo.CARDIO -> "🏃"
}

fun Iterable<Musculo>.emojiSummary(): String =
    distinctBy { it.emoji() }
        .joinToString(" ") { it.emoji() }
        .ifBlank { "🏋️" }

fun Musculo.labelWithEmoji(): String = "${emoji()} ${displayLabel()}"

fun Musculo.imageRes(): Int = when (this) {
    Musculo.PECHO -> R.drawable.ic_muscle_pecho
    Musculo.ESPALDA -> R.drawable.ic_muscle_espalda
    Musculo.HOMBROS -> R.drawable.ic_muscle_hombros
    Musculo.BICEPS -> R.drawable.ic_muscle_biceps
    Musculo.TRICEPS -> R.drawable.ic_muscle_triceps
    Musculo.ANTEBRAZOS -> R.drawable.ic_muscle_antebrazo
    Musculo.CUÁDRICEPS -> R.drawable.ic_muscle_cuadriceps
    Musculo.FEMORAL -> R.drawable.ic_muscle_femoral
    Musculo.ADUCTOR -> R.drawable.ic_muscle_aductor
    Musculo.ABDUCTOR -> R.drawable.ic_muscle_abductor
    Musculo.GEMELOS -> R.drawable.ic_muscle_gemelos
    Musculo.ABDOMINALES -> R.drawable.ic_muscle_abdominales
    Musculo.CARDIO -> R.drawable.ic_muscle_cardio
}

fun Iterable<Musculo>.primaryImageRes(): Int =
    firstOrNull()?.imageRes() ?: R.drawable.ic_muscle_pecho
