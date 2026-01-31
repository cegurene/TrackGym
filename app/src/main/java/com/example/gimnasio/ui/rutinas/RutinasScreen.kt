package com.example.gimnasio.ui.rutinas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gimnasio.data.entity.RutinaEntity

@Composable
fun RutinasScreen(
    rutinas: List<RutinaEntity>,
    modifier: Modifier = Modifier
) {
    if (rutinas.isEmpty()) {
        Text(
            text = "No hay rutinas todavía",
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyColumn(modifier = modifier) {
            items(rutinas) { rutina ->
                Text(text = rutina.nombre)
            }
        }
    }
}
