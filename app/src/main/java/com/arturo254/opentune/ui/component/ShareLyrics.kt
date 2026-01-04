package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arturo254.opentune.R
import com.arturo254.opentune.models.MediaMetadata
import kotlinx.coroutines.launch
import kotlin.math.min
import androidx.compose.ui.res.stringResource

// Enums
enum class FontStyle {
    REGULAR, BOLD, EXTRA_BOLD
}

enum class LogoPosition {
    BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT, NONE
}

enum class BackgroundStyle {
    SOLID, GRADIENT, PATTERN
}

enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

enum class LogoSize {
    SMALL, MEDIUM, LARGE
}

enum class CoverArtStyle {
    ROUNDED, CIRCLE, SQUARE
}

enum class LyricsStyle {
    NORMAL, ITALIC, CONDENSED
}

// Data classes
data class ImageCustomization(
    val backgroundColor: Color = Color(0xFF1A1A1A),
    val textColor: Color = Color.White,
    val secondaryTextColor: Color = Color.White.copy(alpha = 0.7f),
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SOLID,
    val gradientColors: List<Color>? = null,
    val fontStyle: FontStyle = FontStyle.EXTRA_BOLD,
    val showCoverArt: Boolean = true,
    val showSongTitle: Boolean = true,
    val showArtistName: Boolean = true,
    val showLogo: Boolean = true,
    val logoPosition: LogoPosition = LogoPosition.BOTTOM_RIGHT,
    val logoSize: LogoSize = LogoSize.MEDIUM,
    val patternOpacity: Float = 0.05f,
    val cornerRadius: Float = 16f,
    val isDark: Boolean = true,
    val textAlignment: TextAlignment = TextAlignment.CENTER,
    val padding: Float = 24f,
    val textShadowEnabled: Boolean = true,
    val borderEnabled: Boolean = false,
    val borderColor: Color = Color.White.copy(alpha = 0.3f),
    val borderWidth: Float = 2f,
    val coverArtStyle: CoverArtStyle = CoverArtStyle.ROUNDED,
    val lyricsStyle: LyricsStyle = LyricsStyle.NORMAL,
    val accentColor: Color? = null,
    val showAccentLine: Boolean = false,
    val spacingBetweenElements: Float = 16f,
    val lyricsLineSpacing: Float = 1.3f
)

data class ColorPreset(
    val name: String,
    val customization: ImageCustomization
)

// Presets unificados
val colorPresets = listOf(
    ColorPreset(
        "Dark",
        ImageCustomization(
            backgroundColor = Color(0xFF1A1A1A),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.7f),
            isDark = true
        )
    ),
    ColorPreset(
        "Light",
        ImageCustomization(
            backgroundColor = Color(0xFFF5F5F5),
            textColor = Color.Black,
            secondaryTextColor = Color.Black.copy(alpha = 0.7f),
            isDark = false
        )
    ),
    ColorPreset(
        "Blue",
        ImageCustomization(
            backgroundColor = Color(0xFF1E3A8A),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            isDark = true
        )
    ),
    ColorPreset(
        "Purple",
        ImageCustomization(
            backgroundColor = Color(0xFF4C1D95),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            isDark = true
        )
    ),
    ColorPreset(
        "Red",
        ImageCustomization(
            backgroundColor = Color(0xFF991B1B),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            isDark = true
        )
    ),
    ColorPreset(
        "Green",
        ImageCustomization(
            backgroundColor = Color(0xFF065F46),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            isDark = true
        )
    ),
    ColorPreset(
        "Gradient Blue",
        ImageCustomization(
            backgroundStyle = BackgroundStyle.GRADIENT,
            gradientColors = listOf(
                Color(0xFF1E3A8A),
                Color(0xFF3B82F6),
                Color(0xFF60A5FA)
            ),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.9f),
            isDark = true
        )
    ),
    ColorPreset(
        "Gradient Purple",
        ImageCustomization(
            backgroundStyle = BackgroundStyle.GRADIENT,
            gradientColors = listOf(
                Color(0xFF4C1D95),
                Color(0xFF7C3AED),
                Color(0xFFA78BFA)
            ),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.9f),
            isDark = true
        )
    ),
    ColorPreset(
        "Gradient Sunset",
        ImageCustomization(
            backgroundStyle = BackgroundStyle.GRADIENT,
            gradientColors = listOf(
                Color(0xFFF59E0B),
                Color(0xFFEF4444),
                Color(0xFF8B5CF6)
            ),
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.9f),
            isDark = true
        )
    )
)


@Composable
fun rememberAdjustedFontSize(
    text: String,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
    initialFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 14.sp,
    style: TextStyle = TextStyle.Default,
    textMeasurer: androidx.compose.ui.text.TextMeasurer? = null
): TextUnit {
    val measurer = textMeasurer ?: rememberTextMeasurer()

    var calculatedFontSize by remember(text, maxWidth, maxHeight, style, density) {
        val initialSize = when {
            text.length < 30 -> (initialFontSize.value * 1.1f).sp
            text.length < 60 -> initialFontSize
            text.length < 120 -> (initialFontSize.value * 0.85f).sp
            text.length < 200 -> (initialFontSize.value * 0.7f).sp
            else -> (initialFontSize.value * 0.6f).sp
        }
        mutableStateOf(initialSize)
    }

    LaunchedEffect(key1 = text, key2 = maxWidth, key3 = maxHeight) {
        val targetWidthPx = with(density) { maxWidth.toPx() * 0.85f }
        val targetHeightPx = with(density) { maxHeight.toPx() * 0.8f }

        if (text.isBlank()) {
            calculatedFontSize = minFontSize
            return@LaunchedEffect
        }

        var minSize = minFontSize.value
        var maxSize = (initialFontSize.value * 1.2f)
        var bestFit = minSize
        var iterations = 0

        while (minSize <= maxSize && iterations < 20) {
            iterations++
            val midSize = (minSize + maxSize) / 2
            val midSizeSp = midSize.sp

            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(
                    fontSize = midSizeSp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = (midSize * 1.3f).sp,
                    letterSpacing = 0.3.sp
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsImageCard(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    selectedCustomization: ImageCustomization = ImageCustomization(),
    onCustomizationChange: (ImageCustomization) -> Unit = {},
    onSaveImage: () -> Unit = {},
    showControls: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isGenerating by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showControls) {
            ModernControlsSection(
                selectedCustomization = selectedCustomization,
                isGenerating = isGenerating,
                onSaveImage = {
                    isGenerating = true
                    onSaveImage()
                    kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(1500)
                        isGenerating = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LyricsImageCardPreview(
            lyricText = lyricText,
            mediaMetadata = mediaMetadata,
            customization = selectedCustomization,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Image will be saved in high resolution",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernControlsSection(
    selectedCustomization: ImageCustomization,
    isGenerating: Boolean,
    onSaveImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Share Lyrics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FloatingActionButton(
                onClick = onSaveImage,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.image),
                        contentDescription = "Save",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LyricsImageCardPreview(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    customization: ImageCustomization = ImageCustomization(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp.dp
    val cardSize = remember(screenWidth) {
        min(screenWidth.value - 40f, 400f).dp
    }

    val outerPadding = cardSize * (customization.padding / 380f)
    val thumbnailSize = cardSize * 0.21f

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context)
            .data(mediaMetadata.thumbnailUrl)
            .crossfade(true)
            .placeholder(R.drawable.music_note)
            .error(R.drawable.music_note)
            .build()
    )

    val logoSizeMultiplier = when (customization.logoSize) {
        LogoSize.SMALL -> 0.7f
        LogoSize.MEDIUM -> 1f
        LogoSize.LARGE -> 1.3f
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .size(cardSize)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(customization.cornerRadius.dp),
                    ambientColor = customization.textColor.copy(alpha = 0.3f),
                    spotColor = customization.textColor.copy(alpha = 0.5f)
                )
                .then(
                    if (customization.borderEnabled) {
                        Modifier.border(
                            width = customization.borderWidth.dp,
                            color = customization.borderColor,
                            shape = RoundedCornerShape(customization.cornerRadius.dp)
                        )
                    } else Modifier
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(customization.cornerRadius.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = when (customization.backgroundStyle) {
                            BackgroundStyle.SOLID -> Brush.linearGradient(
                                listOf(
                                    customization.backgroundColor,
                                    customization.backgroundColor
                                )
                            )
                            BackgroundStyle.GRADIENT -> {
                                val colors = customization.gradientColors ?: listOf(
                                    customization.backgroundColor,
                                    customization.backgroundColor
                                )
                                Brush.linearGradient(
                                    colors = colors,
                                    start = Offset(0f, 0f),
                                    end = Offset(
                                        Float.POSITIVE_INFINITY,
                                        Float.POSITIVE_INFINITY
                                    )
                                )
                            }
                            BackgroundStyle.PATTERN -> Brush.linearGradient(
                                listOf(
                                    customization.backgroundColor,
                                    customization.backgroundColor
                                )
                            )
                        }
                    )
            ) {
                if (customization.backgroundStyle == BackgroundStyle.PATTERN) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pattern = 40.dp.toPx()
                        for (x in 0..size.width.toInt() step pattern.toInt()) {
                            for (y in 0..size.height.toInt() step pattern.toInt()) {
                                drawCircle(
                                    color = customization.textColor.copy(alpha = customization.patternOpacity),
                                    radius = 2.dp.toPx(),
                                    center = Offset(x.toFloat(), y.toFloat())
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(outerPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (customization.showCoverArt) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(customization.spacingBetweenElements.dp)
                        ) {
                            val coverShape = when (customization.coverArtStyle) {
                                CoverArtStyle.CIRCLE -> CircleShape
                                CoverArtStyle.SQUARE -> RoundedCornerShape(4.dp)
                                CoverArtStyle.ROUNDED -> RoundedCornerShape(20.dp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(thumbnailSize)
                                    .clip(coverShape)
                                    .background(customization.textColor.copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        customization.textColor.copy(alpha = 0.2f),
                                        coverShape
                                    )
                            ) {
                                Image(
                                    painter = painter,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(coverShape)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                if (customization.showSongTitle) {
                                    Text(
                                        text = mediaMetadata.title,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 20.sp,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        fontWeight = when (customization.fontStyle) {
                                            FontStyle.REGULAR -> FontWeight.Bold
                                            FontStyle.BOLD -> FontWeight.ExtraBold
                                            FontStyle.EXTRA_BOLD -> FontWeight.Black
                                        },
                                        color = customization.textColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 24.sp
                                    )
                                }
                                if (customization.showArtistName) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mediaMetadata.artists.joinToString { it.name },
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            letterSpacing = 0.2.sp
                                        ),
                                        color = customization.secondaryTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (customization.showAccentLine && customization.accentColor != null) {
                            Spacer(modifier = Modifier.height(customization.spacingBetweenElements.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(customization.accentColor, RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = customization.spacingBetweenElements.dp),
                        contentAlignment = when (customization.textAlignment) {
                            TextAlignment.LEFT -> Alignment.CenterStart
                            TextAlignment.CENTER -> Alignment.Center
                            TextAlignment.RIGHT -> Alignment.CenterEnd
                        }
                    ) {
                        val textMeasurer = rememberTextMeasurer()
                        val optimalFontSize = rememberAdjustedFontSize(
                            lyricText, maxWidth, maxHeight, density,
                            textMeasurer = textMeasurer
                        )

                        val lyricsWeight = when (customization.lyricsStyle) {
                            LyricsStyle.NORMAL -> when (customization.fontStyle) {
                                FontStyle.REGULAR -> FontWeight.Bold
                                FontStyle.BOLD -> FontWeight.ExtraBold
                                FontStyle.EXTRA_BOLD -> FontWeight.Black
                            }
                            LyricsStyle.ITALIC -> FontWeight.Bold
                            LyricsStyle.CONDENSED -> when (customization.fontStyle) {
                                FontStyle.REGULAR -> FontWeight.SemiBold
                                FontStyle.BOLD -> FontWeight.Bold
                                FontStyle.EXTRA_BOLD -> FontWeight.ExtraBold
                            }
                        }

                        Text(
                            text = lyricText,
                            textAlign = when (customization.textAlignment) {
                                TextAlignment.LEFT -> TextAlign.Left
                                TextAlignment.CENTER -> TextAlign.Center
                                TextAlignment.RIGHT -> TextAlign.Right
                            },
                            fontSize = optimalFontSize,
                            fontWeight = lyricsWeight,
                            color = customization.textColor,
                            lineHeight = optimalFontSize * customization.lyricsLineSpacing,
                            modifier = Modifier.fillMaxWidth(),
                            letterSpacing = if (customization.lyricsStyle == LyricsStyle.CONDENSED) (-0.5).sp else 0.3.sp,
                            style = if (customization.textShadowEnabled) {
                                TextStyle(
                                    shadow = Shadow(
                                        color = customization.backgroundColor.copy(alpha = 0.5f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    ),
                                    fontStyle = if (customization.lyricsStyle == LyricsStyle.ITALIC)
                                        androidx.compose.ui.text.font.FontStyle.Italic
                                    else
                                        androidx.compose.ui.text.font.FontStyle.Normal
                                )
                            } else TextStyle(
                                fontStyle = if (customization.lyricsStyle == LyricsStyle.ITALIC)
                                    androidx.compose.ui.text.font.FontStyle.Italic
                                else
                                    androidx.compose.ui.text.font.FontStyle.Normal
                            )
                        )
                    }

                    if (customization.showLogo && customization.logoPosition != LogoPosition.NONE) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = when (customization.logoPosition) {
                                LogoPosition.BOTTOM_LEFT, LogoPosition.TOP_LEFT -> Alignment.CenterStart
                                LogoPosition.BOTTOM_RIGHT, LogoPosition.TOP_RIGHT -> Alignment.CenterEnd
                                LogoPosition.NONE -> Alignment.CenterStart
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.opentune),
                                    contentDescription = null,
                                    modifier = Modifier.size((28 * logoSizeMultiplier).dp)
                                )

                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontSize = (15 * logoSizeMultiplier).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = customization.textColor,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPresetSelector(
    selectedPreset: ColorPreset,
    onPresetChange: (ColorPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(colorPresets) { preset ->
            ColorPresetItem(
                preset = preset,
                isSelected = preset == selectedPreset,
                onClick = { onPresetChange(preset) }
            )
        }
    }
}

@Composable
private fun ColorPresetItem(
    preset: ColorPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = if (preset.customization.backgroundStyle == BackgroundStyle.GRADIENT
                        && preset.customization.gradientColors != null) {
                        Brush.linearGradient(preset.customization.gradientColors)
                    } else {
                        Brush.linearGradient(
                            listOf(
                                preset.customization.backgroundColor,
                                preset.customization.backgroundColor
                            )
                        )
                    }
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                        alpha = 0.3f
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aa",
                color = preset.customization.textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (isSelected) 1f else 0.7f
            ),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}