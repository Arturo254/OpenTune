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
import com.arturo254.opentune.constants.AodAutoActivationKey
import com.arturo254.opentune.constants.AodDarknessKey
import com.arturo254.opentune.constants.AodFullscreenKey
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
import me.saket.squiggles.SquigglySlider
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

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
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
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
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
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

// AOD Auto-activation timeout values
enum class AodAutoTimeout(val seconds: Int, val labelRes: Int) {
    NEVER(0, R.string.aod_auto_never),
    SECONDS_15(15, R.string.aod_auto_15s),
    SECONDS_30(30, R.string.aod_auto_30s),
    MINUTE_1(60, R.string.aod_auto_1m),
    MINUTE_2(120, R.string.aod_auto_2m)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AODSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (rawStyle,   setRawStyle)   = rememberPreference(AodStyleKey,        AodStyle.CLASSIC.name)
    val (rawShape,   setRawShape)   = rememberPreference(AodArtShapeKey,     AodArtShape.ROUNDED.name)
    val (darkness,   setDarkness)   = rememberPreference(AodDarknessKey,     0.55f)
    val (artSize,    setArtSize)    = rememberPreference(AodArtSizeKey,      0.65f)
    val (showTitle,  setShowTitle)  = rememberPreference(AodShowTitleKey,    true)
    val (showArtist, setShowArtist) = rememberPreference(AodShowArtistKey,   true)
    val (showTime,   setShowTime)   = rememberPreference(AodShowTimeKey,     true)
    val (showProgress, setShowProgress) = rememberPreference(AodShowProgressKey, true)
    val (showControls, setShowControls) = rememberPreference(AodShowControlsKey, true)
    val (fullscreen, setFullscreen) = rememberPreference(AodFullscreenKey,   true)
    val (autoTimeoutSeconds, setAutoTimeoutSeconds) = rememberPreference(AodAutoActivationKey, 30)

    val currentStyle = remember(rawStyle) {
        runCatching { AodStyle.valueOf(rawStyle) }.getOrDefault(AodStyle.CLASSIC)
    }
    val currentShape = remember(rawShape) {
        runCatching { AodArtShape.valueOf(rawShape) }.getOrDefault(AodArtShape.ROUNDED)
    }

    var autoTimeoutIndex by remember(autoTimeoutSeconds) {
        mutableIntStateOf(
            when (autoTimeoutSeconds) {
                0 -> 0
                15 -> 1
                30 -> 2
                60 -> 3
                120 -> 4
                else -> 2
            }
        )
    }

    val autoTimeouts = listOf(
        AodAutoTimeout.NEVER,
        AodAutoTimeout.SECONDS_15,
        AodAutoTimeout.SECONDS_30,
        AodAutoTimeout.MINUTE_1,
        AodAutoTimeout.MINUTE_2
    )

    Column(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        // Fullscreen mode preference (new)
        PreferenceGroupTitle(title = stringResource(R.string.aod_fullscreen_title))

        SwitchPreference(
            title = { Text(stringResource(R.string.aod_fullscreen_label)) },
            description = stringResource(R.string.aod_fullscreen_description),
            icon = { Icon(painterResource(R.drawable.fullscreen), null) },
            checked = fullscreen, onCheckedChange = setFullscreen,
        )

        Spacer(Modifier.height(8.dp))

        PreferenceGroupTitle(title = stringResource(R.string.aod_auto_activation_title))

        // Auto-activation timeout selector with nice animation
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
                Text(
                    text = stringResource(autoTimeouts[autoTimeoutIndex].labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))

            // Custom slider for timeout values
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp)
                )
                SquigglySlider(
                    value = autoTimeoutIndex.toFloat(),
                    onValueChange = { newIndex ->
                        // Redondear al valor más cercano (0,1,2,3,4)
                        val rounded = (newIndex + 0.5f).toInt().coerceIn(0, 4)
                        autoTimeoutIndex = rounded
                        val newSeconds = autoTimeouts[rounded].seconds
                        setAutoTimeoutSeconds(newSeconds)
                    },
                    valueRange = 0f..4f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text(
                    text = stringResource(R.string.aod_auto_2m_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.aod_auto_activation_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        PreferenceGroupTitle(title = stringResource(R.string.aod_style_title))

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) {
            items(AodStyle.entries) { style ->
                AodStyleCard(
                    style = style,
                    selected = style == currentStyle,
                    onClick = { setRawStyle(style.name) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        PreferenceGroupTitle(title = stringResource(R.string.aod_shape_title))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            items(AodArtShape.entries) { shape ->
                AodShapeCard(
                    artShape = shape,
                    selected = shape == currentShape,
                    onClick = { setRawShape(shape.name) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        PreferenceGroupTitle(title = stringResource(R.string.aod_darkness_title))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.aod_darkness_label),
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.aod_darkness_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("0%", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = darkness, onValueChange = setDarkness,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("100%", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(4.dp))

        PreferenceGroupTitle(title = stringResource(R.string.aod_art_size_title))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.aod_art_size_label),
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
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.aod_art_size_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("40%", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = artSize, onValueChange = setArtSize,
                    valueRange = 0.40f..1.0f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("100%", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(4.dp))

        PreferenceGroupTitle(title = stringResource(R.string.aod_visible_elements_title))

        SwitchPreference(
            title = { Text(stringResource(R.string.aod_show_title)) },
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
            checked = showTitle, onCheckedChange = setShowTitle,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.aod_show_artist)) },
            icon = { Icon(painterResource(R.drawable.artist), null) },
            checked = showArtist, onCheckedChange = setShowArtist,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.aod_show_time)) },
            description = stringResource(R.string.aod_show_time_description),
            icon = { Icon(painterResource(R.drawable.timer), null) },
            checked = showTime, onCheckedChange = setShowTime,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.aod_show_progress)) },
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            checked = showProgress, onCheckedChange = setShowProgress,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.aod_show_controls)) },
            description = stringResource(R.string.aod_show_controls_description),
            icon = { Icon(painterResource(R.drawable.queue_music), null) },
            checked = showControls, onCheckedChange = setShowControls,
        )

        Spacer(Modifier.height(16.dp))
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
        animationSpec = tween(250), label = "style_border"
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
            .width(116.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color(0xFF080808))
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            when (style) {
                AodStyle.CLASSIC -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(46.dp)) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color.White.copy(0.10f), Color.Transparent)
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(0.18f))
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(0.70f))
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.48f).height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(0.30f))
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.5.dp)
                            .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(0.15f)))
                        Box(modifier = Modifier.fillMaxWidth(0.55f).height(1.5.dp)
                            .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(0.80f)))
                    }
                }

                AodStyle.BACKGROUND -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF6A4C93).copy(0.8f),
                                        Color(0xFF1A237E).copy(0.5f),
                                        Color(0xFF000000)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(0.7f), Color.Transparent)
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
                                    listOf(Color.Transparent, Color.Black.copy(0.85f))
                                )
                            )
                            .padding(bottom = 8.dp, top = 16.dp)
                            .padding(horizontal = 10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.65f).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.85f)))
                        Spacer(Modifier.height(3.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.45f).height(2.dp)
                            .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(0.40f)))
                        Spacer(Modifier.height(5.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(Color.White.copy(0.25f)))
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape)
                                .background(Color.White.copy(0.90f)))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(Color.White.copy(0.25f)))
                        }
                    }
                }

                AodStyle.MINIMAL -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.5f).height(2.dp)
                            .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(0.30f)))
                        Spacer(Modifier.height(5.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.85f).height(0.5.dp)
                            .background(Color.White.copy(0.15f)))
                        Spacer(Modifier.height(5.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.78f).height(4.dp)
                            .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.75f)))
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(Color.White.copy(0.12f)))
                        Box(modifier = Modifier.fillMaxWidth(0.40f).height(1.dp)
                            .background(Color.White.copy(0.65f)))
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                                .background(Color.White.copy(0.20f)))
                            Box(modifier = Modifier.size(11.dp).clip(CircleShape)
                                .background(Color.White.copy(0.80f)))
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape)
                                .background(Color.White.copy(0.20f)))
                        }
                    }
                }

                AodStyle.LARGE -> {
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
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black)
                                )
                            )
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .padding(horizontal = 10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.65f).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.85f)))
                        Spacer(Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.45f).height(2.dp)
                            .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(0.35f)))
                        Spacer(Modifier.height(5.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(Color.White.copy(0.20f)))
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape)
                                .background(Color.White.copy(0.90f)))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(Color.White.copy(0.20f)))
                        }
                    }
                }

                AodStyle.SPOTLIGHT -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height * 0.42f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(0.06f), Color.Transparent),
                                center = c, radius = size.minDimension * 0.56f
                            ),
                            center = c, radius = size.minDimension * 0.56f
                        )
                        drawCircle(
                            color = Color.White.copy(0.10f),
                            center = c, radius = size.minDimension * 0.34f,
                            style = Stroke(width = 0.8f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(0.18f), Color.Transparent),
                                center = c, radius = size.minDimension * 0.26f
                            ),
                            center = c, radius = size.minDimension * 0.26f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(0.28f))
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 10.dp)
                            .padding(bottom = 9.dp)
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
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color.White.copy(0.32f))
                        )
                    }
                }
            }
        }

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
        animationSpec = tween(250), label = "shape_border"
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(shape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .clip(shape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
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