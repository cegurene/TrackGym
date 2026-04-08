package com.example.gimnasio.ui.components

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
    distinct().joinToString(" ") { it.emoji() }.ifBlank { "🏋️" }

fun Musculo.labelWithEmoji(): String = "${emoji()} ${displayLabel()}"

