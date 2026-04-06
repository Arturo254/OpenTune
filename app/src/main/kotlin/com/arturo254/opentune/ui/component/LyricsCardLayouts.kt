/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.UseSystemFontKey
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.utils.rememberPreference

// ─────────────────────────────────────────────────────────────────────────────
// Constantes compartidas entre layouts
// ─────────────────────────────────────────────────────────────────────────────

internal val LyricsCardSize       = 340.dp
internal val LyricsCardCorner     = 24.dp

// ─────────────────────────────────────────────────────────────────────────────
// Helpers internos — reutilizados por todos los layouts
// ─────────────────────────────────────────────────────────────────────────────

/** Fuente de letras respetando la preferencia del usuario */
@Composable
internal fun rememberLyricsFontFamily(): FontFamily? {
    val (useSystemFont) = rememberPreference(UseSystemFontKey, defaultValue = false)
    return remember(useSystemFont) {
        if (useSystemFont) null else FontFamily(Font(R.font.sfprodisplaybold))
    }
}

/** Painter de la carátula con crossfade */
@Composable
internal fun rememberArtworkPainter(thumbnailUrl: String?): Painter =
    rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl)
            .crossfade(true)
            .build()
    )

/** Fila de marca OpenTune — aparece en el pie de todos los layouts si showBranding=true */
@Composable
internal fun LyricsBrandingRow(
    secondaryColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(secondaryColor.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.opentune),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                colorFilter = ColorFilter.tint(
                    if (isDark) Color.Black.copy(alpha = 0.85f)
                    else Color.White.copy(alpha = 0.9f)
                ),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text  = context.getString(R.string.app_name),
            color = secondaryColor,
            fontSize    = 13.sp,
            fontWeight  = FontWeight.SemiBold,
            letterSpacing = 0.02.em,
        )
    }
}

/**
 * Fila de metadatos (carátula + título + artista).
 * Respeta los flags showCoverArt / showTitle / showArtist del config.
 */
@Composable
internal fun LyricsMetadataRow(
    mediaMetadata: MediaMetadata,
    artworkPainter: Painter,
    config: LyricsCardConfig,
    mainTextColor: Color,
    secondaryColor: Color,
    coverArtSize: Dp = 56.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    ) {
        if (config.showCoverArt) {
            Image(
                painter      = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier     = Modifier
                    .size(coverArtSize)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            if (config.showTitle) {
                Text(
                    text       = mediaMetadata.title,
                    color      = mainTextColor,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.padding(bottom = 2.dp),
                    style      = TextStyle(letterSpacing = (-0.02).em),
                )
            }
            if (config.showArtist) {
                Text(
                    text       = mediaMetadata.artists.joinToString { it.name },
                    color      = secondaryColor,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dispatcher — punto de entrada único para cualquier caller
// Añadir un nuevo layout = agregar entry al when + crear el composable abajo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LyricsCardByLayout(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    when (config.layoutStyle) {
        LyricsLayoutStyle.GlassCard      -> GlassCardLayout(lyricText, mediaMetadata, config, modifier)
        LyricsLayoutStyle.Minimal        -> MinimalLayout(lyricText, mediaMetadata, config, modifier)
        LyricsLayoutStyle.CoverFocused   -> CoverFocusedLayout(lyricText, mediaMetadata, config, modifier)
        LyricsLayoutStyle.Centered       -> CenteredLayout(lyricText, mediaMetadata, config, modifier)
        LyricsLayoutStyle.BlurWash       -> BlurWashLayout(lyricText, mediaMetadata, config, modifier)
        LyricsLayoutStyle.StreamingModern -> StreamingModernLayout(lyricText, mediaMetadata, config, modifier)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout 1 — Glass Card
// Extrae la lógica del LyricsImageCard original. LyricsImageCard delega aquí.
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GlassCardLayout(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    val density       = LocalDensity.current
    val fontFamily    = rememberLyricsFontFamily()
    val glassStyle    = config.glassStyle
    val mainTextColor = glassStyle.textColor
    val secondaryColor = glassStyle.secondaryTextColor
    val artworkPainter = rememberArtworkPainter(mediaMetadata.thumbnailUrl)

    var glassComponentSize by remember { mutableStateOf(Size.Zero) }
    val lensCenter = remember(glassComponentSize) {
        Offset(glassComponentSize.width / 2f, glassComponentSize.height / 2f)
    }
    val lensSize = remember(glassComponentSize) {
        Size(glassComponentSize.width, glassComponentSize.height)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LyricsCardSize)
                .clip(RoundedCornerShape(LyricsCardCorner)),
            contentAlignment = Alignment.Center,
        ) {
            // Fondo desenfocado
            Image(
                painter      = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier     = Modifier.fillMaxSize().cloudy(radius = glassStyle.cloudyRadius),
            )
            // Gradiente oscurecedor
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = glassStyle.backgroundDimAlpha * 0.8f),
                            Color.Black.copy(alpha = glassStyle.backgroundDimAlpha),
                            Color.Black.copy(alpha = glassStyle.backgroundDimAlpha * 1.2f),
                        )
                    )
                )
            )

            // Panel de vidrio líquido
            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxSize()
                    .onSizeChanged { glassComponentSize = Size(it.width.toFloat(), it.height.toFloat()) }
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .cloudy(radius = glassStyle.cloudyRadius)
                        .then(
                            if (glassComponentSize.width > 0f && glassComponentSize.height > 0f) {
                                Modifier.liquidGlass(
                                    lensCenter  = lensCenter,
                                    lensSize    = lensSize,
                                    cornerRadius = glassStyle.glassCornerRadius,
                                    refraction  = glassStyle.refraction,
                                    curve       = glassStyle.curve,
                                    dispersion  = glassStyle.dispersion,
                                    saturation  = glassStyle.glassSaturation,
                                    contrast    = glassStyle.glassContrast,
                                    tint        = glassStyle.glassTint,
                                    edge        = glassStyle.glassEdge,
                                )
                            } else Modifier
                        )
                        .drawWithContent {
                            drawContent()
                            drawRect(glassStyle.surfaceTint.copy(alpha = glassStyle.surfaceAlpha))
                            drawRect(glassStyle.overlayColor.copy(alpha = glassStyle.overlayAlpha))
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(config.cardPadding),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LyricsMetadataRow(mediaMetadata, artworkPainter, config, mainTextColor, secondaryColor)

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val textStyle = TextStyle(
                            color         = mainTextColor,
                            fontWeight    = FontWeight.SemiBold,
                            textAlign     = config.textAlign,
                            letterSpacing = (-0.01).em,
                            fontFamily    = fontFamily,
                        )
                        val initialSize = when {
                            lyricText.length < 50  -> 22.sp
                            lyricText.length < 100 -> 19.sp
                            lyricText.length < 200 -> 16.sp
                            lyricText.length < 300 -> 14.sp
                            else                   -> 12.sp
                        }
                        val dynamicFontSize = rememberAdjustedFontSize(
                            text            = lyricText,
                            maxWidth        = maxWidth - 6.dp,
                            maxHeight       = maxHeight - 6.dp,
                            density         = density,
                            initialFontSize = (initialSize.value * config.textSizeMultiplier).sp,
                            minFontSize     = 11.sp,
                            style           = textStyle,
                            textMeasurer    = rememberTextMeasurer(),
                        )
                        Text(
                            text      = lyricText,
                            style     = textStyle.copy(
                                fontSize   = dynamicFontSize,
                                lineHeight = dynamicFontSize.value.sp * 1.35f,
                            ),
                            overflow  = TextOverflow.Ellipsis,
                            textAlign = config.textAlign,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }

                    if (config.showBranding) {
                        LyricsBrandingRow(secondaryColor, glassStyle.isDark)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout 2 — Minimal
// Fondo sólido, tipografía grande, cita decorativa, sin efectos de vidrio.
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MinimalLayout(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    val density    = LocalDensity.current
    val fontFamily = rememberLyricsFontFamily()
    val isDark     = config.glassStyle.isDark

    val bgColor      = if (isDark) Color(0xFF0F0F0F) else Color(0xFFF5F5F5)
    val mainText     = if (isDark) Color.White else Color(0xFF1A1A1A)
    val secondaryTxt = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF1A1A1A).copy(alpha = 0.5f)
    // Acento: usa el tint del estilo si no es negro/blanco puro
    val accent = config.glassStyle.surfaceTint.let {
        if (it == Color.Black || it == Color.White) Color(0xFF6366F1) else it.copy(alpha = 1f)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LyricsCardSize)
                .clip(RoundedCornerShape(LyricsCardCorner))
                .background(bgColor),
        ) {
            // Línea de acento superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0f))))
                    .align(Alignment.TopCenter),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(config.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Comilla decorativa
                Text(
                    text       = "\u201C",
                    color      = accent.copy(alpha = 0.25f),
                    fontSize   = 80.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 64.sp,
                )

                // Letra
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val textStyle = TextStyle(
                        color         = mainText,
                        fontWeight    = FontWeight.Bold,
                        textAlign     = config.textAlign,
                        fontFamily    = fontFamily,
                        letterSpacing = (-0.02).em,
                    )
                    val initialSize = when {
                        lyricText.length < 50  -> 26.sp
                        lyricText.length < 100 -> 22.sp
                        lyricText.length < 200 -> 18.sp
                        else                   -> 14.sp
                    }
                    val dynamicFontSize = rememberAdjustedFontSize(
                        text            = lyricText,
                        maxWidth        = maxWidth - 6.dp,
                        maxHeight       = maxHeight - 6.dp,
                        density         = density,
                        initialFontSize = (initialSize.value * config.textSizeMultiplier).sp,
                        minFontSize     = 11.sp,
                        style           = textStyle,
                        textMeasurer    = rememberTextMeasurer(),
                    )
                    Text(
                        text      = lyricText,
                        style     = textStyle.copy(
                            fontSize   = dynamicFontSize,
                            lineHeight = dynamicFontSize.value.sp * 1.4f,
                        ),
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = config.textAlign,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }

                // Pie: separador + metadatos + branding
                Column {
                    if (config.showTitle || config.showArtist) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(mainText.copy(alpha = 0.1f))
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (config.showTitle) {
                        Text(
                            text       = mediaMetadata.title,
                            color      = mainText,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                    if (config.showArtist) {
                        Text(
                            text       = mediaMetadata.artists.joinToString { it.name },
                            color      = secondaryTxt,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                    if (config.showBranding) {
                        Spacer(Modifier.height(8.dp))
                        LyricsBrandingRow(secondaryTxt, isDark)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout 3 — Cover Focused
// La carátula ocupa la parte superior; la letra en la parte inferior sobre blur.
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CoverFocusedLayout(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    val density        = LocalDensity.current
    val fontFamily     = rememberLyricsFontFamily()
    val glassStyle     = config.glassStyle
    val mainTextColor  = glassStyle.textColor
    val secondaryColor = glassStyle.secondaryTextColor
    val artworkPainter = rememberArtworkPainter(mediaMetadata.thumbnailUrl)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LyricsCardSize)
                .clip(RoundedCornerShape(LyricsCardCorner)),
        ) {
            // Fondo desenfocado general
            Image(
                painter      = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier     = Modifier.fillMaxSize().cloudy(radius = 22),
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Carátula grande
                if (config.showCoverArt) {
                    Image(
                        painter      = artworkPainter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier     = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (config.showTitle) {
                    Text(
                        text       = mediaMetadata.title,
                        color      = mainTextColor,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        textAlign  = TextAlign.Center,
                        style      = TextStyle(letterSpacing = (-0.02).em),
                    )
                }
                if (config.showArtist) {
                    Text(
                        text       = mediaMetadata.artists.joinToString { it.name },
                        color      = secondaryColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        textAlign  = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.15f)))
                Spacer(Modifier.height(10.dp))

                // Letra
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val textStyle = TextStyle(
                        color         = mainTextColor,
                        fontWeight    = FontWeight.SemiBold,
                        textAlign     = config.textAlign,
                        fontFamily    = fontFamily,
                        letterSpacing = (-0.01).em,
                    )
                    val initialSize = when {
                        lyricText.length < 50  -> 20.sp
                        lyricText.length < 100 -> 17.sp
                        lyricText.length < 200 -> 14.sp
                        else                   -> 12.sp
                    }
                    val dynamicFontSize = rememberAdjustedFontSize(
                        text            = lyricText,
                        maxWidth        = maxWidth - 6.dp,
                        maxHeight       = maxHeight - 6.dp,
                        density         = density,
                        initialFontSize = (initialSize.value * config.textSizeMultiplier).sp,
                        minFontSize     = 10.sp,
                        style           = textStyle,
                        textMeasurer    = rememberTextMeasurer(),
                    )
                    Text(
                        text      = lyricText,
                        style     = textStyle.copy(
                            fontSize   = dynamicFontSize,
                            lineHeight = dynamicFontSize.value.sp * 1.4f,
                        ),
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = config.textAlign,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }

                if (config.showBranding) {
                    Spacer(Modifier.height(8.dp))
                    LyricsBrandingRow(secondaryColor, glassStyle.isDark, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout 4 — Centered
// Letra protagonista en el centro exacto; metadatos arriba y branding abajo.
// Sin panel de vidrio — solo scrim sobre la imagen desenfocada.
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CenteredLayout(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    val density        = LocalDensity.current
    val fontFamily     = rememberLyricsFontFamily()
    val artworkPainter = rememberArtworkPainter(mediaMetadata.thumbnailUrl)
    val mainTextColor  = config.glassStyle.textColor
    val secondaryColor = config.glassStyle.secondaryTextColor

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LyricsCardSize)
                .clip(RoundedCornerShape(LyricsCardCorner)),
        ) {
            // Imagen con blur
            Image(
                painter      = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier     = Modifier.fillMaxSize().cloudy(radius = config.glassStyle.cloudyRadius),
            )
            // Scrim vertical con más opacidad en bordes
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.62f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.72f),
                        )
                    )
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(config.cardPadding),
                verticalArrangement    = Arrangement.SpaceBetween,
                horizontalAlignment    = Alignment.CenterHorizontally,
            ) {
                // Metadatos arriba (centrados)
                if (config.showTitle || config.showArtist) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (config.showTitle) {
                            Text(
                                text          = mediaMetadata.title,
                                color         = mainTextColor,
                                fontSize      = 15.sp,
                                fontWeight    = FontWeight.Bold,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis,
                                textAlign     = TextAlign.Center,
                                letterSpacing = 0.01.em,
                            )
                        }
                        if (config.showArtist) {
                            Text(
                                text          = mediaMetadata.artists.joinToString { it.name },
                                color         = secondaryColor,
                                fontSize      = 12.sp,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis,
                                textAlign     = TextAlign.Center,
                                letterSpacing = 0.04.em,
                            )
                        }
                    }
                }

                // Letra centrada — toma todo el espacio disponible
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val textStyle = TextStyle(
                        color         = mainTextColor,
                        fontWeight    = FontWeight.Bold,
                        textAlign     = TextAlign.Center,
                        fontFamily    = fontFamily,
                        letterSpacing = (-0.02).em,
                    )
                    val initialSize = when {
                        lyricText.length < 40  -> 28.sp
                        lyricText.length < 80  -> 24.sp
                        lyricText.length < 150 -> 20.sp
                        lyricText.length < 250 -> 16.sp
                        else                   -> 13.sp
                    }
                    val dynamicFontSize = rememberAdjustedFontSize(
                        text            = lyricText,
                        maxWidth        = maxWidth - 6.dp,
                        maxHeight       = maxHeight - 6.dp,
                        density         = density,
                        initialFontSize = (initialSize.value * config.textSizeMultiplier).sp,
                        minFontSize     = 11.sp,
                        style           = textStyle,
                        textMeasurer    = rememberTextMeasurer(),
                    )
                    Text(
                        text      = lyricText,
                        style     = textStyle.copy(
                            fontSize   = dynamicFontSize,
                            lineHeight = dynamicFontSize.value.sp * 1.4f,
                        ),
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }

                if (config.showBranding) {
                    LyricsBrandingRow(secondaryColor, config.glassStyle.isDark, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout 5 — Blur Wash
// Blur ultra intenso de fondo; letra sobre un panel frosted contenido.
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BlurWashLayout(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    val density        = LocalDensity.current
    val fontFamily     = rememberLyricsFontFamily()
    val glassStyle     = config.glassStyle
    val mainTextColor  = glassStyle.textColor
    val secondaryColor = glassStyle.secondaryTextColor
    val artworkPainter = rememberArtworkPainter(mediaMetadata.thumbnailUrl)

    // Estado del tamaño para liquidGlass del panel interior
    var panelSize by remember { mutableStateOf(Size.Zero) }
    val lensCenter = remember(panelSize) { Offset(panelSize.width / 2f, panelSize.height / 2f) }
    val lensSize   = remember(panelSize) { Size(panelSize.width, panelSize.height) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LyricsCardSize)
                .clip(RoundedCornerShape(LyricsCardCorner)),
        ) {
            // Fondo con blur máximo
            Image(
                painter      = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier     = Modifier.fillMaxSize().cloudy(radius = 30),
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(config.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Header compacto
                LyricsMetadataRow(mediaMetadata, artworkPainter, config, mainTextColor, secondaryColor, 46.dp)

                // Letra dentro de panel frosted
                val textMeasurer = rememberTextMeasurer()

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val constraintsScope = this

                    val availableWidth = constraintsScope.maxWidth - 32.dp
                    val availableHeight = constraintsScope.maxHeight - 32.dp

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged {
                                panelSize = Size(
                                    it.width.toFloat(),
                                    it.height.toFloat()
                                )
                            }
                            .clip(RoundedCornerShape(18.dp)),
                    ) {

                        // Superficie frosted
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .cloudy(radius = 14)
                                .then(
                                    if (panelSize.width > 0f && panelSize.height > 0f) {
                                        Modifier.liquidGlass(
                                            lensCenter = lensCenter,
                                            lensSize = lensSize,
                                            cornerRadius = 40f,
                                            refraction = 0.18f,
                                            curve = 0.18f,
                                            dispersion = 0.0f,
                                            saturation = 1.08f,
                                            contrast = 1.0f,
                                            tint = Color.White.copy(alpha = 0.08f),
                                            edge = 0.18f,
                                        )
                                    } else Modifier
                                )
                                .drawWithContent {
                                    drawContent()
                                    drawRect(Color.White.copy(alpha = 0.07f))
                                }
                        )

                        val textStyle = TextStyle(
                            color = mainTextColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = config.textAlign,
                            fontFamily = fontFamily,
                            letterSpacing = (-0.01).em,
                        )

                        val initialSize = when {
                            lyricText.length < 50 -> 21.sp
                            lyricText.length < 100 -> 18.sp
                            lyricText.length < 200 -> 15.sp
                            else -> 12.sp
                        }

                        // ✅ ahora SIN error de receiver
                        val dynamicFontSize = rememberAdjustedFontSize(
                            text = lyricText,
                            maxWidth = availableWidth,
                            maxHeight = availableHeight,
                            density = density,
                            initialFontSize =
                                (initialSize.value * config.textSizeMultiplier).sp,
                            minFontSize = 11.sp,
                            style = textStyle,
                            textMeasurer = textMeasurer,
                        )

                        Text(
                            text = lyricText,
                            style = textStyle.copy(
                                fontSize = dynamicFontSize,
                                lineHeight = dynamicFontSize.value.sp * 1.35f,
                            ),
                            overflow = TextOverflow.Ellipsis,
                            textAlign = config.textAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                }

                if (config.showBranding) {
                    Spacer(Modifier.height(4.dp))
                    LyricsBrandingRow(secondaryColor, glassStyle.isDark)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout 6 — Streaming Modern
// Fondo oscuro con gradiente, acento de color, comillas, estilo Spotify/Apple.
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun StreamingModernLayout(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    config: LyricsCardConfig,
    modifier: Modifier = Modifier,
) {
    val density        = LocalDensity.current
    val fontFamily     = rememberLyricsFontFamily()
    val artworkPainter = rememberArtworkPainter(mediaMetadata.thumbnailUrl)

    // Acento: usa surfaceTint del glassStyle, fallback índigo si es negro/blanco
    val accent = config.glassStyle.surfaceTint.let {
        if (it == Color.Black || it == Color.White) Color(0xFF6366F1) else it.copy(alpha = 1f)
    }
    val mainTextColor  = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.55f)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(LyricsCardSize)
                .clip(RoundedCornerShape(LyricsCardCorner))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF0D0D1A), Color(0xFF191928)))
                ),
        ) {
            // Resplandor de acento en esquina superior derecha
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .offset(x = 120.dp, y = (-50).dp)
                    .background(
                        Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent))
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(config.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Header: carátula pequeña + título + artista + ícono play
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (config.showCoverArt) {
                        Image(
                            painter      = artworkPainter,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier     = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (config.showTitle) {
                            Text(
                                text          = mediaMetadata.title,
                                color         = mainTextColor,
                                fontSize      = 15.sp,
                                fontWeight    = FontWeight.Bold,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis,
                                letterSpacing = (-0.02).em,
                            )
                        }
                        if (config.showArtist) {
                            Text(
                                text          = mediaMetadata.artists.joinToString { it.name },
                                color         = secondaryColor,
                                fontSize      = 12.sp,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis,
                                letterSpacing = 0.01.em,
                            )
                        }
                    }
                    // Botón play decorativo
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.18f))
                            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\u25B6", color = accent, fontSize = 9.sp)
                    }
                }

                // Separador con acento
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), Color.Transparent))
                        )
                )

                // Bloque de letra con comilla de apertura
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val textStyle = TextStyle(
                        color         = mainTextColor,
                        fontWeight    = FontWeight.Bold,
                        textAlign     = config.textAlign,
                        fontFamily    = fontFamily,
                        letterSpacing = (-0.02).em,
                    )
                    val initialSize = when {
                        lyricText.length < 50  -> 23.sp
                        lyricText.length < 100 -> 19.sp
                        lyricText.length < 200 -> 16.sp
                        else                   -> 13.sp
                    }
                    val dynamicFontSize = rememberAdjustedFontSize(
                        text            = lyricText,
                        maxWidth        = maxWidth - 6.dp,
                        maxHeight       = maxHeight - 36.dp,  // reserva espacio para la comilla
                        density         = density,
                        initialFontSize = (initialSize.value * config.textSizeMultiplier).sp,
                        minFontSize     = 11.sp,
                        style           = textStyle,
                        textMeasurer    = rememberTextMeasurer(),
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text       = "\u201C",
                            color      = accent.copy(alpha = 0.55f),
                            fontSize   = 38.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 30.sp,
                        )
                        Text(
                            text      = lyricText,
                            style     = textStyle.copy(
                                fontSize   = dynamicFontSize,
                                lineHeight = dynamicFontSize.value.sp * 1.35f,
                            ),
                            overflow  = TextOverflow.Ellipsis,
                            textAlign = config.textAlign,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (config.showBranding) {
                    LyricsBrandingRow(secondaryColor, isDark = true)
                }
            }
        }
    }
}
