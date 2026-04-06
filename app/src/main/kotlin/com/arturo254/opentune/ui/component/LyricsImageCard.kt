/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.*
import com.arturo254.opentune.models.MediaMetadata

// ─────────────────────────────────────────────────────────────────────────────
// rememberAdjustedFontSize
//
// Utilidad compartida — package-internal, accesible desde LyricsCardLayouts.
// Calcula el tamaño de fuente óptimo mediante búsqueda binaria para que
// el texto quepa dentro de maxWidth × maxHeight sin desbordarse.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberAdjustedFontSize(
    text: String,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
    initialFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 14.sp,
    style: TextStyle = TextStyle.Default,
    textMeasurer: androidx.compose.ui.text.TextMeasurer? = null,
): TextUnit {
    val measurer = textMeasurer ?: rememberTextMeasurer()

    var calculatedFontSize by remember(text, maxWidth, maxHeight, style, density) {
        val initialSize = when {
            text.length < 50  -> initialFontSize
            text.length < 100 -> (initialFontSize.value * 0.8f).sp
            text.length < 200 -> (initialFontSize.value * 0.6f).sp
            else              -> (initialFontSize.value * 0.5f).sp
        }
        mutableStateOf(initialSize)
    }

    LaunchedEffect(key1 = text, key2 = maxWidth, key3 = maxHeight) {
        val targetWidthPx  = with(density) { maxWidth.toPx()  * 0.92f }
        val targetHeightPx = with(density) { maxHeight.toPx() * 0.92f }

        if (text.isBlank()) {
            calculatedFontSize = minFontSize
            return@LaunchedEffect
        }

        // Intenta un tamaño mayor para textos muy cortos
        when {
            text.length < 20 -> {
                val largerSize = (initialFontSize.value * 1.1f).sp
                val result = measurer.measure(
                    text  = AnnotatedString(text),
                    style = style.copy(fontSize = largerSize),
                )
                if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                    calculatedFontSize = largerSize
                    return@LaunchedEffect
                }
            }
            text.length < 30 -> {
                val largerSize = (initialFontSize.value * 0.9f).sp
                val result = measurer.measure(
                    text  = AnnotatedString(text),
                    style = style.copy(fontSize = largerSize),
                )
                if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                    calculatedFontSize = largerSize
                    return@LaunchedEffect
                }
            }
        }

        // Búsqueda binaria
        var minSize  = minFontSize.value
        var maxSize  = initialFontSize.value
        var bestFit  = minSize
        var iterations = 0

        while (minSize <= maxSize && iterations < 20) {
            iterations++
            val midSize   = (minSize + maxSize) / 2
            val midSizeSp = midSize.sp
            val result    = measurer.measure(
                text  = AnnotatedString(text),
                style = style.copy(fontSize = midSizeSp),
            )
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                bestFit = midSize
                minSize = midSize + 0.5f
            } else {
                maxSize = midSize - 0.5f
            }
        }

        calculatedFontSize = if (bestFit < minFontSize.value) minFontSize else bestFit.sp
    }

    return calculatedFontSize
}

// ─────────────────────────────────────────────────────────────────────────────
// LyricsImageCard — thin wrapper (backward-compatible)
//
// Todos los callers existentes siguen funcionando sin cambios.
// Internamente delega a GlassCardLayout con un LyricsCardConfig derivado.
// Para usar el nuevo sistema con múltiples layouts, usa LyricsShareCarouselSheet
// o LyricsCardByLayout directamente.
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LyricsImageCard(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    glassStyle: LyricsGlassStyle = LyricsGlassStyle.FrostedDark,
    darkBackground: Boolean = true,       // mantenido por compatibilidad; actualmente no usado por GlassCardLayout
    backgroundColor: Color? = null,       // mantenido por compatibilidad
    textColor: Color? = null,
    secondaryTextColor: Color? = null,
) {
    // Si se proveen overrides de color, se aplican sobre una copia del estilo
    val effectiveStyle = remember(glassStyle, textColor, secondaryTextColor) {
        glassStyle.copy(
            textColor          = textColor          ?: glassStyle.textColor,
            secondaryTextColor = secondaryTextColor ?: glassStyle.secondaryTextColor,
        )
    }

    GlassCardLayout(
        lyricText     = lyricText,
        mediaMetadata = mediaMetadata,
        config        = LyricsCardConfig(glassStyle = effectiveStyle),
    )
}
