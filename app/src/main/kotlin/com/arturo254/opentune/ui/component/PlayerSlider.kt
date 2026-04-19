/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */


@file:OptIn(ExperimentalMaterial3Api::class)
package com.arturo254.opentune.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.theme.PlayerSliderColors
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors,
    trackHeight: Dp = 10.dp
) {
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val inactiveTickColor = colors.inactiveTickColor
    val activeTickColor = colors.activeTickColor
    val valueRange = sliderState.valueRange

    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        drawTrack(
            tickFractions = stepsToTickFractions(sliderState.steps),
            activeRangeStart = 0f,
            activeRangeEnd = calcFraction(
                valueRange.start,
                valueRange.endInclusive,
                sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
            ),
            inactiveTrackColor = inactiveTrackColor,
            activeTrackColor = activeTrackColor,
            inactiveTickColor = inactiveTickColor,
            activeTickColor = activeTickColor,
            trackHeight = trackHeight
        )
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight

    val trackStrokeWidth = trackHeight.toPx()
    val tickSize = 2.dp.toPx()

    // Track inactivo
    drawLine(
        color = inactiveTrackColor,
        start = sliderStart,
        end = sliderEnd,
        strokeWidth = trackStrokeWidth,
        cap = StrokeCap.Round
    )

    // Track activo
    val sliderValueEnd = Offset(
        x = sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeEnd,
        y = center.y
    )

    val sliderValueStart = Offset(
        x = sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeStart,
        y = center.y
    )

    drawLine(
        color = activeTrackColor,
        start = sliderValueStart,
        end = sliderValueEnd,
        strokeWidth = trackStrokeWidth,
        cap = StrokeCap.Round
    )

    // Ticks
    tickFractions.forEach { tick ->
        val isInactive = tick > activeRangeEnd || tick < activeRangeStart
        drawCircle(
            color = if (isInactive) inactiveTickColor else activeTickColor,
            center = Offset(
                x = lerp(sliderStart, sliderEnd, tick).x,
                y = center.y
            ),
            radius = tickSize / 2f
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) {
        floatArrayOf()
    } else {
        FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
    }
}

private fun calcFraction(a: Float, b: Float, pos: Float): Float {
    return if (b - a == 0f) 0f else ((pos - a) / (b - a)).coerceIn(0f, 1f)
}

/**
 * Player slider color configuration for consistent styling across all slider types
 * 
 * This object provides standardized color schemes for Default, Squiggly, and Slim sliders
 * used in the music player interface, ensuring visual consistency and proper contrast.
 */
object PlayerSliderColors {

    /**
     * Standard slider colors for all slider types
     * 
     * @param activeColor Color for active track, ticks, and thumb
     * @param inactiveAlpha Alpha transparency for inactive track (default: 0.15f for subtle appearance)
     * @return SliderColors configuration
     */
    @Composable
    fun getSliderColors(
        activeColor: Color,
        inactiveAlpha: Float = 0.15f
    ): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = activeColor,
            activeTickColor = activeColor,
            thumbColor = activeColor,
            inactiveTrackColor = Color.White.copy(alpha = inactiveAlpha)
        )
    }

    /**
     * Default slider colors using button color scheme
     * 
     * @param buttonColor The active button color from player theme
     * @return SliderColors configuration for default slider
     */
    @Composable
    fun standardSliderColors(buttonColor: Color): SliderColors {
        return getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.INACTIVE_TRACK_ALPHA
        )
    }

    /**
     * Squiggly slider colors using button color scheme
     * 
     * @param buttonColor The active button color from player theme
     * @return SliderColors configuration for squiggly slider
     */
    @Composable
    fun wavySliderColors(buttonColor: Color): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = buttonColor,
            activeTickColor = buttonColor,
            thumbColor = Color.Transparent,
            inactiveTrackColor = Color.White.copy(alpha = Config.INACTIVE_TRACK_ALPHA),
            inactiveTickColor = Color.White.copy(alpha = Config.INACTIVE_TICK_ALPHA)
        )
    }

    @Composable
    fun thickSliderColors(buttonColor: Color): SliderColors {
        return getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.THICK_INACTIVE_TRACK_ALPHA
        )
    }

    @Composable
    fun circularSliderColors(buttonColor: Color): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = buttonColor,
            activeTickColor = buttonColor,
            thumbColor = buttonColor,
            inactiveTrackColor = Color.White.copy(alpha = Config.INACTIVE_TRACK_ALPHA)
        )
    }

    @Composable
    fun simpleSliderColors(buttonColor: Color): SliderColors {
        return SliderDefaults.colors(
            activeTrackColor = buttonColor.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            activeTickColor = buttonColor.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            thumbColor = Color.Transparent,
            inactiveTrackColor = Color.White.copy(alpha = Config.SIMPLE_INACTIVE_TRACK_ALPHA),
            inactiveTickColor = Color.White.copy(alpha = Config.SIMPLE_INACTIVE_TRACK_ALPHA)
        )
    }

    /**
     * Configuration constants for slider colors
     */
    object Config {
        /** Alpha transparency for inactive track - subtle white appearance */
        const val INACTIVE_TRACK_ALPHA = 0.15f

        const val THICK_INACTIVE_TRACK_ALPHA = 0.2f

        const val SIMPLE_ACTIVE_TRACK_ALPHA = 0.8f

        const val SIMPLE_INACTIVE_TRACK_ALPHA = 0.1f
        
        /** Alpha transparency for inactive ticks */
        const val INACTIVE_TICK_ALPHA = 0.2f
        
        /** Default active color sentinel — caller must supply a themed color from MaterialTheme */
        val DEFAULT_ACTIVE_COLOR = Color.Unspecified
        
        /** Default inactive color when no theme color is available */
        val DEFAULT_INACTIVE_COLOR = Color.White.copy(alpha = PlayerSliderColors.Config.INACTIVE_TRACK_ALPHA)
    }
}
