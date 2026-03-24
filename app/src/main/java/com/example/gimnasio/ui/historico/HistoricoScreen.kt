package com.example.gimnasio.ui.historico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 Histórico") }
            )
        }
    ) { padding ->
        if (entrenamientos.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Aún no has completado entrenamientos.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

        } else {

            val numeroEntrenamientos = entrenamientos.size

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
}