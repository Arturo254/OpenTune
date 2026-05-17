@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

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

    // ── Ocultar barra de estado para look más AOD ────────────────────────────
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

    // ── Estado del reproductor ───────────────────────────────────────────────
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

    // ── Loop de posición ─────────────────────────────────────────────────────
    LaunchedEffect(mediaMetadata?.id, playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(200L)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                sliderPosition?.let { target ->
                    if (abs(playerConnection.player.currentPosition - target) <= 1_500L) {
                        sliderPosition = null
                    }
                }
            }
        }
    }

    // ── Cálculos derivados ───────────────────────────────────────────────────
    val displayPosition = sliderPosition ?: position
    val progressFraction = remember(displayPosition, duration) {
        if (duration > 0L && duration != C.TIME_UNSET)
            (displayPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        else 0f
    }
    val artistText = remember(mediaMetadata?.artists) {
        mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty()
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UI
    // ════════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)  // Esto ya lo tienes
            // Agrega elevation para asegurar que está por encima
            .graphicsLayer {
                shadowElevation = 10f
                shape = RectangleShape
                clip = false
            }
    ) {

        // ── Botón colapsar ───────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 14.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { navController.navigateUp() }
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.size(26.dp)
            )
        }

        // ── Contenido central ────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp)
        ) {

            Spacer(Modifier.weight(1f))

            // ── Portada ──────────────────────────────────────────────────────
            AnimatedContent(
                targetState = mediaMetadata?.thumbnailUrl,
                transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
                label = "aod_art"
            ) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(264.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(Modifier.height(38.dp))

            // ── Título ───────────────────────────────────────────────────────
            AnimatedContent(
                targetState = mediaMetadata?.title.orEmpty(),
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "aod_title"
            ) { title ->
                Text(
                    text = title,
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

            Spacer(Modifier.height(6.dp))

            // ── Artista ──────────────────────────────────────────────────────
            AnimatedContent(
                targetState = artistText,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "aod_artist"
            ) { artist ->
                Text(
                    text = artist,
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

            Spacer(Modifier.height(42.dp))

            // ── WavySlider de progreso ───────────────────────────────────────
            Slider(
                value = progressFraction,
                onValueChange = { fraction ->
                    if (duration > 0L && duration != C.TIME_UNSET) {
                        sliderPosition = (fraction * duration).toLong().coerceIn(0L, duration)
                    }
                },
                onValueChangeFinished = {
                    sliderPosition?.let { target ->
                        playerConnection.player.seekTo(target)
                        position = target
                    }
                    sliderPosition = null
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor          = Color.White,
                    activeTrackColor    = Color.White,
                    inactiveTrackColor  = Color.White.copy(alpha = 0.22f),
                    activeTickColor     = Color.Transparent,
                    inactiveTickColor   = Color.Transparent,
                )
            )

            // ── Tiempos ──────────────────────────────────────────────────────
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
                if (duration > 0L && duration != C.TIME_UNSET) {
                    Text(
                        text = makeTimeString(duration),
                        color = Color.White.copy(alpha = 0.48f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.height(46.dp))

            // ── Controles ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Anterior
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (canSkipPrev) 0.10f else 0.04f))
                        .clickable(
                            enabled = canSkipPrev,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { playerConnection.seekToPrevious() }
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (canSkipPrev) 0.90f else 0.28f),
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Play / Pausa  ── el botón grande
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { playerConnection.player.togglePlayPause() }
                        )
                ) {
                    Icon(
                        painter = painterResource(
                            // Cambia "play" por "play_arrow" si tu proyecto usa ese nombre
                            if (isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Siguiente
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (canSkipNext) 0.10f else 0.04f))
                        .clickable(
                            enabled = canSkipNext,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { playerConnection.seekToNext() }
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (canSkipNext) 0.90f else 0.28f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}