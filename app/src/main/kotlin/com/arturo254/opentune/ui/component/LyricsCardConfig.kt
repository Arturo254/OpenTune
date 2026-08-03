/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R

// ─────────────────────────────────────────────────────────────────────────────
// Layout styles — cada valor representa un diseño visual distinto para la tarjeta
// Agregar un nuevo estilo = añadir un entry aquí + un composable en LyricsCardLayouts
// ─────────────────────────────────────────────────────────────────────────────

enum class LyricsLayoutStyle(
    val displayName: String,
    val description: String,
) {
    GlassCard(
        displayName = "Glass Card",
        description = "Liquid glass panel",
    ),
    Minimal(
        displayName = "Minimal",
        description = "Clean and distraction-free",
    ),
    CoverFocused(
        displayName = "Cover Focus",
        description = "Featured album cover",
    ),
    Centered(
        displayName = "Centered",
        description = "Lyrics take center stage",
    ),
    BlurWash(
        displayName = "Blur Wash",
        description = "Ultra-blurred background",
    ),
    StreamingModern(
        displayName = "Streaming",
        description = "Modern music app style",
    ),
}

// ─────────────────────────────────────────────────────────────────────────────
// Tipo de fondo para layouts que aceptan variantes de fondo
// ─────────────────────────────────────────────────────────────────────────────

enum class LyricsBackgroundType(val displayName: String) {
    AlbumArt("Cover art"),
    SolidDark("Dark"),
    SolidLight("Light"),
    Gradient("Gradient"),
}

/** Localized label for [style], shown in the layout picker thumbnail. */
@Composable
fun LyricsLayoutStyle.localizedDisplayName(): String = when (this) {
    LyricsLayoutStyle.GlassCard -> stringResource(R.string.lyrics_card_style_glass_card)
    LyricsLayoutStyle.Minimal -> stringResource(R.string.lyrics_card_style_minimal)
    LyricsLayoutStyle.CoverFocused -> stringResource(R.string.lyrics_card_style_cover_focused)
    LyricsLayoutStyle.Centered -> stringResource(R.string.lyrics_card_style_centered)
    LyricsLayoutStyle.BlurWash -> stringResource(R.string.lyrics_card_style_blur_wash)
    LyricsLayoutStyle.StreamingModern -> stringResource(R.string.lyrics_card_style_streaming_modern)
}

// ─────────────────────────────────────────────────────────────────────────────
// LyricsCardConfig — estado inmutable del usuario.
// Se pasa a LyricsCardByLayout y a LyricsShareCarouselSheet.
// Modifica con .copy(...) para aplicar cambios sin mutación.
// ─────────────────────────────────────────────────────────────────────────────

data class LyricsCardConfig(

    /** Qué template visual se renderiza en la tarjeta */
    val layoutStyle: LyricsLayoutStyle = LyricsLayoutStyle.GlassCard,

    /** Estilo de vidrio/colores/blur; solo los layouts que usan cloudy/liquidGlass lo consumen */
    val glassStyle: LyricsGlassStyle = LyricsGlassStyle.FrostedDark,

    /**
     * Multiplicador sobre el tamaño de fuente calculado automáticamente.
     * Rango recomendado: 0.6f – 1.5f
     */
    val textSizeMultiplier: Float = 1f,

    /** Alineación del bloque de letra */
    val textAlign: TextAlign = TextAlign.Center,

    /** Visibilidad de elementos dentro de la tarjeta */
    val showTitle: Boolean = true,
    val showArtist: Boolean = true,
    val showCoverArt: Boolean = true,
    val showBranding: Boolean = true,

    /** Tipo de fondo (consumido por Minimal y StreamingModern) */
    val backgroundType: LyricsBackgroundType = LyricsBackgroundType.AlbumArt,

    /**
     * Padding interno de la tarjeta.
     * Rango recomendado: 12.dp – 36.dp
     */
    val cardPadding: Dp = 24.dp,
)
