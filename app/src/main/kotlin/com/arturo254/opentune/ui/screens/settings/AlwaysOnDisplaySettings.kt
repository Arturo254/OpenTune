@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AodArtShape
import com.arturo254.opentune.constants.AodArtShapeKey
import com.arturo254.opentune.constants.AodArtSizeKey
import com.arturo254.opentune.constants.AodDarknessKey
import com.arturo254.opentune.constants.AodShowArtistKey
import com.arturo254.opentune.constants.AodShowControlsKey
import com.arturo254.opentune.constants.AodShowProgressKey
import com.arturo254.opentune.constants.AodShowTimeKey
import com.arturo254.opentune.constants.AodShowTitleKey
import com.arturo254.opentune.constants.AodStyle
import com.arturo254.opentune.constants.AodStyleKey
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.PreferenceGroupTitle
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


// ═════════════════════════════════════════════════════════════════════════════
//  Shape definitions — M3 Expressive inspired
// ═════════════════════════════════════════════════════════════════════════════

/** Squircle / superelipse (|x|^(2/n) + |y|^(2/n) = r, n = 4) */
private val squircleShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2.0
    val cy = size.height / 2.0
    val rx = size.width / 2.0
    val ry = size.height / 2.0
    val n = 4.0
    val steps = 120
    for (i in 0..steps) {
        val t = 2.0 * PI * i / steps
        val ct = cos(t); val st = sin(t)
        val sx = if (ct >= 0) 1.0 else -1.0
        val sy = if (st >= 0) 1.0 else -1.0
        val x = (cx + rx * sx * ct.absoluteValue.pow(2.0 / n)).toFloat()
        val y = (cy + ry * sy * st.absoluteValue.pow(2.0 / n)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Diamante (cuadrado rotado 45°) */
private val diamondShape: Shape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

/** Hexágono regular */
private val hexagonShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f; val cy = size.height / 2f
    val r = minOf(cx, cy) * 0.96f
    for (i in 0..5) {
        val angle = (PI / 180.0 * (60.0 * i - 30.0)).toFloat()
        val x = cx + r * cos(angle); val y = cy + r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Estrella de 5 puntas */
private val starShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f; val cy = size.height / 2f
    val outerR = minOf(cx, cy) * 0.96f
    val innerR = outerR * 0.42f
    for (i in 0 until 10) {
        val angle = (PI * i / 5.0 - PI / 2.0).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        val x = cx + r * cos(angle); val y = cy + r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Arco — semicírculo en la parte superior, lados rectos abajo */
private val archShape: Shape = GenericShape { size, _ ->
    val r = size.width / 2f
    moveTo(0f, r)
    arcTo(
        rect = Rect(0f, 0f, size.width, size.width),
        startAngleDegrees = 180f,
        sweepAngleDegrees = 180f,
        forceMoveTo = false
    )
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/** Flor de 4 pétalos (curva rosa r = a·|cos(2θ)|) */
private val petalShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2.0; val cy = size.height / 2.0
    val r = minOf(size.width, size.height) / 2.0 * 0.92
    var started = false
    for (i in 0..240) {
        val t = 2.0 * PI * i / 240
        val rT = r * cos(2.0 * t).absoluteValue
        val x = (cx + rT * cos(t)).toFloat()
        val y = (cy + rT * sin(t)).toFloat()
        if (!started) { moveTo(x, y); started = true } else lineTo(x, y)
    }
    close()
}

// ─────────────────────────────────────────────────────────────────────────────
/** Convierte el enum al Shape de Compose correspondiente */
fun AodArtShape.toShape(): Shape = when (this) {
    AodArtShape.ROUNDED  -> RoundedCornerShape(24.dp)
    AodArtShape.CIRCLE   -> CircleShape
    AodArtShape.SQUIRCLE -> squircleShape
    AodArtShape.DIAMOND  -> diamondShape
    AodArtShape.HEXAGON  -> hexagonShape
    AodArtShape.STAR     -> starShape
    AodArtShape.ARCH     -> archShape
    AodArtShape.PETAL    -> petalShape
}

// ═════════════════════════════════════════════════════════════════════════════
//  Settings Screen
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AODSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    // ── Preferences ─────────────────────────────────────────────────────────
    val (rawStyle, setRawStyle)     = rememberPreference(AodStyleKey,        AodStyle.CLASSIC.name)
    val (rawShape, setRawShape)     = rememberPreference(AodArtShapeKey,     AodArtShape.ROUNDED.name)
    val (darkness, setDarkness)     = rememberPreference(AodDarknessKey,     0.55f)
    val (artSize, setArtSize)       = rememberPreference(AodArtSizeKey,      0.65f)
    val (showTitle, setShowTitle)   = rememberPreference(AodShowTitleKey,    true)
    val (showArtist, setShowArtist) = rememberPreference(AodShowArtistKey,   true)
    val (showTime, setShowTime)     = rememberPreference(AodShowTimeKey,     true)
    val (showProgress, setShowProgress) = rememberPreference(AodShowProgressKey, true)
    val (showControls, setShowControls) = rememberPreference(AodShowControlsKey, true)

    val currentStyle = remember(rawStyle) {
        runCatching { AodStyle.valueOf(rawStyle) }.getOrDefault(AodStyle.CLASSIC)
    }
    val currentShape = remember(rawShape) {
        runCatching { AodArtShape.valueOf(rawShape) }.getOrDefault(AodArtShape.ROUNDED)
    }

    // ── UI ──────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Estilo AOD ───────────────────────────────────────────────────────
        PreferenceGroupTitle(title = "Estilo AOD")

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            items(AodStyle.entries) { style ->
                AodStyleCard(
                    style = style,
                    selected = style == currentStyle,
                    onClick = { setRawStyle(style.name) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Forma de la portada ──────────────────────────────────────────────
        PreferenceGroupTitle(title = "Forma de la portada")

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 2 rows × ~90dp
        ) {
            items(AodArtShape.entries) { shape ->
                AodShapeCard(
                    artShape = shape,
                    selected = shape == currentShape,
                    onClick = { setRawShape(shape.name) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Oscuridad del fondo ──────────────────────────────────────────────
        PreferenceGroupTitle(title = "Oscuridad del fondo")

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nivel de oscuridad",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(darkness * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Controla cuán oscuro es el fondo en estilos Fondo y Spotlight",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "0%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = darkness,
                    onValueChange = setDarkness,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Tamaño de la portada ─────────────────────────────────────────────
        PreferenceGroupTitle(title = "Tamaño de la portada")

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tamaño relativo",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(artSize * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Porcentaje del ancho de pantalla que ocupa la portada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "40%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = artSize,
                    onValueChange = setArtSize,
                    valueRange = 0.40f..1.0f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Elementos visibles ───────────────────────────────────────────────
        PreferenceGroupTitle(title = "Elementos visibles")

        SwitchPreference(
            title = { Text("Mostrar título") },
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
            checked = showTitle,
            onCheckedChange = setShowTitle,
        )

        SwitchPreference(
            title = { Text("Mostrar artista") },
            icon = { Icon(painterResource(R.drawable.artist), null) },
            checked = showArtist,
            onCheckedChange = setShowArtist,
        )

        SwitchPreference(
            title = { Text("Mostrar tiempo") },
            description = "Etiquetas de posición y duración",
            icon = { Icon(painterResource(R.drawable.timer), null) },
            checked = showTime,
            onCheckedChange = setShowTime,
        )

        SwitchPreference(
            title = { Text("Mostrar barra de progreso") },
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            checked = showProgress,
            onCheckedChange = setShowProgress,
        )

        SwitchPreference(
            title = { Text("Mostrar controles") },
            description = "Anterior, Play/Pausa, Siguiente",
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = showControls,
            onCheckedChange = setShowControls,
        )

        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text("Always On Display") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Style card — miniatura visual del estilo AOD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AodStyleCard(
    style: AodStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(250),
        label = "aod_style_border"
    )
    val label = when (style) {
        AodStyle.CLASSIC    -> "Clásico"
        AodStyle.BACKGROUND -> "Fondo"
        AodStyle.MINIMAL    -> "Minimal"
        AodStyle.LARGE      -> "Grande"
        AodStyle.SPOTLIGHT  -> "Spotlight"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .width(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(bottom = 8.dp)
    ) {
        // ── Miniatura ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.Black)
        ) {
            when (style) {
                AodStyle.CLASSIC -> {
                    // Portada pequeña centrada
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                }
                AodStyle.BACKGROUND -> {
                    // Gradiente que simula arte difuminado
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF7986CB).copy(alpha = 0.6f),
                                        Color(0xFF303F9F).copy(alpha = 0.3f),
                                        Color.Black
                                    )
                                )
                            )
                    )
                    // Overlay oscuro
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                    // Portada pequeña centrada encima
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }
                AodStyle.MINIMAL -> {
                    // Solo barra de progreso y texto tiny
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.30f))
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                    }
                }
                AodStyle.LARGE -> {
                    // Portada que casi llena el card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize(0.85f)
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp, start = 6.dp, end = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                }
                AodStyle.SPOTLIGHT -> {
                    // Halo radial + portada pequeña
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.55f
                            ),
                            radius = size.minDimension * 0.55f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }
            }
        }

        // ── Etiqueta ─────────────────────────────────────────────────────────
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shape card — muestra el shape aplicado como clip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AodShapeCard(
    artShape: AodArtShape,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(250),
        label = "aod_shape_border"
    )
    val label = when (artShape) {
        AodArtShape.ROUNDED  -> "Redondo"
        AodArtShape.CIRCLE   -> "Círculo"
        AodArtShape.SQUIRCLE -> "Squircle"
        AodArtShape.DIAMOND  -> "Diamante"
        AodArtShape.HEXAGON  -> "Hexágono"
        AodArtShape.STAR     -> "Estrella"
        AodArtShape.ARCH     -> "Arco"
        AodArtShape.PETAL    -> "Flor"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        // ── Shape preview ─────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(artShape.toShape())
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            // Inner subtle detail
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .clip(artShape.toShape())
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                    )
            )
        }

        // ── Etiqueta ──────────────────────────────────────────────────────────
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}