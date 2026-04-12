package com.example.gimnasio.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

fun Float.formatUiNumber(): String =
    BigDecimal.valueOf(this.toDouble())
        .setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

