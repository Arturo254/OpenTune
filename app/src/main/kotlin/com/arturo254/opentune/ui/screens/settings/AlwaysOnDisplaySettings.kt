@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.arturo254.opentune.constants.AodClockFormatKey
import com.arturo254.opentune.constants.AodControlStyle
import com.arturo254.opentune.constants.AodControlStyleKey
import com.arturo254.opentune.constants.AodAutoActivationKey
import com.arturo254.opentune.constants.AodDarknessKey
import com.arturo254.opentune.constants.AodFullscreenKey
import com.arturo254.opentune.constants.AodShowArtistKey
import com.arturo254.opentune.constants.AodShowClockKey
import com.arturo254.opentune.constants.AodShowControlsKey
import com.arturo254.opentune.constants.AodShowProgressKey
import com.arturo254.opentune.constants.AodShowTimeKey
import com.arturo254.opentune.constants.AodShowTitleKey
import com.arturo254.opentune.constants.AodSpotlightIntensityKey
import com.arturo254.opentune.constants.AodSpotlightPulseKey
import com.arturo254.opentune.constants.AodStyle
import com.arturo254.opentune.constants.AodStyleKey
import com.arturo254.opentune.constants.AodTextScaleKey
import com.arturo254.opentune.constants.AodTransitionDurationKey
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.PreferenceGroupTitle
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// ── Shapes (mismos que tenías, no cambian) ────────────────────────────────

private val squircleShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2.0; val cy = size.height / 2.0
    val rx = size.width / 2.0; val ry = size.height / 2.0
    val n = 4.0; val steps = 120
    for (i in 0..steps) {
        val t = 2.0 * PI * i / steps
        val ct = cos(t); val st = sin(t)
        val x = (cx + rx * (if (ct >= 0) 1.0 else -1.0) * ct.absoluteValue.pow(2.0 / n)).toFloat()
        val y = (cy + ry * (if (st >= 0) 1.0 else -1.0) * st.absoluteValue.pow(2.0 / n)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val diamondShape: Shape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f); lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height); lineTo(0f, size.height / 2f); close()
}

private val hexagonShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f; val cy = size.height / 2f
    val r = minOf(cx, cy) * 0.96f
    for (i in 0..5) {
        val a = (PI / 180.0 * (60.0 * i - 30.0)).toFloat()
        if (i == 0) moveTo(cx + r * cos(a), cy + r * sin(a))
        else lineTo(cx + r * cos(a), cy + r * sin(a))
    }
    close()
}

private val starShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f; val cy = size.height / 2f
    val outerR = minOf(cx, cy) * 0.96f; val innerR = outerR * 0.42f
    for (i in 0 until 10) {
        val a = (PI * i / 5.0 - PI / 2.0).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        if (i == 0) moveTo(cx + r * cos(a), cy + r * sin(a))
        else lineTo(cx + r * cos(a), cy + r * sin(a))
    }
    close()
}

private val archShape: Shape = GenericShape { size, _ ->
    moveTo(0f, size.width / 2f)
    arcTo(Rect(0f, 0f, size.width, size.width), 180f, 180f, false)
    lineTo(size.width, size.height); lineTo(0f, size.height); close()
}

private val petalShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2.0; val cy = size.height / 2.0
    val r = minOf(size.width, size.height) / 2.0 * 0.92
    var started = false
    for (i in 0..240) {
        val t = 2.0 * PI * i / 240
        val rT = r * cos(2.0 * t).absoluteValue
        val x = (cx + rT * cos(t)).toFloat(); val y = (cy + rT * sin(t)).toFloat()
        if (!started) { moveTo(x, y); started = true } else lineTo(x, y)
    }
    close()
}

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

enum class AodAutoTimeout(val seconds: Int, val labelRes: Int) {
    NEVER(0, R.string.aod_auto_never),
    SECONDS_15(15, R.string.aod_auto_15s),
    SECONDS_30(30, R.string.aod_auto_30s),
    MINUTE_1(60, R.string.aod_auto_1m),
    MINUTE_2(120, R.string.aod_auto_2m)
}

// ═══════════════════════════════════════════════════════════════════════════
//  PANTALLA PRINCIPAL DE AJUSTES AOD
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AODSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    // ── Preferencias ─────────────────────────────────────────────────────
    val (rawStyle, setRawStyle) = rememberPreference(AodStyleKey, AodStyle.CLASSIC.name)
    val (rawShape, setRawShape) = rememberPreference(AodArtShapeKey, AodArtShape.ROUNDED.name)
    val (darkness, setDarkness) = rememberPreference(AodDarknessKey, 0.55f)
    val (artSize, setArtSize) = rememberPreference(AodArtSizeKey, 0.65f)
    val (showTitle, setShowTitle) = rememberPreference(AodShowTitleKey, true)
    val (showArtist, setShowArtist) = rememberPreference(AodShowArtistKey, true)
    val (showTime, setShowTime) = rememberPreference(AodShowTimeKey, true)
    val (showProgress, setShowProgress) = rememberPreference(AodShowProgressKey, true)
    val (showControls, setShowControls) = rememberPreference(AodShowControlsKey, true)
    val (fullscreen, setFullscreen) = rememberPreference(AodFullscreenKey, true)
    val (autoTimeoutSecs, setAutoTimeoutSecs) = rememberPreference(AodAutoActivationKey, 0)
    val (spotlightInt, setSpotlightInt) = rememberPreference(AodSpotlightIntensityKey, 0.75f)
    val (spotlightPulse, setSpotlightPulse) = rememberPreference(AodSpotlightPulseKey, true)
    val (transitionDur, setTransitionDur) = rememberPreference(AodTransitionDurationKey, 700)
    val (rawControlStyle, setRawControlStyle) = rememberPreference(
        AodControlStyleKey,
        AodControlStyle.ROUNDED.name
    )
    val (textScale, setTextScale) = rememberPreference(AodTextScaleKey, 1.0f)
    val (showClock, setShowClock) = rememberPreference(AodShowClockKey, false)
    val (clockFormat24h, setClockFormat24h) = rememberPreference(AodClockFormatKey, true)

    val currentStyle = remember(rawStyle) {
        runCatching { AodStyle.valueOf(rawStyle) }.getOrDefault(AodStyle.CLASSIC)
    }
    val currentShape = remember(rawShape) {
        runCatching { AodArtShape.valueOf(rawShape) }.getOrDefault(AodArtShape.ROUNDED)
    }
    val currentControlStyle = remember(rawControlStyle) {
        runCatching { AodControlStyle.valueOf(rawControlStyle) }.getOrDefault(AodControlStyle.ROUNDED)
    }

    var autoTimeoutIndex by remember(autoTimeoutSecs) {
        mutableIntStateOf(
            when (autoTimeoutSecs) {
                0 -> 0; 15 -> 1; 30 -> 2; 60 -> 3; 120 -> 4; else -> 2
            }
        )
    }
    val autoTimeouts = listOf(
        AodAutoTimeout.NEVER, AodAutoTimeout.SECONDS_15, AodAutoTimeout.SECONDS_30,
        AodAutoTimeout.MINUTE_1, AodAutoTimeout.MINUTE_2
    )

    Column(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {

        Spacer(Modifier.height(8.dp))

        // ── SECCIÓN: Pantalla ─────────────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_fullscreen_title)) {
            AodSwitchRow(
                icon = R.drawable.fullscreen,
                title = stringResource(R.string.aod_fullscreen_label),
                description = stringResource(R.string.aod_fullscreen_description),
                checked = fullscreen,
                onChanged = setFullscreen,
            )
        }

        // ── SECCIÓN: Auto-activación ──────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_auto_activation_title)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.aod_auto_activation_label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    ExpressivePill(text = stringResource(autoTimeouts[autoTimeoutIndex].labelRes))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        " ", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp)
                    )
                    SquigglySlider(
                        value = autoTimeoutIndex.toFloat(),
                        onValueChange = { v ->
                            val idx = (v + 0.5f).toInt().coerceIn(0, 4)
                            autoTimeoutIndex = idx
                            setAutoTimeoutSecs(autoTimeouts[idx].seconds)
                        },
                        valueRange = 0f..4f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        stringResource(R.string.aod_auto_2m_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.aod_auto_activation_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── SECCIÓN: Estilo visual ────────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_style_title)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                items(AodStyle.entries) { style ->
                    AodStyleCard(
                        style = style,
                        selected = style == currentStyle,
                        onClick = { setRawStyle(style.name) }
                    )
                }
            }

            // Opciones extra sólo para SPOTLIGHT
            AnimatedVisibility(
                visible = currentStyle == AodStyle.SPOTLIGHT,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Intensidad
                    AodSliderRow(
                        label = stringResource(R.string.aod_spotlight_intensity_label),
                        valueLabel = "${(spotlightInt * 100).toInt()}%",
                        value = spotlightInt,
                        onValueChange = setSpotlightInt,
                        valueRange = 0.15f..1.0f,
                        startLabel = stringResource(R.string.aod_spotlight_intensity_low),
                        endLabel = stringResource(R.string.aod_spotlight_intensity_high),
                    )
                    Spacer(Modifier.height(8.dp))
                    AodSwitchRow(
                        icon = R.drawable.timer,
                        title = stringResource(R.string.aod_spotlight_pulse_label),
                        checked = spotlightPulse,
                        onChanged = setSpotlightPulse,
                    )
                }
            }
        }

// ── SECCIÓN: Forma de la portada ──────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_shape_title)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                items(AodArtShape.entries) { shape ->
                    AodShapeCard(
                        artShape = shape,
                        selected = shape == currentShape,
                        onClick = { setRawShape(shape.name) }
                    )
                }
            }
        }

        // ── SECCIÓN: Controles ────────────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_controls_title)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.aod_controls_style_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(AodControlStyle.entries) { cs ->
                        ControlStyleChip(
                            style = cs,
                            selected = cs == currentControlStyle,
                            onClick = { setRawControlStyle(cs.name) }
                        )
                    }
                }
            }
        }

        // ── SECCIÓN: Tipografía ───────────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_typography_title)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AodSliderRow(
                    label = stringResource(R.string.aod_art_size_label),
                    valueLabel = "${(artSize * 100).toInt()}%",
                    value = artSize,
                    onValueChange = setArtSize,
                    valueRange = 0.40f..1.0f,
                    startLabel = "40%",
                    endLabel = "100%",
                    description = stringResource(R.string.aod_art_size_description),
                )
                Spacer(Modifier.height(12.dp))
                AodSliderRow(
                    label = stringResource(R.string.aod_text_size_label),
                    valueLabel = "${(textScale * 100).roundToInt()}%",
                    value = textScale,
                    onValueChange = setTextScale,
                    valueRange = 0.75f..1.40f,
                    startLabel = "75%",
                    endLabel = "140%",
                    description = stringResource(R.string.aod_text_size_description),
                )
            }
        }

        // ── SECCIÓN: Fondo y brillo ───────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_darkness_title)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AodSliderRow(
                    label = stringResource(R.string.aod_darkness_label),
                    valueLabel = "${(darkness * 100).toInt()}%",
                    value = darkness,
                    onValueChange = setDarkness,
                    valueRange = 0f..1f,
                    startLabel = "0%",
                    endLabel = "100%",
                    description = stringResource(R.string.aod_darkness_description),
                )
            }
        }

        // ── SECCIÓN: Transiciones ─────────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_transitions_title)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AodSliderRow(
                    label = stringResource(R.string.aod_transition_duration_label),
                    valueLabel = "${transitionDur}ms",
                    value = transitionDur.toFloat(),
                    onValueChange = { setTransitionDur(it.roundToInt()) },
                    valueRange = 200f..1500f,
                    startLabel = stringResource(R.string.aod_transition_duration_fast),
                    endLabel = stringResource(R.string.aod_transition_duration_slow),
                    description = stringResource(R.string.aod_transition_duration_description),
                )
            }
        }

        // ── SECCIÓN: Reloj del sistema ────────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_system_clock_title)) {
            AodSwitchRow(
                icon = R.drawable.timer,
                title = stringResource(R.string.aod_show_clock_label),
                description = stringResource(R.string.aod_show_clock_description),
                checked = showClock,
                onChanged = setShowClock,
            )
            AnimatedVisibility(
                visible = showClock,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                AodSwitchRow(
                    icon = R.drawable.timer,
                    title = stringResource(R.string.aod_clock_format_24h_label),
                    description = stringResource(R.string.aod_clock_format_24h_description),
                    checked = clockFormat24h,
                    onChanged = setClockFormat24h,
                )
            }
        }

        // ── SECCIÓN: Elementos visibles ───────────────────────────────────
        AodSettingsSection(title = stringResource(R.string.aod_visible_elements_title)) {
            AodSwitchRow(
                icon = R.drawable.text_fields,
                title = stringResource(R.string.aod_show_title),
                checked = showTitle,
                onChanged = setShowTitle,
            )
            AodSwitchRow(
                icon = R.drawable.artist,
                title = stringResource(R.string.aod_show_artist),
                checked = showArtist,
                onChanged = setShowArtist,
            )
            AodSwitchRow(
                icon = R.drawable.timer,
                title = stringResource(R.string.aod_show_time),
                description = stringResource(R.string.aod_show_time_description),
                checked = showTime,
                onChanged = setShowTime,
            )
            AodSwitchRow(
                icon = R.drawable.sliders,
                title = stringResource(R.string.aod_show_progress),
                checked = showProgress,
                onChanged = setShowProgress,
            )
            AodSwitchRow(
                icon = R.drawable.queue_music,
                title = stringResource(R.string.aod_show_controls),
                description = stringResource(R.string.aod_show_controls_description),
                checked = showControls,
                onChanged = setShowControls,
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.aod_screen_title)) },
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

// ═══════════════════════════════════════════════════════════════════════════
//  COMPONENTES DE UI M3 Expressive
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Sección con título y contenido en una tarjeta M3 con bordes suaves.
 */
@Composable
private fun AodSettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

/**
 * Fila con switch estilo M3 Expressive.
 */
@Composable
private fun AodSwitchRow(
    icon: Int,
    title: String,
    description: String? = null,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onChanged(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

/**
 * Fila de slider con etiqueta de valor como pill M3 Expressive.
 */
@Composable
private fun AodSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    startLabel: String,
    endLabel: String,
    description: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        ExpressivePill(text = valueLabel)
    }
    if (description != null) {
        Spacer(Modifier.height(2.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            startLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        Text(
            endLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}

/**
 * Pill / chip con color primario — M3 Expressive style.
 */
@Composable
private fun ExpressivePill(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Chip de estilo de controles con preview visual.
 */
@Composable
private fun ControlStyleChip(
    style: AodControlStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "control_chip_border"
    )
    val bgColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val label = when (style) {
        AodControlStyle.ROUNDED -> stringResource(R.string.aod_control_style_rounded)
        AodControlStyle.SQUARE -> stringResource(R.string.aod_control_style_square)
        AodControlStyle.ACCENT -> stringResource(R.string.aod_control_style_accent)
        AodControlStyle.MINIMAL_FLAT -> stringResource(R.string.aod_control_style_minimal_flat)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .width(72.dp)
    ) {
        // Mini preview del estilo de botón
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .then(
                    when (style) {
                        AodControlStyle.ROUNDED, AodControlStyle.ACCENT ->
                            Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(0.18f)
                                )

                        AodControlStyle.SQUARE ->
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(0.18f)
                                )

                        AodControlStyle.MINIMAL_FLAT ->
                            Modifier
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(0.06f)
                                )
                    }
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = if (selected) {
                    when (style) {
                        AodControlStyle.MINIMAL_FLAT ->
                            MaterialTheme.colorScheme.primary

                        else -> MaterialTheme.colorScheme.onPrimary
                    }
                } else MaterialTheme.colorScheme.onSurface.copy(0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  CARD: Estilo AOD (horizontal scroll)
// ═══════════════════════════════════════════════════════════════════════════

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
        label = "style_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "style_scale"
    )

    val label = when (style) {
        AodStyle.CLASSIC    -> stringResource(R.string.aod_style_classic)
        AodStyle.BACKGROUND -> stringResource(R.string.aod_style_background)
        AodStyle.MINIMAL    -> stringResource(R.string.aod_style_minimal)
        AodStyle.LARGE      -> stringResource(R.string.aod_style_large)
        AodStyle.SPOTLIGHT  -> stringResource(R.string.aod_style_spotlight)
    }
    val description = when (style) {
        AodStyle.CLASSIC    -> stringResource(R.string.aod_style_classic_desc)
        AodStyle.BACKGROUND -> stringResource(R.string.aod_style_background_desc)
        AodStyle.MINIMAL    -> stringResource(R.string.aod_style_minimal_desc)
        AodStyle.LARGE      -> stringResource(R.string.aod_style_large_desc)
        AodStyle.SPOTLIGHT  -> stringResource(R.string.aod_style_spotlight_desc)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .width(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(0.25f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Preview miniatura
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(Color(0xFF070707))
        ) {
            AodStylePreview(style = style)
        }

        // Nombre + descripción
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
            )
        }
    }
}

/**
 * Preview visual de cada estilo en miniaturas — extraído para limpieza.
 */
@Composable
private fun AodStylePreview(style: AodStyle) {
    when (style) {
        AodStyle.CLASSIC -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(48.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFF6650A4).copy(0.22f), Color.Transparent)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(0.18f))
                    )
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(0.70f))
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(0.30f))
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(0.15f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(0.80f))
                )
            }
        }

        AodStyle.BACKGROUND -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF6A4C93).copy(0.8f),
                                Color(0xFF1A237E).copy(0.5f),
                                Color.Black
                            )
                        )
                    )
            )
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(0.85f)
                                )
                            )
                        )
                        .padding(bottom = 8.dp, top = 16.dp)
                        .padding(horizontal = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.85f))
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.40f))
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            6.dp,
                            Alignment.CenterHorizontally
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.25f))
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.90f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.25f))
                        )
                    }
                }
            }
        }

        AodStyle.MINIMAL -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White.copy(0.30f))
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(0.5.dp)
                        .background(Color.White.copy(0.15f))
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(0.75f))
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(0.12f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(1.dp)
                        .background(Color.White.copy(0.65f))
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        10.dp,
                        Alignment.CenterHorizontally
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.20f))
                    )
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.80f))
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.20f))
                    )
                }
            }
        }

        AodStyle.LARGE -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(0.62f)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF8BC34A).copy(0.6f),
                                    Color(0xFF388E3C).copy(0.4f),
                                    Color(0xFF1B5E20).copy(0.2f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(0.45f)
                        .align(Alignment.Center)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.85f))
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.35f))
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            6.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.20f))
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.90f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.20f))
                        )
                    }
                }
            }
        }

        AodStyle.SPOTLIGHT -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height * 0.40f)
                    // Glow exterior con tono violeta/azul para el preview
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7C4DFF).copy(0.35f),
                                Color(0xFF448AFF).copy(0.12f),
                                Color.Transparent
                            ),
                            center = c, radius = size.minDimension * 0.60f
                        ),
                        center = c, radius = size.minDimension * 0.60f
                    )
                    // Corona interior
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(0.40f),
                                Color(0xFF7C4DFF).copy(0.55f),
                                Color.Transparent
                            ),
                            center = c, radius = size.minDimension * 0.30f
                        ),
                        center = c, radius = size.minDimension * 0.30f
                    )
                    // Anillo
                    drawCircle(
                        color = Color(0xFF7C4DFF).copy(0.40f),
                        center = c, radius = size.minDimension * 0.34f,
                        style = Stroke(width = 1.0f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(0.30f))
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.60f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.80f))
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.32f))
                    )
                }
            } // end Box wrapper
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  CARD: Forma de portada
// ═══════════════════════════════════════════════════════════════════════════

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
        label = "shape_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "shape_scale"
    )

    val label = when (artShape) {
        AodArtShape.ROUNDED  -> stringResource(R.string.aod_shape_rounded)
        AodArtShape.CIRCLE   -> stringResource(R.string.aod_shape_circle)
        AodArtShape.SQUIRCLE -> stringResource(R.string.aod_shape_squircle)
        AodArtShape.DIAMOND  -> stringResource(R.string.aod_shape_diamond)
        AodArtShape.HEXAGON  -> stringResource(R.string.aod_shape_hexagon)
        AodArtShape.STAR     -> stringResource(R.string.aod_shape_star)
        AodArtShape.ARCH     -> stringResource(R.string.aod_shape_arch)
        AodArtShape.PETAL    -> stringResource(R.string.aod_shape_petal)
    }
    val shape = artShape.toShape()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(0.30f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(shape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(0.20f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .clip(shape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(0.50f)
                        else MaterialTheme.colorScheme.onSurface.copy(0.12f)
                    )
            )
        }
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