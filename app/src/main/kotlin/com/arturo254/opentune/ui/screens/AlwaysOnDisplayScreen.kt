@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import androidx.compose.ui.unit.sp
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

@Composable
fun AlwaysOnDisplayScreen(navController: NavController) {

    val playerConnection = LocalPlayerConnection.current ?: run {
        LaunchedEffect(Unit) { navController.navigateUp() }
        return
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val context = LocalContext.current
    val window = (context as? Activity)?.window
    DisposableEffect(Unit) {
        window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            WindowInsetsControllerCompat(win, win.decorView).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            window?.let { win ->
                WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    val (rawStyle)     = rememberPreference(AodStyleKey,        AodStyle.CLASSIC.name)
    val (rawShape)     = rememberPreference(AodArtShapeKey,     AodArtShape.ROUNDED.name)
    val (darkness)     = rememberPreference(AodDarknessKey,     0.55f)
    val (artSizeFrac)  = rememberPreference(AodArtSizeKey,      0.65f)
    val (showTitle)    = rememberPreference(AodShowTitleKey,    true)
    val (showArtist)   = rememberPreference(AodShowArtistKey,   true)
    val (showTime)     = rememberPreference(AodShowTimeKey,     true)
    val (showProgress) = rememberPreference(AodShowProgressKey, true)
    val (showControls) = rememberPreference(AodShowControlsKey, true)

    val aodStyle = remember(rawStyle)  { runCatching { AodStyle.valueOf(rawStyle) }.getOrDefault(AodStyle.CLASSIC) }
    val artShape = remember(rawShape)  { runCatching { AodArtShape.valueOf(rawShape) }.getOrDefault(AodArtShape.ROUNDED) }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying     by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipPrev   by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext   by playerConnection.canSkipNext.collectAsState()

    var position by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(mediaMetadata?.id) { mutableLongStateOf(playerConnection.player.duration) }
    var sliderPosition by remember(mediaMetadata?.id) { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaMetadata?.id, playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(200L)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                sliderPosition?.let { if (abs(playerConnection.player.currentPosition - it) <= 1_500L) sliderPosition = null }
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
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val artSizeDp: Dp = remember(artSizeFrac, screenWidthDp) {
        (screenWidthDp * artSizeFrac).coerceIn(120.dp, screenWidthDp)
    }

    val onSeekChange: (Float) -> Unit = { f ->
        if (duration > 0L && duration != C.TIME_UNSET)
            sliderPosition = (f * duration).toLong().coerceIn(0L, duration)
    }
    val onSeekFinished: () -> Unit = {
        sliderPosition?.let { playerConnection.player.seekTo(it); position = it }
        sliderPosition = null
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (aodStyle) {
            AodStyle.BACKGROUND -> BackgroundAodLayout(
                thumbnailUrl = mediaMetadata?.thumbnailUrl, darkness = darkness,
                artSizeDp = artSizeDp, artShape = artShape,
                title = mediaMetadata?.title.orEmpty(), artist = artistText,
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist, showTime = showTime,
                showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = { playerConnection.seekToPrevious() },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onSkipNext = { playerConnection.seekToNext() },
                onCollapse = { navController.navigateUp() },
            )
            AodStyle.MINIMAL -> MinimalAodLayout(
                title = mediaMetadata?.title.orEmpty(), artist = artistText,
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist, showTime = showTime,
                showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = { playerConnection.seekToPrevious() },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onSkipNext = { playerConnection.seekToNext() },
                onCollapse = { navController.navigateUp() },
            )
            AodStyle.SPOTLIGHT -> SpotlightAodLayout(
                thumbnailUrl = mediaMetadata?.thumbnailUrl, artSizeDp = artSizeDp,
                artShape = artShape, darkness = darkness,
                title = mediaMetadata?.title.orEmpty(), artist = artistText,
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist, showTime = showTime,
                showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = { playerConnection.seekToPrevious() },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onSkipNext = { playerConnection.seekToNext() },
                onCollapse = { navController.navigateUp() },
            )
            AodStyle.LARGE -> LargeAodLayout(
                thumbnailUrl = mediaMetadata?.thumbnailUrl, artShape = artShape,
                title = mediaMetadata?.title.orEmpty(), artist = artistText,
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist, showTime = showTime,
                showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = { playerConnection.seekToPrevious() },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onSkipNext = { playerConnection.seekToNext() },
                onCollapse = { navController.navigateUp() },
            )
            AodStyle.CLASSIC -> ClassicAodLayout(
                thumbnailUrl = mediaMetadata?.thumbnailUrl, artSizeDp = artSizeDp, artShape = artShape,
                title = mediaMetadata?.title.orEmpty(), artist = artistText,
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
                isPlaying = isPlaying, canSkipPrev = canSkipPrev, canSkipNext = canSkipNext,
                showTitle = showTitle, showArtist = showArtist, showTime = showTime,
                showProgress = showProgress, showControls = showControls,
                onSeekChange = onSeekChange, onSeekFinished = onSeekFinished,
                onSkipPrev = { playerConnection.seekToPrevious() },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onSkipNext = { playerConnection.seekToNext() },
                onCollapse = { navController.navigateUp() },
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  CLASSIC — fondo negro, portada centrada con halo ambiente
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ClassicAodLayout(
    thumbnailUrl: String?, artSizeDp: Dp, artShape: AodArtShape,
    title: String, artist: String,
    progressFraction: Float, displayPosition: Long, duration: Long,
    isPlaying: Boolean, canSkipPrev: Boolean, canSkipNext: Boolean,
    showTitle: Boolean, showArtist: Boolean, showTime: Boolean,
    showProgress: Boolean, showControls: Boolean,
    onSeekChange: (Float) -> Unit, onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit, onPlayPause: () -> Unit, onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp)
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

            // Art with ambient glow ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(artSizeDp * 1.28f)
            ) {
                // Soft ambient glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.055f),
                                Color.White.copy(alpha = 0.018f),
                                Color.Transparent
                            )
                        )
                    )
                }
                ArtImage(url = thumbnailUrl, sizeDp = artSizeDp, shape = artShape)
            }

            Spacer(Modifier.height(28.dp))

            AodMetaAndControls(
                title = title, artist = artist,
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
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
//  BACKGROUND / AMBIENT — arte difuminado de fondo, dual vignette
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun BackgroundAodLayout(
    thumbnailUrl: String?, darkness: Float, artSizeDp: Dp, artShape: AodArtShape,
    title: String, artist: String,
    progressFraction: Float, displayPosition: Long, duration: Long,
    isPlaying: Boolean, canSkipPrev: Boolean, canSkipNext: Boolean,
    showTitle: Boolean, showArtist: Boolean, showTime: Boolean,
    showProgress: Boolean, showControls: Boolean,
    onSeekChange: (Float) -> Unit, onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit, onPlayPause: () -> Unit, onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen blurred art ──────────────────────────────────────────
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(800)) },
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
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }

        // ── Dark overlay ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = (0.28f + darkness * 0.62f).coerceIn(0.22f, 0.92f)))
        )

        // ── Top vignette ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.28f).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.82f), Color.Transparent)))
        )

        // ── Bottom vignette (stronger, for legibility) ───────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.38f).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.72f))))
        )

        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp)
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

            // Smaller art (fondo already shows it large)
            ArtImage(
                url = thumbnailUrl,
                sizeDp = (artSizeDp * 0.72f).coerceAtLeast(100.dp),
                shape = artShape
            )

            Spacer(Modifier.height(24.dp))

            AodMetaAndControls(
                title = title, artist = artist,
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
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
//  MINIMAL — watch-like, ultra clean, zero art
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalAodLayout(
    title: String, artist: String,
    progressFraction: Float, displayPosition: Long, duration: Long,
    isPlaying: Boolean, canSkipPrev: Boolean, canSkipNext: Boolean,
    showTitle: Boolean, showArtist: Boolean, showTime: Boolean,
    showProgress: Boolean, showControls: Boolean,
    onSeekChange: (Float) -> Unit, onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit, onPlayPause: () -> Unit, onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 48.dp)
        ) {
            // Artist — small, letter-spaced, muted
            AnimatedVisibility(visible = showArtist, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = artist.uppercase(),
                    color = Color.White.copy(alpha = 0.40f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().basicMarquee()
                )
            }

            if (showArtist && showTitle) Spacer(Modifier.height(10.dp))

            // Thin separator line
            if (showTitle && showArtist) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.20f).height(0.8.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Spacer(Modifier.height(10.dp))
            }

            // Title — large, prominent
            AnimatedVisibility(visible = showTitle, enter = fadeIn(), exit = fadeOut()) {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                    label = "min_title"
                ) { t ->
                    Text(
                        text = t,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if ((showTitle || showArtist) && (showProgress || showControls || showTime)) {
                Spacer(Modifier.height(36.dp))
            }

            // Progress — ultra thin
            AnimatedVisibility(visible = showProgress, enter = fadeIn(), exit = fadeOut()) {
                SquigglySlider(
                    value = progressFraction,
                    onValueChange = onSeekChange,
                    onValueChangeFinished = onSeekFinished,
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

            // Time labels — small, very muted
            AnimatedVisibility(visible = showTime, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(displayPosition),
                        color = Color.White.copy(alpha = 0.38f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                    if (duration > 0L && duration != C.TIME_UNSET) {
                        Text(
                            text = makeTimeString(duration),
                            color = Color.White.copy(alpha = 0.38f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if ((showProgress || showTime) && showControls) Spacer(Modifier.height(32.dp))

            // Controls — minimal circles
            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AodControlButton(
                        iconRes = R.drawable.skip_previous, enabled = canSkipPrev,
                        size = 48.dp, iconSize = 22.dp, onClick = onSkipPrev
                    )
                    // Play button — slightly larger but still minimal
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(62.dp).clip(CircleShape)
                            .background(Color.White)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, onClick = onPlayPause
                            )
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = null, tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    AodControlButton(
                        iconRes = R.drawable.skip_next, enabled = canSkipNext,
                        size = 48.dp, iconSize = 22.dp, onClick = onSkipNext
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LargeAodLayout(
    thumbnailUrl: String?, artShape: AodArtShape,
    title: String, artist: String,
    progressFraction: Float, displayPosition: Long, duration: Long,
    isPlaying: Boolean, canSkipPrev: Boolean, canSkipNext: Boolean,
    showTitle: Boolean, showArtist: Boolean, showTime: Boolean,
    showProgress: Boolean, showControls: Boolean,
    onSeekChange: (Float) -> Unit, onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit, onPlayPause: () -> Unit, onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen art (ocupa toda la pantalla) ──────────────────────────
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
            label = "large_art"
        ) { url ->
            Box(modifier = Modifier.fillMaxSize()) {


                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                // Imagen a pantalla completa
                AsyncImage(
                    model = url, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(screenWidth)
                        .clip(artShape.toShape())
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithCache {
                            // Fade out bottom 38% of art into black
                            val fadeStart = 0.18f
                            val gradient = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f        to Color.Black,
                                    fadeStart to Color.Black,
                                    1f        to Color.Transparent
                                )
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = gradient, blendMode = BlendMode.DstIn)
                            }
                        }
                )

                // ── Gradient fade en la parte inferior para legibilidad ────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.42f)  // 42% desde abajo
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
            }
        }

        // ── Controls and meta at bottom (flotando sobre el fade) ───────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp)
                .padding(bottom = 28.dp)
        ) {
            AnimatedVisibility(visible = showTitle, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().basicMarquee()
                )
            }

            if (showTitle && showArtist) Spacer(Modifier.height(4.dp))

            AnimatedVisibility(visible = showArtist, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().basicMarquee()
                )
            }

            if ((showTitle || showArtist) && (showProgress || showControls)) Spacer(Modifier.height(20.dp))

            if (showProgress) {
                SquigglySlider(
                    value = progressFraction,
                    onValueChange = onSeekChange,
                    onValueChangeFinished = onSeekFinished,
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

            if (showTime) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        makeTimeString(displayPosition),
                        color = Color.White.copy(0.65f),
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (duration > 0L && duration != C.TIME_UNSET)
                        Text(
                            makeTimeString(duration),
                            color = Color.White.copy(0.65f),
                            style = MaterialTheme.typography.labelMedium
                        )
                }
            }

            if ((showProgress || showTime) && showControls) Spacer(Modifier.height(28.dp))

            if (showControls) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AodControlButton(R.drawable.skip_previous, canSkipPrev, 54.dp, 26.dp, onSkipPrev)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.95f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPlayPause
                            )
                    ) {
                        Icon(
                            painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            null,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    AodControlButton(R.drawable.skip_next, canSkipNext, 54.dp, 26.dp, onSkipNext)
                }
            }
        }

        // ── Collapse button (flotante sobre la imagen, esquina superior derecha) ──
        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(14.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  SPOTLIGHT — anillos concéntricos + halo suave + portada centrada
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SpotlightAodLayout(
    thumbnailUrl: String?, artSizeDp: Dp, artShape: AodArtShape, darkness: Float,
    title: String, artist: String,
    progressFraction: Float, displayPosition: Long, duration: Long,
    isPlaying: Boolean, canSkipPrev: Boolean, canSkipNext: Boolean,
    showTitle: Boolean, showArtist: Boolean, showTime: Boolean,
    showProgress: Boolean, showControls: Boolean,
    onSeekChange: (Float) -> Unit, onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit, onPlayPause: () -> Unit, onSkipNext: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Concentric glow rings ─────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowAlpha = (0.22f - darkness * 0.14f).coerceIn(0.04f, 0.24f)
            val artApproxRadius = artSizeDp.toPx() / 2f
            val cx = size.width / 2f
            val cy = size.height * 0.40f
            val center = Offset(cx, cy)

            // Outermost diffuse ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(glowAlpha * 0.28f), Color.Transparent),
                    center = center, radius = artApproxRadius * 2.2f
                ),
                center = center, radius = artApproxRadius * 2.2f
            )
            // Mid ring stroke (subtle circle outline)
            drawCircle(
                color = Color.White.copy(alpha = glowAlpha * 0.35f),
                center = center,
                radius = artApproxRadius * 1.48f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.6.dp.toPx())
            )
            // Inner warm glow around art
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(glowAlpha * 1.0f),
                        Color.White.copy(glowAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = center, radius = artApproxRadius * 1.22f
                ),
                center = center, radius = artApproxRadius * 1.22f
            )
        }

        CollapseButton(
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(14.dp)
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
                progressFraction = progressFraction, displayPosition = displayPosition, duration = duration,
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
private fun ArtImage(url: String?, sizeDp: Dp, shape: AodArtShape, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = url,
        transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
        label = "aod_art"
    ) { thumbUrl ->
        AsyncImage(
            model = thumbUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = modifier.size(sizeDp).clip(shape.toShape())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AodMetaAndControls(
    title: String, artist: String,
    progressFraction: Float, displayPosition: Long, duration: Long,
    isPlaying: Boolean, canSkipPrev: Boolean, canSkipNext: Boolean,
    showTitle: Boolean, showArtist: Boolean, showTime: Boolean,
    showProgress: Boolean, showControls: Boolean,
    onSeekChange: (Float) -> Unit, onSeekFinished: () -> Unit,
    onSkipPrev: () -> Unit, onPlayPause: () -> Unit, onSkipNext: () -> Unit,
) {
    if (showTitle) {
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "meta_title"
        ) { t ->
            Text(
                text = t, color = Color.White,
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().basicMarquee()
            )
        }
        Spacer(Modifier.height(4.dp))
    }

    if (showArtist) {
        AnimatedContent(
            targetState = artist,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "meta_artist"
        ) { a ->
            Text(
                text = a, color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().basicMarquee()
            )
        }
    }

    if ((showTitle || showArtist) && (showProgress || showControls)) Spacer(Modifier.height(30.dp))

    if (showProgress) {
        SquigglySlider(
            value = progressFraction, onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinished,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White, activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(0.22f),
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
            )
        )
    }

    if (showTime) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(makeTimeString(displayPosition), color = Color.White.copy(0.45f),
                style = MaterialTheme.typography.labelMedium)
            if (duration > 0L && duration != C.TIME_UNSET)
                Text(makeTimeString(duration), color = Color.White.copy(0.45f),
                    style = MaterialTheme.typography.labelMedium)
        }
    }

    if ((showProgress || showTime) && showControls) Spacer(Modifier.height(34.dp))

    if (showControls) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            AodControlButton(R.drawable.skip_previous, canSkipPrev, 56.dp, 28.dp, onSkipPrev)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(74.dp).clip(CircleShape).background(Color.White)
                    .clickable(interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onPlayPause)
            ) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    null, tint = Color.Black, modifier = Modifier.size(32.dp)
                )
            }
            AodControlButton(R.drawable.skip_next, canSkipNext, 56.dp, 28.dp, onSkipNext)
        }
    }
}

@Composable
private fun AodControlButton(iconRes: Int, enabled: Boolean, size: Dp, iconSize: Dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(CircleShape)
            .background(Color.White.copy(if (enabled) 0.10f else 0.04f))
            .clickable(enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick)
    ) {
        Icon(painterResource(iconRes), null,
            tint = Color.White.copy(if (enabled) 0.92f else 0.28f),
            modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun CollapseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(44.dp).clip(CircleShape)
            .background(Color.White.copy(0.09f))
            .clickable(interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick)
    ) {
        Icon(painterResource(R.drawable.expand_more), null,
            tint = Color.White.copy(0.70f), modifier = Modifier.size(26.dp))
    }
}