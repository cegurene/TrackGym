package com.example.gimnasio.ui.historico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HistoricoScreen(
    onEntrenamientoClick: (Long) -> Unit
) {

    val context = LocalContext.current

    val viewModel: HistoricoViewModel = viewModel(
        factory = HistoricoViewModelFactory(context)
    )

    val entrenamientos by viewModel.entrenamientos
        .collectAsState(initial = emptyList())

    if (entrenamientos.isEmpty()) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aún no has completado entrenamientos")
        }

    } else {

        val numeroEntrenamientos = entrenamientos.size

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(entrenamientos) { index, item ->
                HistoricoItem(
                    item = item,
                    onClick = {
                        onEntrenamientoClick(item.entrenamiento.id)
                    },
                    numero = numeroEntrenamientos - index
                )
            }
        }
    }
}