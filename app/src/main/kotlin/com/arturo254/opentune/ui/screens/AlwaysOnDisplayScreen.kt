@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AodArtShape
import com.arturo254.opentune.constants.AodArtShapeKey
import com.arturo254.opentune.constants.AodArtSizeKey
import com.arturo254.opentune.constants.AodDarknessKey
import com.arturo254.opentune.constants.AodFullscreenKey
import com.arturo254.opentune.constants.AodShowArtistKey
import com.arturo254.opentune.constants.AodShowControlsKey
import com.arturo254.opentune.constants.AodShowProgressKey
import com.arturo254.opentune.constants.AodShowTimeKey
import com.arturo254.opentune.constants.AodShowTitleKey
import com.arturo254.opentune.constants.AodStyle
import com.arturo254.opentune.constants.AodStyleKey
import com.arturo254.opentune.constants.AodSpotlightIntensityKey
import com.arturo254.opentune.constants.AodSpotlightPulseKey
import com.arturo254.opentune.constants.AodTransitionDurationKey
import com.arturo254.opentune.constants.AodControlStyleKey
import com.arturo254.opentune.constants.AodControlStyle
import com.arturo254.opentune.constants.AodTextScaleKey
import com.arturo254.opentune.constants.AodClockFormatKey
import com.arturo254.opentune.constants.AodShowClockKey
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.ui.screens.settings.toShape
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberPreference
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.saket.squiggles.SquigglySlider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ═══════════════════════════════════════════════════════════════════════════
//  Utilidad: extrae el color dominante de la portada vía Palette API
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun rememberDominantColor(thumbnailUrl: String?): Color {
    val context = LocalContext.current
    var dominantColor by remember(thumbnailUrl) { mutableStateOf(Color.White) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl == null) {
            dominantColor = Color.White
            return@LaunchedEffect
        }
        runCatching {
            val req = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .allowHardware(false)   // Palette necesita bitmap de software
                .size(128, 128)         // Tamaño pequeño es suficiente y más rápido
                .build()
            val result = context.imageLoader.execute(req)
            if (result is SuccessResult) {
                val bmp: Bitmap = (result.image as? coil3.BitmapImage)?.bitmap
                    ?: return@LaunchedEffect
                Palette.from(bmp).generate { palette ->
                    val swatch = palette?.vibrantSwatch
                        ?: palette?.dominantSwatch
                        ?: palette?.lightVibrantSwatch
                    swatch?.rgb?.let { rgb ->
                        dominantColor = Color(rgb)
                    }
                }
            }
        }
    }
    return dominantColor
}

// ═══════════════════════════════════════════════════════════════════════════
//  Estado compartido entre transiciones — clave de sincronización
// ═══════════════════════════════════════════════════════════════════════════

private data class AodMediaState(
    val id: String?,
    val thumbnailUrl: String?,
    val title: String,
    val artist: String,
)

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AlwaysOnDisplayScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current ?: run {
        LaunchedEffect(Unit) { navController.navigateUp() }
        return
    }

    val view = LocalView.current
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    val (fullscreenMode) = rememberPreference(AodFullscreenKey, true)

    DisposableEffect(Unit, fullscreenMode) {
        view.keepScreenOn = true

        if (fullscreenMode && window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowInsetsControllerCompat(window, view)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            view.keepScreenOn = false
            if (window != null) {
                val insetsController = WindowInsetsControllerCompat(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    // ── Preferencias ────────────────────────────────────────────────────
    val (rawStyle) = rememberPreference(AodStyleKey, AodStyle.CLASSIC.name)
    val (rawShape) = rememberPreference(AodArtShapeKey, AodArtShape.ROUNDED.name)
    val (darkness) = rememberPreference(AodDarknessKey, 0.55f)
    val (artSizeFrac) = rememberPreference(AodArtSizeKey, 0.65f)
    val (showTitle) = rememberPreference(AodShowTitleKey, true)
    val (showArtist) = rememberPreference(AodShowArtistKey, true)
    val (showTime) = rememberPreference(AodShowTimeKey, true)
    val (showProgress) = rememberPreference(AodShowProgressKey, true)
    val (showControls) = rememberPreference(AodShowControlsKey, true)
    val (spotlightIntensity) = rememberPreference(AodSpotlightIntensityKey, 0.75f)
    val (spotlightPulse) = rememberPreference(AodSpotlightPulseKey, true)
    val (transitionDuration) = rememberPreference(AodTransitionDurationKey, 700)
    val (rawControlStyle) = rememberPreference(AodControlStyleKey, AodControlStyle.ROUNDED.name)
    val (textScale) = rememberPreference(AodTextScaleKey, 1.0f)
    val (showClock) = rememberPreference(AodShowClockKey, false)
    val (clockFormat24h) = rememberPreference(AodClockFormatKey, true)

    val aodStyle =
        remember(rawStyle) { runCatching { AodStyle.valueOf(rawStyle) }.getOrDefault(AodStyle.CLASSIC) }
    val artShape =
        remember(rawShape) { runCatching { AodArtShape.valueOf(rawShape) }.getOrDefault(AodArtShape.ROUNDED) }
    val controlStyle = remember(rawControlStyle) {
        runCatching { AodControlStyle.valueOf(rawControlStyle) }.getOrDefault(AodControlStyle.ROUNDED)
    }

    // ── Player state ─────────────────────────────────────────────────────
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipPrev by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var position by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.duration) }
    var sliderPosition by remember(mediaMetadata?.id) { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaMetadata?.id, playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(200L)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                sliderPosition?.let {
                    if (abs(playerConnection.player.currentPosition - it) <= 1_500L) sliderPosition =
                        null
                }
            }
        }
    }

    val displayPosition = sliderPosition ?: position
    val progressFraction = remember(displayPosition, duration) {
        if (duration > 0L && duration != C.TIME_UNSET)
            (displayPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }
    val artistText = remember(mediaMetadata?.artists) {
        mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty()
    }

    // ── Estado sincronizado para transiciones ────────────────────────────
    val mediaState = remember(mediaMetadata?.id) {
        AodMediaState(
            id = mediaMetadata?.id,
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
            title = mediaMetadata?.title.orEmpty(),
            artist = artistText,
        )
    }

    // ── Color dominante (se comparte entre layouts) ───────────────────────
    val dominantColor = rememberDominantColor(mediaState.thumbnailUrl)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp.dp
    val artSizeDp: Dp = remember(artSizeFrac, screenWidthDp, isLandscape) {
        val base = if (isLandscape) screenWidthDp * 0.35f else screenWidthDp * artSizeFrac
        base.coerceIn(100.dp, screenWidthDp)
    }

    val onSeekChange: (Float) -> Unit = { f ->
        if (duration > 0L && duration != C.TIME_UNSET)
            sliderPosition = (f * duration).toLong().coerceIn(0L, duration)
    }
    val onSeekFinished: () -> Unit = {
        sliderPosition?.let { playerConnection.player.seekTo(it); position = it }
        sliderPosition = null
    }

    val contentModifier = if (fullscreenMode) Modifier.fillMaxSize()
    else Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()

    // ── Reloj del sistema (opcional) ─────────────────────────────────────
    var clockText by remember { mutableStateOf("") }
    val clockPattern = if (clockFormat24h) "HH:mm" else "hh:mm a"
    LaunchedEffect(showClock) {
        if (showClock) {
            while (isActive) {
                clockText = SimpleDateFormat(clockPattern, Locale.getDefault()).format(Date())
                delay(10_000L)
            }
        }
    }

    // ── Parámetros unificados pasados a todos los layouts ─────────────────
    val commonParams = AodCommonParams(
        mediaState = mediaState,
        dominantColor = dominantColor,
        progressFraction = progressFraction,
        displayPosition = displayPosition,
        duration = duration,
        isPlaying = isPlaying,
        canSkipPrev = canSkipPrev,
        canSkipNext = canSkipNext,
        showTitle = showTitle,
        showArtist = showArtist,
        showTime = showTime,
        showProgress = showProgress,
        showControls = showControls,
        showClock = showClock,
        clockText = clockText,
        textScale = textScale,
        controlStyle = controlStyle,
        transitionDuration = transitionDuration,
        onSeekChange = onSeekChange,
        onSeekFinished = onSeekFinished,
        onSkipPrev = { playerConnection.seekToPrevious() },
        onPlayPause = { playerConnection.player.togglePlayPause() },
        onSkipNext = { playerConnection.seekToNext() },
        onCollapse = { navController.navigateUp() },
        isLandscape = isLandscape,
        fullscreen = fullscreenMode,
        contentModifier = contentModifier,
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        when (aodStyle) {
            AodStyle.BACKGROUND -> BackgroundAodLayout(
                params = commonParams,
                artSizeDp = artSizeDp,
                artShape = artShape,
                darkness = darkness,
            )

            AodStyle.MINIMAL -> MinimalAodLayout(params = commonParams)
            AodStyle.SPOTLIGHT -> SpotlightAodLayout(
                params = commonParams,
                artSizeDp = artSizeDp,
                artShape = artShape,
                darkness = darkness,
                spotlightIntensity = spotlightIntensity,
                spotlightPulse = spotlightPulse,
            )
            AodStyle.LARGE -> LargeAodLayout(
                params = commonParams,
                artShape = artShape,
            )
            AodStyle.CLASSIC -> ClassicAodLayout(
                params = commonParams,
                artSizeDp = artSizeDp,
                artShape = artShape,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Datos compartidos — evita param explosion
// ═══════════════════════════════════════════════════════════════════════════

private data class AodCommonParams(
    val mediaState: AodMediaState,
    val dominantColor: Color,
    val progressFraction: Float,
    val displayPosition: Long,
    val duration: Long,
    val isPlaying: Boolean,
    val canSkipPrev: Boolean,
    val canSkipNext: Boolean,
    val showTitle: Boolean,
    val showArtist: Boolean,
    val showTime: Boolean,
    val showProgress: Boolean,
    val showControls: Boolean,
    val showClock: Boolean,
    val clockText: String,
    val textScale: Float,
    val controlStyle: AodControlStyle,
    val transitionDuration: Int,
    val onSeekChange: (Float) -> Unit,
    val onSeekFinished: () -> Unit,
    val onSkipPrev: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSkipNext: () -> Unit,
    val onCollapse: () -> Unit,
    val isLandscape: Boolean,
    val fullscreen: Boolean,
    val contentModifier: Modifier,
)

// ═══════════════════════════════════════════════════════════════════════════
//  CLASSIC
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ClassicAodLayout(
    params: AodCommonParams,
    artSizeDp: Dp,
    artShape: AodArtShape,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!params.fullscreen) {
            CollapseButton(
                onClick = params.onCollapse,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(14.dp)
            )
        }

        if (params.isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(artSizeDp * 1.28f)
                ) {
                    AmbientHalo(color = params.dominantColor)
                    SyncedArtImage(
                        mediaState = params.mediaState,
                        sizeDp = artSizeDp,
                        shape = artShape,
                        duration = params.transitionDuration
                    )
                }
                Spacer(Modifier.width(32.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    AodMetaAndControls(params = params, isLandscape = true)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = params.contentModifier.padding(horizontal = 36.dp)
            ) {
                Spacer(Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(artSizeDp * 1.28f)
                ) {
                    AmbientHalo(color = params.dominantColor)
                    SyncedArtImage(
                        mediaState = params.mediaState,
                        sizeDp = artSizeDp,
                        shape = artShape,
                        duration = params.transitionDuration
                    )
                }
                Spacer(Modifier.height(28.dp))
                AodMetaAndControls(params = params)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BACKGROUND / AMBIENT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BackgroundAodLayout(
    params: AodCommonParams,
    artSizeDp: Dp,
    artShape: AodArtShape,
    darkness: Float,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo borroso sincronizado
        AnimatedContent(
            targetState = params.mediaState.thumbnailUrl,
            transitionSpec = { fadeIn(tween(params.transitionDuration)) togetherWith fadeOut(tween((params.transitionDuration * 0.85f).toInt())) },
            label = "aod_bg"
        ) { url ->
            if (url != null) {
                AsyncImage(
                    model = url, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .cloudy(radius = 100)
                        .graphicsLayer { scaleX = 1.10f; scaleY = 1.10f }
                )
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black))
            }
        }
        // Oscurecimiento
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = (0.28f + darkness * 0.62f).coerceIn(
                            0.22f,
                            0.92f
                        )
                    )
                )
        )
        // Viñetas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.28f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(0.82f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(0.72f)
                        )
                    )
                )
        )

        if (!params.fullscreen) {
            CollapseButton(
                onClick = params.onCollapse,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(14.dp)
            )
        }

        if (params.isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp)
            ) {
                SyncedArtImage(
                    mediaState = params.mediaState,
                    sizeDp = (artSizeDp * 0.72f).coerceAtLeast(100.dp),
                    shape = artShape,
                    duration = params.transitionDuration
                )
                Spacer(Modifier.width(32.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    AodMetaAndControls(params = params, isLandscape = true)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = params.contentModifier.padding(horizontal = 36.dp)
            ) {
                Spacer(Modifier.weight(1f))
                SyncedArtImage(
                    mediaState = params.mediaState,
                    sizeDp = (artSizeDp * 0.72f).coerceAtLeast(100.dp),
                    shape = artShape,
                    duration = params.transitionDuration
                )
                Spacer(Modifier.height(24.dp))
                AodMetaAndControls(params = params)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MINIMAL
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalAodLayout(params: AodCommonParams) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!params.fullscreen) {
            CollapseButton(
                onClick = params.onCollapse,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(14.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = params.contentModifier.padding(horizontal = if (params.isLandscape) 80.dp else 48.dp)
        ) {
            if (params.showClock && params.clockText.isNotEmpty()) {
                SyncedText(key = params.clockText, duration = 400) {
                    Text(
                        text = params.clockText,
                        color = Color.White.copy(alpha = 0.80f),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            AnimatedVisibility(visible = params.showArtist, enter = fadeIn(), exit = fadeOut()) {
                SyncedText(key = params.mediaState.artist, duration = params.transitionDuration) {
                    Text(
                        text = params.mediaState.artist.uppercase(),
                        color = Color.White.copy(alpha = 0.40f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = ((if (params.isLandscape) 12f else 10f) * params.textScale).sp,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )
                }
            }
            if (params.showArtist && params.showTitle) Spacer(Modifier.height(10.dp))
            if (params.showTitle && params.showArtist) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.20f)
                        .height(0.8.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Spacer(Modifier.height(10.dp))
            }
            AnimatedVisibility(visible = params.showTitle, enter = fadeIn(), exit = fadeOut()) {
                SyncedText(key = params.mediaState.title, duration = params.transitionDuration) {
                    Text(
                        text = params.mediaState.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (params.isLandscape) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = ((if (params.isLandscape) 28f else 24f) * params.textScale).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if ((params.showTitle || params.showArtist) && (params.showProgress || params.showControls || params.showTime))
                Spacer(Modifier.height(if (params.isLandscape) 24.dp else 36.dp))

            AnimatedVisibility(visible = params.showProgress, enter = fadeIn(), exit = fadeOut()) {
                SquigglySlider(
                    value = params.progressFraction,
                    onValueChange = params.onSeekChange,
                    onValueChangeFinished = params.onSeekFinished,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    )
                )
            }
            AnimatedVisibility(visible = params.showTime, enter = fadeIn(), exit = fadeOut()) {
                TimeRow(
                    displayPosition = params.displayPosition, duration = params.duration,
                    textScale = params.textScale, isLandscape = params.isLandscape
                )
            }
            if ((params.showProgress || params.showTime) && params.showControls)
                Spacer(Modifier.height(if (params.isLandscape) 24.dp else 32.dp))
            AnimatedVisibility(visible = params.showControls, enter = fadeIn(), exit = fadeOut()) {
                AodControlButtons(
                    isPlaying = params.isPlaying,
                    canSkipPrev = params.canSkipPrev,
                    canSkipNext = params.canSkipNext,
                    onSkipPrev = params.onSkipPrev,
                    onPlayPause = params.onPlayPause,
                    onSkipNext = params.onSkipNext,
                    controlStyle = params.controlStyle,
                    accentColor = params.dominantColor,
                    isLandscape = params.isLandscape,
                    minimal = true,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LARGE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LargeAodLayout(
    params: AodCommonParams,
    artShape: AodArtShape,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Arte a pantalla completa con fade-out inferior
        AnimatedContent(
            targetState = params.mediaState.thumbnailUrl,
            transitionSpec = {
                fadeIn(tween(params.transitionDuration)) togetherWith fadeOut(
                    tween(
                        params.transitionDuration
                    )
                )
            },
            label = "large_art"
        ) { url ->
            Box(modifier = Modifier.fillMaxSize()) {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AsyncImage(
                    model = url, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (params.isLandscape) screenHeight else screenWidth)
                        .clip(artShape.toShape())
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithCache {
                            val gradient = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black,
                                    0.18f to Color.Black,
                                    1f to Color.Transparent
                                )
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = gradient, blendMode = BlendMode.DstIn)
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.42f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(0.7f),
                                    Color.Black.copy(0.92f)
                                )
                            )
                        )
                )
            }
        }

        if (!params.fullscreen) {
            CollapseButton(
                onClick = params.onCollapse,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(14.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(if (!params.fullscreen) Modifier.navigationBarsPadding() else Modifier)
                .padding(horizontal = if (params.isLandscape) 48.dp else 36.dp)
                .padding(bottom = if (params.isLandscape) 32.dp else 28.dp)
        ) {
            if (params.showClock && params.clockText.isNotEmpty()) {
                Text(
                    text = params.clockText, color = Color.White.copy(0.70f),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            AodMetaAndControls(params = params, isLandscape = params.isLandscape)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SPOTLIGHT ★ — halo real con color extraído de la portada
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SpotlightAodLayout(
    params: AodCommonParams,
    artSizeDp: Dp,
    artShape: AodArtShape,
    darkness: Float,
    spotlightIntensity: Float,
    spotlightPulse: Boolean,
) {
    // Color del halo con animación suave al cambiar de canción
    val animatedHaloColor by animateColorAsState(
        targetValue = params.dominantColor,
        animationSpec = tween(params.transitionDuration + 200),
        label = "spotlight_color"
    )

    // Pulso animado (breathing effect)
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseScale by if (spotlightPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val pulseAlpha by if (spotlightPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Capas de brillo del spotlight ───────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val artApproxRadius = artSizeDp.toPx() / 2f
            val cx = size.width / 2f
            val cy = if (params.isLandscape) size.height / 2f else size.height * 0.38f
            val center = Offset(cx, cy)

            // Máxima intensidad configurada por el usuario
            val maxAlpha = spotlightIntensity.coerceIn(0.10f, 1.0f)

            // ① Glow exterior difuso — radio muy amplio, muy translúcido
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedHaloColor.copy(alpha = maxAlpha * 0.22f * pulseAlpha),
                        animatedHaloColor.copy(alpha = maxAlpha * 0.08f * pulseAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = artApproxRadius * 3.6f * pulseScale
                ),
                center = center,
                radius = artApproxRadius * 3.6f * pulseScale
            )

            // ② Halo medio — define el "cono" de luz
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedHaloColor.copy(alpha = maxAlpha * 0.55f * pulseAlpha),
                        animatedHaloColor.copy(alpha = maxAlpha * 0.18f * pulseAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = artApproxRadius * 1.9f * pulseScale
                ),
                center = center,
                radius = artApproxRadius * 1.9f * pulseScale
            )

            // ③ Corona interior brillante — justo alrededor de la portada
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = maxAlpha * 0.45f * pulseAlpha),
                        animatedHaloColor.copy(alpha = maxAlpha * 0.75f * pulseAlpha),
                        animatedHaloColor.copy(alpha = maxAlpha * 0.20f * pulseAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = artApproxRadius * 1.28f * pulseScale
                ),
                center = center,
                radius = artApproxRadius * 1.28f * pulseScale
            )

            // ④ Anillo de borde — contorno sutil con color dominante
            drawCircle(
                color = animatedHaloColor.copy(alpha = maxAlpha * 0.38f * pulseAlpha),
                center = center,
                radius = artApproxRadius * 1.14f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.5.dp.toPx()
                )
            )

            // ⑤ Anillo exterior decorativo
            drawCircle(
                color = animatedHaloColor.copy(alpha = maxAlpha * 0.12f * pulseAlpha),
                center = center,
                radius = artApproxRadius * 1.60f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 0.8.dp.toPx()
                )
            )

            // ⑥ Oscurecimiento del fondo lejos del centro
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = darkness * 0.5f)
                    ),
                    center = center,
                    radius = size.maxDimension * 0.75f
                ),
                center = center,
                radius = size.maxDimension * 0.75f
            )
        }

        if (!params.fullscreen) {
            CollapseButton(
                onClick = params.onCollapse,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(14.dp)
            )
        }

        if (params.isLandscape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp)
            ) {
                SyncedArtImage(
                    mediaState = params.mediaState,
                    sizeDp = artSizeDp,
                    shape = artShape,
                    duration = params.transitionDuration
                )
                Spacer(Modifier.width(32.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    AodMetaAndControls(params = params, isLandscape = true)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = params.contentModifier.padding(horizontal = 36.dp)
            ) {
                Spacer(Modifier.weight(1f))
                SyncedArtImage(
                    mediaState = params.mediaState,
                    sizeDp = artSizeDp,
                    shape = artShape,
                    duration = params.transitionDuration
                )
                Spacer(Modifier.height(32.dp))
                AodMetaAndControls(params = params)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Shared composables
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Imagen de portada con transición sincronizada al [mediaState].
 * Todos los layouts usan este composable → fade del arte ocurre al mismo tiempo
 */
@Composable
private fun SyncedArtImage(
    mediaState: AodMediaState,
    sizeDp: Dp,
    shape: AodArtShape,
    duration: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = mediaState,       // ← sincronizado con title + artist
        transitionSpec = {
            fadeIn(tween(duration)) togetherWith fadeOut(tween((duration * 0.85f).toInt()))
        },
        label = "aod_art_synced"
    ) { state ->
        AsyncImage(
            model = state.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp)
                .clip(shape.toShape())
        )
    }
}

/**
 * Halo ambiente con gradiente radial colorido, reemplaza el Canvas blanco del classic.
 */
@Composable
private fun AmbientHalo(color: Color, modifier: Modifier = Modifier) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(800),
        label = "ambient_halo_color"
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedColor.copy(alpha = 0.22f),
                    animatedColor.copy(alpha = 0.07f),
                    Color.Transparent
                )
            )
        )
    }
}

/**
 * Wrapper que sincroniza el crossfade de texto con la misma key del mediaState.
 */
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
private fun SyncedText(
    key: String,
    duration: Int,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = key,
        transitionSpec = { fadeIn(tween(duration)) togetherWith fadeOut(tween((duration * 0.75f).toInt())) },
        label = "synced_text_$key"
    ) {
        content()
    }
}

@Composable
private fun TimeRow(
    displayPosition: Long,
    duration: Long,
    textScale: Float,
    isLandscape: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            makeTimeString(displayPosition), color = Color.White.copy(0.38f),
            style = MaterialTheme.typography.labelSmall,
            fontSize = ((if (isLandscape) 12f else 10f) * textScale).sp
        )
        if (duration > 0L && duration != C.TIME_UNSET)
            Text(
                makeTimeString(duration), color = Color.White.copy(0.38f),
                style = MaterialTheme.typography.labelSmall,
                fontSize = ((if (isLandscape) 12f else 10f) * textScale).sp
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AodMetaAndControls(
    params: AodCommonParams,
    isLandscape: Boolean = false,
) {
    if (params.showClock && params.clockText.isNotEmpty() && params.mediaState.id != null) {
        Text(
            text = params.clockText,
            color = Color.White.copy(0.60f),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }

    if (params.showTitle) {
        SyncedText(key = params.mediaState.title, duration = params.transitionDuration) {
            Text(
                text = params.mediaState.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = ((if (isLandscape) 28f else 24f) * params.textScale).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
        }
        Spacer(Modifier.height(if (isLandscape) 6.dp else 4.dp))
    }

    if (params.showArtist) {
        SyncedText(key = params.mediaState.artist, duration = params.transitionDuration) {
            Text(
                text = params.mediaState.artist,
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = ((if (isLandscape) 18f else 16f) * params.textScale).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
        }
    }

    if ((params.showTitle || params.showArtist) && (params.showProgress || params.showControls))
        Spacer(Modifier.height(if (isLandscape) 20.dp else 30.dp))

    if (params.showProgress) {
        SquigglySlider(
            value = params.progressFraction,
            onValueChange = params.onSeekChange,
            onValueChangeFinished = params.onSeekFinished,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(0.22f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            )
        )
    }

    if (params.showTime) {
        TimeRow(
            displayPosition = params.displayPosition, duration = params.duration,
            textScale = params.textScale, isLandscape = isLandscape
        )
    }

    if ((params.showProgress || params.showTime) && params.showControls)
        Spacer(Modifier.height(if (isLandscape) 24.dp else 34.dp))

    if (params.showControls) {
        AodControlButtons(
            isPlaying = params.isPlaying,
            canSkipPrev = params.canSkipPrev,
            canSkipNext = params.canSkipNext,
            onSkipPrev = params.onSkipPrev,
            onPlayPause = params.onPlayPause,
            onSkipNext = params.onSkipNext,
            controlStyle = params.controlStyle,
            accentColor = params.dominantColor,
            isLandscape = isLandscape,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOTONES DE CONTROL — soportan AodControlStyle
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AodControlButtons(
    isPlaying: Boolean,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    controlStyle: AodControlStyle,
    accentColor: Color,
    isLandscape: Boolean = false,
    minimal: Boolean = false,
) {
    val buttonSize =
        if (isLandscape) (if (minimal) 56.dp else 64.dp) else (if (minimal) 48.dp else 56.dp)
    val playButtonSize =
        if (isLandscape) (if (minimal) 72.dp else 84.dp) else (if (minimal) 62.dp else 74.dp)
    val iconSize =
        if (isLandscape) (if (minimal) 26.dp else 32.dp) else (if (minimal) 22.dp else 28.dp)
    val playIconSize =
        if (isLandscape) (if (minimal) 32.dp else 38.dp) else (if (minimal) 26.dp else 32.dp)

    val playBg = when (controlStyle) {
        AodControlStyle.ROUNDED, AodControlStyle.SQUARE -> Color.White
        AodControlStyle.ACCENT -> accentColor.copy(alpha = 0.90f)
        AodControlStyle.MINIMAL_FLAT -> Color.White.copy(0.08f)
    }
    val playTint = when (controlStyle) {
        AodControlStyle.ACCENT -> Color.White
        AodControlStyle.MINIMAL_FLAT -> Color.White.copy(0.90f)
        else -> Color.Black
    }

    var expandedButton by remember { mutableStateOf<Int?>(null) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        AodButton(
            iconRes = R.drawable.skip_previous,
            enabled = canSkipPrev,
            baseSize = buttonSize,
            iconSize = iconSize,
            isExpanded = expandedButton == 0,
            controlStyle = controlStyle,
            accentColor = accentColor,
            onClick = { expandedButton = 0; onSkipPrev() },
            onAnimEnd = { expandedButton = null },
            modifier = Modifier.weight(if (expandedButton == 0) 1.3f else 1f)
        )
        Spacer(Modifier.width(2.dp))
        AodButton(
            iconRes = if (isPlaying) R.drawable.pause else R.drawable.play,
            enabled = true,
            baseSize = playButtonSize,
            iconSize = playIconSize,
            isExpanded = expandedButton == 1,
            controlStyle = controlStyle,
            accentColor = accentColor,
            isPlayButton = true,
            playBg = playBg,
            playTint = playTint,
            onClick = { expandedButton = 1; onPlayPause() },
            onAnimEnd = { expandedButton = null },
            modifier = Modifier.weight(if (expandedButton == 1) 1.3f else 1f)
        )
        Spacer(Modifier.width(2.dp))
        AodButton(
            iconRes = R.drawable.skip_next,
            enabled = canSkipNext,
            baseSize = buttonSize,
            iconSize = iconSize,
            isExpanded = expandedButton == 2,
            controlStyle = controlStyle,
            accentColor = accentColor,
            onClick = { expandedButton = 2; onSkipNext() },
            onAnimEnd = { expandedButton = null },
            modifier = Modifier.weight(if (expandedButton == 2) 1.3f else 1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AodButton(
    iconRes: Int,
    enabled: Boolean,
    baseSize: Dp,
    iconSize: Dp,
    isExpanded: Boolean,
    controlStyle: AodControlStyle,
    accentColor: Color,
    isPlayButton: Boolean = false,
    playBg: Color = Color.White,
    playTint: Color = Color.Black,
    onClick: () -> Unit,
    onAnimEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val animatedSize by animateDpAsState(
        targetValue = if (isExpanded) baseSize * 1.2f else baseSize,
        animationSpec = tween(150, easing = LinearEasing),
        finishedListener = { onAnimEnd() },
        label = "btn_size"
    )

    val buttonShape = when (controlStyle) {
        AodControlStyle.SQUARE -> androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        else -> CircleShape
    }

    val bg = when {
        isPlayButton -> playBg
        controlStyle == AodControlStyle.ACCENT ->
            accentColor.copy(alpha = if (enabled) 0.22f else 0.06f)

        else -> Color.White.copy(if (enabled) 0.10f else 0.04f)
    }
    val tint = when {
        isPlayButton -> playTint
        controlStyle == AodControlStyle.ACCENT && enabled ->
            accentColor.copy(alpha = 0.95f)

        else -> Color.White.copy(if (enabled) 0.92f else 0.28f)
    }


    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(animatedSize)
            .clip(buttonShape)
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .animateContentSize()
        )
    }
}

@Composable
private fun CollapseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.09f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(painterResource(R.drawable.expand_more), null,
            tint = Color.White.copy(0.70f),
            modifier = Modifier.size(26.dp)
        )
    }
}