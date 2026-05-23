@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
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
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.ui.screens.settings.toShape
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberPreference
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.saket.squiggles.SquigglySlider
import kotlin.math.abs

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun AlwaysOnDisplayScreen(navController: NavController) {

    val playerConnection = LocalPlayerConnection.current ?: run {
        LaunchedEffect(Unit) { navController.navigateUp() }
        return
    }

    // ── Mantener pantalla encendida ──────────────────────────────────────────
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // ── Ocultar status bar ───────────────────────────────────────────────────
    val context = LocalContext.current
    val window = (context as? Activity)?.window
    DisposableEffect(Unit) {
        window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            WindowInsetsControllerCompat(win, win.decorView).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            window?.let { win ->
                WindowInsetsControllerCompat(win, win.decorView)
                    .show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    // ── Preferences ─────────────────────────────────────────────────────────
    val (rawStyle)    = rememberPreference(AodStyleKey,       AodStyle.CLASSIC.name)
    val (rawShape)    = rememberPreference(AodArtShapeKey,    AodArtShape.ROUNDED.name)
    val (darkness)    = rememberPreference(AodDarknessKey,    0.55f)
    val (artSizeFrac) = rememberPreference(AodArtSizeKey,     0.65f)
    val (showTitle)   = rememberPreference(AodShowTitleKey,   true)
    val (showArtist)  = rememberPreference(AodShowArtistKey,  true)
    val (showTime)    = rememberPreference(AodShowTimeKey,    true)
    val (showProgress)= rememberPreference(AodShowProgressKey,true)
    val (showControls)= rememberPreference(AodShowControlsKey,true)

    val aodStyle = remember(rawStyle) {
        runCatching { AodStyle.valueOf(rawStyle) }.getOrDefault(AodStyle.CLASSIC)
    }
    val artShape = remember(rawShape) {
        runCatching { AodArtShape.valueOf(rawShape) }.getOrDefault(AodArtShape.ROUNDED)
    }

    // ── Player state ─────────────────────────────────────────────────────────
    val mediaMetadata  by playerConnection.mediaMetadata.collectAsState()
    val isPlaying      by playerConnection.isPlaying.collectAsState()
    val playbackState  by playerConnection.playbackState.collectAsState()
    val canSkipPrev    by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext    by playerConnection.canSkipNext.collectAsState()

    var position by rememberSaveable(mediaMetadata?.id) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(mediaMetadata?.id) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember(mediaMetadata?.id) { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaMetadata?.id, playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(200L)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                sliderPosition?.let { target ->
                    if (abs(playerConnection.player.currentPosition - target) <= 1_500L)
                        sliderPosition = null
                }
            }
        }
    }

    // ── Derived values ────────────────────────────────────────────────────────
    val displayPosition = sliderPosition ?: position
    val progressFraction = remember(displayPosition, duration) {
        if (duration > 0L)
            (displayPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        else 0f
    }
    val artistText = remember(mediaMetadata?.artists) {
        mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty()
    }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val artSizeDp: Dp = remember(artSizeFrac, screenWidthDp) {
        (screenWidthDp * artSizeFrac).coerceIn(120.dp, screenWidthDp)
    }

    // ── Seek callbacks ────────────────────────────────────────────────────────
    val onSeekChange: (Float) -> Unit = { fraction ->
        if (duration > 0L)
            sliderPosition = (fraction * duration).toLong().coerceIn(0L, duration)
    }
    val onSeekFinished: () -> Unit = {
        sliderPosition?.let { target ->
            playerConnection.player.seekTo(target)
            position = target
        }
        sliderPosition = null
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Layout selector
    // ════════════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        when (aodStyle) {

            // ─────────────────────────────────────────────────────────────────
            AodStyle.BACKGROUND -> BackgroundAodLayout(
                thumbnailUrl       = mediaMetadata?.thumbnailUrl,
                darkness           = darkness,
                artSizeDp          = artSizeDp,
                artShape           = artShape,
                title              = mediaMetadata?.title.orEmpty(),
                artist             = artistText,
                progressFraction   = progressFraction,
                displayPosition    = displayPosition,
                duration           = duration,
                isPlaying          = isPlaying,
                canSkipPrev        = canSkipPrev,
                canSkipNext        = canSkipNext,
                showTitle          = showTitle,
                showArtist         = showArtist,
                showTime           = showTime,
                showProgress       = showProgress,
                showControls       = showControls,
                onSeekChange       = onSeekChange,
                onSeekFinished     = onSeekFinished,
                onSkipPrev         = { playerConnection.seekToPrevious() },
                onPlayPause        = { playerConnection.player.togglePlayPause() },
                onSkipNext         = { playerConnection.seekToNext() },
                onCollapse         = { navController.navigateUp() },
            )

            // ─────────────────────────────────────────────────────────────────
            AodStyle.MINIMAL -> MinimalAodLayout(
                title              = mediaMetadata?.title.orEmpty(),
                artist             = artistText,
                progressFraction   = progressFraction,
                displayPosition    = displayPosition,
                duration           = duration,
                isPlaying          = isPlaying,
                canSkipPrev        = canSkipPrev,
                canSkipNext        = canSkipNext,
                showTitle          = showTitle,
                showArtist         = showArtist,
                showTime           = showTime,
                showProgress       = showProgress,
                showControls       = showControls,
                onSeekChange       = onSeekChange,
                onSeekFinished     = onSeekFinished,
                onSkipPrev         = { playerConnection.seekToPrevious() },
                onPlayPause        = { playerConnection.player.togglePlayPause() },
                onSkipNext         = { playerConnection.seekToNext() },
                onCollapse         = { navController.navigateUp() },
            )

            // ─────────────────────────────────────────────────────────────────
            AodStyle.SPOTLIGHT -> SpotlightAodLayout(
                thumbnailUrl       = mediaMetadata?.thumbnailUrl,
                artSizeDp          = artSizeDp,
                artShape           = artShape,
                darkness           = darkness,
                title              = mediaMetadata?.title.orEmpty(),
                artist             = artistText,
                progressFraction   = progressFraction,
                displayPosition    = displayPosition,
                duration           = duration,
                isPlaying          = isPlaying,
                canSkipPrev        = canSkipPrev,
                canSkipNext        = canSkipNext,
                showTitle          = showTitle,
                showArtist         = showArtist,
                showTime           = showTime,
                showProgress       = showProgress,
                showControls       = showControls,
                onSeekChange       = onSeekChange,
                onSeekFinished     = onSeekFinished,
                onSkipPrev         = { playerConnection.seekToPrevious() },
                onPlayPause        = { playerConnection.player.togglePlayPause() },
                onSkipNext         = { playerConnection.seekToNext() },
                onCollapse         = { navController.navigateUp() },
            )

            // ─────────────────────────────────────────────────────────────────
            // CLASSIC + LARGE comparten el mismo layout; LARGE solo cambia el
            // tamaño (artSizeDp ya viene calculado desde preferences)
            else -> ClassicAodLayout(
                thumbnailUrl       = mediaMetadata?.thumbnailUrl,
                artSizeDp          = artSizeDp,
                artShape           = artShape,
                title              = mediaMetadata?.title.orEmpty(),
                artist             = artistText,
                progressFraction   = progressFraction,
                displayPosition    = displayPosition,
                duration           = duration,
                isPlaying          = isPlaying,
                canSkipPrev        = canSkipPrev,
                canSkipNext        = canSkipNext,
                showTitle          = showTitle,
                showArtist         = showArtist,
                showTime           = showTime,
                showProgress       = showProgress,
                showControls       = showControls,
                onSeekChange       = onSeekChange,
                onSeekFinished     = onSeekFinished,
                onSkipPrev         = { playerConnection.seekToPrevious() },
                onPlayPause        = { playerConnection.player.togglePlayPause() },
                onSkipNext         = { playerConnection.seekToNext() },
                onCollapse         = { navController.navigateUp() },
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  CLASSIC / LARGE layout  (fondo negro, portada centrada)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ClassicAodLayout(
    thumbnailUrl: String?,
    artSizeDp: Dp,
    artShape: AodArtShape,
    title: String,
    artist: String,
    progressFraction: Float,
    displayPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    showTitle: Boolean,
    showArtist: Boolean,
    showTime: Boolean,
    showProgress: Boolean,
    showControls: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // ── Botón colapsar ───────────────────────────────────────────────────
        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(14.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp)
        ) {
            Spacer(Modifier.weight(1f))

            ArtImage(url = thumbnailUrl, sizeDp = artSizeDp, shape = artShape)

            Spacer(Modifier.height(32.dp))

            AodMetaAndControls(
                title = title, artist = artist,
                progressFraction = progressFraction,
                displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist,
                showTime = showTime, showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = onSkipPrev, onPlayPause = onPlayPause, onSkipNext = onSkipNext,
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  BACKGROUND layout  (arte difuminado como fondo)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun BackgroundAodLayout(
    thumbnailUrl: String?,
    darkness: Float,
    artSizeDp: Dp,
    artShape: AodArtShape,
    title: String,
    artist: String,
    progressFraction: Float,
    displayPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    showTitle: Boolean,
    showArtist: Boolean,
    showTime: Boolean,
    showProgress: Boolean,
    showControls: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Fondo: arte difuminado a pantalla completa ────────────────────────
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(900)) },
            label = "aod_bg_art"
        ) { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .cloudy(radius = 90)
                        .graphicsLayer {
                            scaleX = 1.08f; scaleY = 1.08f
                        }
                )
            }
        }

        // ── Overlay oscuro ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (0.3f + darkness * 0.6f).coerceIn(0.25f, 0.92f)))
        )

        // ── Gradiente inferior para legibilidad ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                        )
                    )
                )
        )

        // ── Botón colapsar ────────────────────────────────────────────────────
        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(14.dp)
        )

        // ── Contenido ─────────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp)
        ) {
            Spacer(Modifier.weight(1f))

            // Portada más pequeña (el fondo ya la muestra grande)
            val reducedSize = (artSizeDp * 0.75f).coerceAtLeast(100.dp)
            ArtImage(url = thumbnailUrl, sizeDp = reducedSize, shape = artShape)

            Spacer(Modifier.height(28.dp))

            AodMetaAndControls(
                title = title, artist = artist,
                progressFraction = progressFraction,
                displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist,
                showTime = showTime, showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = onSkipPrev, onPlayPause = onPlayPause, onSkipNext = onSkipNext,
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  MINIMAL layout  (sin portada, solo controles)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun MinimalAodLayout(
    title: String,
    artist: String,
    progressFraction: Float,
    displayPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    showTitle: Boolean,
    showArtist: Boolean,
    showTime: Boolean,
    showProgress: Boolean,
    showControls: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(14.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 40.dp)
        ) {
            Spacer(Modifier.weight(1f))

            AodMetaAndControls(
                title = title, artist = artist,
                progressFraction = progressFraction,
                displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist,
                showTime = showTime, showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = onSkipPrev, onPlayPause = onPlayPause, onSkipNext = onSkipNext,
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  SPOTLIGHT layout  (halo de luz radial detrás de la portada)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SpotlightAodLayout(
    thumbnailUrl: String?,
    artSizeDp: Dp,
    artShape: AodArtShape,
    darkness: Float,
    title: String,
    artist: String,
    progressFraction: Float,
    displayPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    showTitle: Boolean,
    showArtist: Boolean,
    showTime: Boolean,
    showProgress: Boolean,
    showControls: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Halo radial en el centro de la pantalla ───────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowAlpha = (0.25f - darkness * 0.15f).coerceIn(0.05f, 0.28f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = glowAlpha),
                        Color.White.copy(alpha = glowAlpha * 0.5f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height * 0.38f),
                    radius = size.minDimension * 0.62f,
                ),
                center = Offset(size.width / 2f, size.height * 0.38f),
                radius = size.minDimension * 0.62f,
            )
        }

        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(14.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp)
        ) {
            Spacer(Modifier.weight(1f))

            ArtImage(url = thumbnailUrl, sizeDp = artSizeDp, shape = artShape)

            Spacer(Modifier.height(32.dp))

            AodMetaAndControls(
                title = title, artist = artist,
                progressFraction = progressFraction,
                displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist,
                showTime = showTime, showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = onSkipPrev, onPlayPause = onPlayPause, onSkipNext = onSkipNext,
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Shared composables
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ArtImage(
    url: String?,
    sizeDp: Dp,
    shape: AodArtShape,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = url,
        transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
        label = "aod_art"
    ) { thumbUrl ->
        AsyncImage(
            model = thumbUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(sizeDp)
                .clip(shape.toShape())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AodMetaAndControls(
    title: String,
    artist: String,
    progressFraction: Float,
    displayPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    showTitle: Boolean,
    showArtist: Boolean,
    showTime: Boolean,
    showProgress: Boolean,
    showControls: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
) {
    // ── Título ───────────────────────────────────────────────────────────────
    if (showTitle) {
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "aod_title"
        ) { t ->
            Text(
                text = t,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
        }
        Spacer(Modifier.height(4.dp))
    }

    // ── Artista ──────────────────────────────────────────────────────────────
    if (showArtist) {
        AnimatedContent(
            targetState = artist,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "aod_artist"
        ) { a ->
            Text(
                text = a,
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
        }
    }

    if ((showTitle || showArtist) && (showProgress || showControls)) {
        Spacer(Modifier.height(32.dp))
    }

    // ── WavySlider ───────────────────────────────────────────────────────────
    if (showProgress) {
        SquigglySlider(
            value = progressFraction,
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor         = Color.White,
                activeTrackColor   = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                activeTickColor    = Color.Transparent,
                inactiveTickColor  = Color.Transparent,
            )
        )
    }

    // ── Tiempos ──────────────────────────────────────────────────────────────
    if (showTime) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = makeTimeString(displayPosition),
                color = Color.White.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelMedium
            )
            if (duration > 0L) {
                Text(
                    text = makeTimeString(duration),
                    color = Color.White.copy(alpha = 0.48f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    if ((showProgress || showTime) && showControls) {
        Spacer(Modifier.height(36.dp))
    }

    // ── Controles ─────────────────────────────────────────────────────────────
    if (showControls) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Anterior
            AodControlButton(
                iconRes = R.drawable.skip_previous,
                enabled = canSkipPrev,
                size = 56.dp,
                iconSize = 28.dp,
                onClick = onSkipPrev
            )

            // Play / Pausa
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayPause
                    )
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play
                    ),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Siguiente
            AodControlButton(
                iconRes = R.drawable.skip_next,
                enabled = canSkipNext,
                size = 56.dp,
                iconSize = 28.dp,
                onClick = onSkipNext
            )
        }
    }
}

@Composable
private fun AodControlButton(
    iconRes: Int,
    enabled: Boolean,
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.10f else 0.04f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(iconRes),
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 0.92f else 0.28f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun CollapseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.09f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(R.drawable.expand_more),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.70f),
            modifier = Modifier.size(26.dp)
        )
    }
}