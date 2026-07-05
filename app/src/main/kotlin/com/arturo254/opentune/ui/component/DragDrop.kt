/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PreferredLyricsProvider

/**
 * Estado de drag & drop basado en la posición REAL de los items dentro del
 * LazyColumn (a través de [LazyListState.layoutInfo]) en lugar de índices
 * guardados por separado en cada item. Esto evita la desincronización que
 * causaba que el drag "se rompiera" al reordenar.
 */
private class DragDropListState(
    private val listState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var draggingItemInitialOffset = 0
    private var draggedDistance by mutableStateOf(0f)

    private val currentElement: LazyListItemInfo?
        get() = draggingItemIndex?.let { index ->
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }

    /** Cuánto debe desplazarse visualmente (translationY) el item que se arrastra. */
    val elementDisplacement: Float
        get() = currentElement?.let { item ->
            (draggingItemInitialOffset + draggedDistance) - item.offset
        } ?: 0f

    fun onDragStart(offset: Offset) {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
                draggedDistance = 0f
            }
    }

    fun onDrag(dragAmount: Offset) {
        val current = currentElement ?: return
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return

        // Límites reales de la lista: el item arrastrado no puede visualmente
        // moverse por encima del primer item ni por debajo del último.
        // Sin este clamp, draggedDistance crecía sin tope al seguir empujando
        // el dedo más allá del borde, lo que empujaba el item fuera del
        // contenedor (se "recortaba" y parecía desaparecer) y dejaba un
        // desfase acumulado que luego hacía saltar varias posiciones de golpe.
        val minTop = visible.first().offset.toFloat()
        val maxTop = (visible.last().offset + visible.last().size - current.size).toFloat()

        val proposedTop = (draggingItemInitialOffset + draggedDistance + dragAmount.y)
            .coerceIn(minTop.coerceAtMost(maxTop), maxTop.coerceAtLeast(minTop))
        draggedDistance = proposedTop - draggingItemInitialOffset

        val startOffset = current.offset + elementDisplacement
        val middle = startOffset + current.size / 2f

        val target = visible.firstOrNull { item ->
            middle.toInt() in item.offset..(item.offset + item.size) && item.index != current.index
        }

        if (target != null) {
            onMove(current.index, target.index)
            // Ajustamos el offset inicial al del nuevo índice para que el
            // desplazamiento visual siga siendo continuo (sin saltos).
            draggingItemInitialOffset = target.offset
            draggedDistance = startOffset - target.offset
            draggingItemIndex = target.index
        }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggedDistance = 0f
        draggingItemInitialOffset = 0
    }
}

@Composable
private fun rememberDragDropListState(
    listState: LazyListState,
    onMove: (Int, Int) -> Unit,
): DragDropListState = remember(listState) { DragDropListState(listState, onMove) }

@Composable
fun DragDropLyricsProviderDialog(
    providers: List<PreferredLyricsProvider>,
    selectedProvider: PreferredLyricsProvider,
    onDismiss: () -> Unit,
    onOrderConfirmed: (List<PreferredLyricsProvider>) -> Unit,
    valueText: (PreferredLyricsProvider) -> String,
) {
    var items by remember { mutableStateOf(providers) }
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val dragDropState = rememberDragDropListState(
        listState = listState,
        onMove = { from, to ->
            items = items.toMutableList().apply { add(to, removeAt(from)) }
        },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp), // Forma expresiva M3
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Handle superior estilo bottom-sheet expressive
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.reorder_lyrics_providers),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.reorder_lyrics_providers_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .pointerInput(dragDropState) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    dragDropState.onDragStart(offset)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDropState.onDrag(dragAmount)
                                },
                                onDragEnd = { dragDropState.onDragEnd() },
                                onDragCancel = { dragDropState.onDragEnd() },
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, provider -> provider.name },
                    ) { index, provider ->
                        val isDragging = dragDropState.draggingItemIndex == index
                        val isSelected = provider == selectedProvider

                        val elevation by animateFloatAsState(
                            targetValue = if (isDragging) 12f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "elevation",
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.03f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "scale",
                        )

                        ProviderRow(
                            index = index,
                            displayName = valueText(provider),
                            isSelected = isSelected,
                            isDragging = isDragging,
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .then(
                                    // Importante: mientras haya un drag activo, NINGÚN item debe
                                    // animar su posición (ni siquiera los que no se arrastran).
                                    // Si animan, su offset en layoutInfo queda en un valor
                                    // transicional durante ~300ms, y el hit-testing de onDrag()
                                    // compara justamente esos offsets — eso es lo que causaba
                                    // que el item "saltara hasta arriba" en vez de moverse de a uno.
                                    if (dragDropState.draggingItemIndex == null) {
                                        Modifier.animateItem(tween(300))
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer {
                                    translationY =
                                        if (isDragging) dragDropState.elementDisplacement else 0f
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = elevation
                                },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            onOrderConfirmed(items)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(
    index: Int,
    displayName: String,
    isSelected: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer
            isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "containerColor",
    )

    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp), // Forma expresiva por item
        color = containerColor,
        tonalElevation = if (isDragging) 4.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected || isDragging) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected || isDragging) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isSelected) {
                        Text(
                            text = "Proveedor preferido",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Icon(
                painter = painterResource(R.drawable.drag_handle),
                contentDescription = "Arrastrar",
                tint = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
        }
    }
}