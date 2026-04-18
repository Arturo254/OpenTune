@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    liquidGlass: Boolean = false,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String = "",
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    onMusicRecognitionClick: (() -> Unit)? = null,
    musicRecognitionContentDescription: String = "",
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    val toolbarContainerColor = floatingToolbarContainerColor(pureBlack = pureBlack, liquidGlass = liquidGlass)
    val toolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = toolbarContainerColor,
    )
    val hasOverflowAction = onShuffleClick != null && shuffleIconRes != null
    val hasFabAction = onFabClick != null && fabIconRes != null

    // Modificador para el efecto Liquid Glass: borde con gradiente de luz y overlay sutil
    val glassModifier = if (liquidGlass) {
        Modifier.liquidGlassStyle(pureBlack = pureBlack)
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val showSelectedLabels = maxWidth >= 360.dp

        if (hasOverflowAction) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarOverflowAction(
                        pureBlack = pureBlack,
                        liquidGlass = liquidGlass,
                        onShuffleClick = onShuffleClick,
                        shuffleIconRes = shuffleIconRes,
                        shuffleContentDescription = shuffleContentDescription,
                        onMusicRecognitionClick = onMusicRecognitionClick,
                        musicRecognitionContentDescription = musicRecognitionContentDescription,
                    )
                },
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .then(glassModifier),
                colors = toolbarColors,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)
                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        showSelectedLabel = showSelectedLabels,
                        pureBlack = pureBlack,
                        liquidGlass = liquidGlass,
                        onClick = { onItemClick(screen, selected) },
                    )
                }
            }
        } else if (hasFabAction) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarFabAction(
                        pureBlack = pureBlack,
                        liquidGlass = liquidGlass,
                        onClick = onFabClick,
                        iconRes = fabIconRes,
                        contentDescription = fabContentDescription,
                    )
                },
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .then(glassModifier),
                colors = toolbarColors,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)
                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        showSelectedLabel = showSelectedLabels,
                        pureBlack = pureBlack,
                        liquidGlass = liquidGlass,
                        onClick = { onItemClick(screen, selected) },
                    )
                }
            }
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .then(glassModifier),
                colors = toolbarColors,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)
                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        showSelectedLabel = showSelectedLabels,
                        pureBlack = pureBlack,
                        liquidGlass = liquidGlass,
                        onClick = { onItemClick(screen, selected) },
                    )
                }
            }
        }
    }
}

// ── Efecto visual Liquid Glass ───────────────────────────────────────────────

/**
 * Aplica el estilo Liquid Glass al toolbar:
 * - Borde superior más brillante (simula refracción de luz en el borde del vidrio)
 * - Borde inferior más sutil (reflejo secundario)
 * - Overlay de highlight en la parte superior (brillo tipo lente)
 */
private fun Modifier.liquidGlassStyle(
    pureBlack: Boolean,
    shape: Shape = RoundedCornerShape(
        topStart = 100.dp,
        bottomStart = 100.dp,
        topEnd = 24.dp,
        bottomEnd = 24.dp
    ),
): Modifier =
    this
        .clip(shape)
        .border(
            width = 0.8.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (pureBlack) 0.22f else 0.38f),
                    Color.White.copy(alpha = if (pureBlack) 0.04f else 0.08f),
                ),
            ),
            shape = shape,
        )
        .drawWithContent {
            drawContent()

            // 🔥 highlight horizontal (vidrio)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (pureBlack) 0.10f else 0.18f),
                        Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f)
                ),
                size = size,
                cornerRadius = CornerRadius(
                    x = size.height / 4f,
                    y = size.height / 4f
                )
            )

            // 🔥 brillo superior
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (pureBlack) 0.06f else 0.12f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.6f
                ),
                size = size,
                cornerRadius = CornerRadius(
                    x = size.height / 4f,
                    y = size.height / 4f
                )
            )
        }



// ── Subcomponentes ───────────────────────────────────────────────────────────

@Composable
private fun FloatingToolbarOverflowAction(
    pureBlack: Boolean,
    liquidGlass: Boolean,
    onShuffleClick: (() -> Unit)?,
    shuffleIconRes: Int?,
    shuffleContentDescription: String,
    onMusicRecognitionClick: (() -> Unit)?,
    musicRecognitionContentDescription: String,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val fabShape = RoundedCornerShape(16.dp)

    val glassModifier = if (liquidGlass) {
        Modifier.liquidGlassStyle(
            pureBlack = pureBlack,
            shape = fabShape
        )
    } else Modifier

    Box {
        // 🔥 FAB con glass consistente
        Box(
            modifier = glassModifier
        ) {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = { fabMenuExpanded = !fabMenuExpanded },
                containerColor = floatingToolbarFabContainerColor(
                    pureBlack = pureBlack,
                    liquidGlass = liquidGlass
                ),
                contentColor = floatingToolbarFabContentColor(
                    pureBlack = pureBlack,
                    liquidGlass = liquidGlass
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription =
                        shuffleContentDescription.ifEmpty {
                            stringResource(R.string.more)
                        },
                )
            }
        }

        DropdownMenu(
            expanded = fabMenuExpanded,
            onDismissRequest = { fabMenuExpanded = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.music_recognition)) },
                onClick = {
                    fabMenuExpanded = false
                    onMusicRecognitionClick?.invoke()
                },
                leadingIcon = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = floatingToolbarMenuIconContainerColor(pureBlack = pureBlack),
                        contentColor = floatingToolbarMenuIconContentColor(pureBlack = pureBlack),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.mic),
                                contentDescription =
                                    musicRecognitionContentDescription.ifEmpty {
                                        stringResource(R.string.music_recognition)
                                    },
                            )
                        }
                    }
                },
                enabled = onMusicRecognitionClick != null,
                colors =
                    MenuDefaults.itemColors(
                        textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledLeadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    ),
            )

            if (onShuffleClick != null && shuffleIconRes != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.shuffle)) },
                    onClick = {
                        fabMenuExpanded = false
                        onShuffleClick()
                    },
                    leadingIcon = {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = floatingToolbarMenuIconContainerColor(pureBlack = pureBlack),
                            contentColor = floatingToolbarMenuIconContentColor(pureBlack = pureBlack),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(shuffleIconRes),
                                    contentDescription =
                                        shuffleContentDescription.ifEmpty {
                                            stringResource(R.string.shuffle)
                                        },
                                )
                            }
                        }
                    },
                    colors =
                        MenuDefaults.itemColors(
                            textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                            leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarFabAction(
    pureBlack: Boolean,
    liquidGlass: Boolean,
    onClick: (() -> Unit)?,
    iconRes: Int?,
    contentDescription: String,
) {
    if (onClick == null || iconRes == null) return

    FloatingToolbarDefaults.VibrantFloatingActionButton(
        onClick = onClick,
        containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack, liquidGlass = liquidGlass),
        contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack, liquidGlass = liquidGlass),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription =
                contentDescription.ifEmpty {
                    stringResource(R.string.create_playlist)
                },
        )
    }
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    showSelectedLabel: Boolean,
    pureBlack: Boolean,
    liquidGlass: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val containerColor by animateColorAsState(
        targetValue =
            when {
                selected -> floatingToolbarSelectedItemContainerColor(pureBlack = pureBlack, liquidGlass = liquidGlass)
                else -> Color.Transparent
            },
        label = "",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                selected -> floatingToolbarSelectedItemContentColor(pureBlack = pureBlack, liquidGlass = liquidGlass)
                else -> floatingToolbarItemContentColor(pureBlack = pureBlack, liquidGlass = liquidGlass)
            },
        label = "",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "",
    )
    val showLabel = selected && showSelectedLabel && screen.route != Screens.Search.route

    Row(
        modifier =
            Modifier
                .scale(scale)
                .animateContentSize()
                .clip(shape)
                .background(color = containerColor, shape = shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .widthIn(min = 48.dp)
                .padding(
                    horizontal = if (showLabel) 16.dp else 12.dp,
                    vertical = 12.dp,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
        )

        if (showLabel) {
            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = stringResource(screen.titleId),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Color helpers ────────────────────────────────────────────────────────────

@Composable
private fun floatingToolbarContainerColor(pureBlack: Boolean, liquidGlass: Boolean): Color {
    return when {
        liquidGlass && pureBlack -> Color.Black.copy(alpha = 0.70f)
        liquidGlass              -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.78f)
        pureBlack                -> Color.Black
        else                     -> MaterialTheme.colorScheme.surfaceContainer
    }
}

@Composable
private fun floatingToolbarFabContainerColor(pureBlack: Boolean, liquidGlass: Boolean): Color {
    return when {
        liquidGlass && pureBlack -> Color.White.copy(alpha = 0.16f)
        liquidGlass              -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
        pureBlack                -> Color.White.copy(alpha = 0.12f)
        else                     -> MaterialTheme.colorScheme.tertiaryContainer
    }
}

@Composable
private fun floatingToolbarFabContentColor(pureBlack: Boolean, liquidGlass: Boolean): Color {
    return when {
        liquidGlass && pureBlack -> Color.White
        liquidGlass              -> MaterialTheme.colorScheme.onTertiaryContainer
        pureBlack                -> Color.White
        else                     -> MaterialTheme.colorScheme.onTertiaryContainer
    }
}

@Composable
private fun floatingToolbarSelectedItemContainerColor(pureBlack: Boolean, liquidGlass: Boolean): Color {
    return when {
        liquidGlass && pureBlack -> Color.White.copy(alpha = 0.18f)
        liquidGlass              -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f)
        pureBlack                -> Color.White.copy(alpha = 0.12f)
        else                     -> MaterialTheme.colorScheme.secondaryContainer
    }
}

@Composable
private fun floatingToolbarSelectedItemContentColor(pureBlack: Boolean, liquidGlass: Boolean): Color {
    return when {
        liquidGlass -> MaterialTheme.colorScheme.onSecondaryContainer  // igual en dark/light/pureBlack
        pureBlack   -> Color.White
        else        -> MaterialTheme.colorScheme.onSecondaryContainer
    }
}

@Composable
private fun floatingToolbarItemContentColor(pureBlack: Boolean, liquidGlass: Boolean): Color {
    return when {
        liquidGlass && pureBlack -> Color.White.copy(alpha = 0.85f)
        liquidGlass              -> MaterialTheme.colorScheme.onSurface  // sin alpha — máxima visibilidad
        pureBlack                -> Color.White.copy(alpha = 0.82f)
        else                     -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

// Estas dos no cambian con liquidGlass (solo se usan en el DropdownMenu)
@Composable
private fun floatingToolbarMenuIconContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun floatingToolbarMenuIconContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
}