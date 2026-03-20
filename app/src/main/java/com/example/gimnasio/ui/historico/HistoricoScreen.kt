package com.example.gimnasio.ui.historico

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        // Cogemos el numero de entrenamientos
        val numeroEntrenamientos = entrenamientos.size

        val listState = rememberLazyListState()

        val isScrolling by remember {
            derivedStateOf {
                listState.isScrollInProgress
            }
        }

        val alpha by animateFloatAsState(
            targetValue = if (isScrolling) 1f else 0f
        )

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
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

            val isScrolling by remember {
                derivedStateOf { listState.isScrollInProgress }
            }

            val alpha by animateFloatAsState(
                targetValue = if (isScrolling) 1f else 0f
            )

            VerticalScrollbar(
                state = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 4.dp)
                    .graphicsLayer { this.alpha = alpha },
                onDrag = { delta ->

                    // Scroll proporcional
                    val totalItems = listState.layoutInfo.totalItemsCount

                    val newIndex = (listState.firstVisibleItemIndex + delta * totalItems)
                        .toInt()
                        .coerceIn(0, totalItems - 1)

                    kotlinx.coroutines.MainScope().launch {
                        listState.animateScrollToItem(newIndex)
                    }
                }
            )
        }
    }
}

@Composable
fun VerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    onDrag: (Float) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {

        val heightPx = constraints.maxHeight.toFloat()

        val layoutInfo = state.layoutInfo
        val totalItems = layoutInfo.totalItemsCount

        if (totalItems == 0) return@BoxWithConstraints

        val firstItem = layoutInfo.visibleItemsInfo.firstOrNull()

        val scrollOffset = state.firstVisibleItemScrollOffset.toFloat()
        val itemHeight = firstItem?.size?.toFloat() ?: 1f
        val index = state.firstVisibleItemIndex.toFloat()

        val scrollProgress = (index + (scrollOffset / itemHeight)) / totalItems

        val alphaAnim = remember { Animatable(0f) }

        val isScrolling by remember {
            derivedStateOf { state.isScrollInProgress }
        }

        LaunchedEffect(isScrolling) {
            if (isScrolling) {
                alphaAnim.animateTo(
                    1f,
                    animationSpec = tween(200)
                )
            } else {
                // 👇 ESPERA antes de desaparecer (CLAVE)
                delay(300)

                // Si ha vuelto el scroll, NO ocultes
                if (!state.isScrollInProgress) {
                    alphaAnim.animateTo(
                        0f,
                        animationSpec = tween(2000) // 👈 ahora sí se nota
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(5.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val delta = dragAmount.y / heightPx

                        onDrag(delta)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.15f)
                    .offset(y = (scrollProgress * heightPx).dp)
                    .graphicsLayer {
                        alpha = alphaAnim.value
                    }
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}