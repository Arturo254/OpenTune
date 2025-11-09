package com.arturo254.opentune.ui.player

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.constants.ShowLyricsKey
import com.arturo254.opentune.constants.SwipeThumbnailKey
import com.arturo254.opentune.ui.component.AppConfig
import com.arturo254.opentune.ui.component.Lyrics
import com.arturo254.opentune.utils.rememberPreference
import kotlin.math.roundToInt

@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    onOpenFullscreenLyrics: () -> Unit, // NUEVO PARÁMETRO
    modifier: Modifier = Modifier,
    changeColor: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()

    var showLyrics by rememberPreference(ShowLyricsKey, false)
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)

    DisposableEffect(showLyrics) {
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier, // QUITADO: .fillMaxSize() y .statusBarsPadding()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxWidth() // SOLO ancho completo, no altura
                        .padding(horizontal = PlayerHorizontalPadding)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragCancel = {
                                    offsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    if (swipeThumbnail) {
                                        offsetX += dragAmount
                                    }
                                },
                                onDragEnd = {
                                    if (offsetX > 300) {
                                        if (playerConnection.player.previousMediaItemIndex != -1) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        }
                                    } else if (offsetX < -300) {
                                        if (playerConnection.player.nextMediaItemIndex != -1) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    offsetX = 0f
                                },
                            )
                        },
            ) {
                var cornerRadius by remember { mutableFloatStateOf(16f) } // Valor por defecto
                val context = LocalContext.current

                // Recuperar el valor de DataStore de manera segura
                LaunchedEffect(Unit) {
                    cornerRadius = AppConfig.getThumbnailCornerRadius(context)
                }

                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(cornerRadius * 2))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    // MODIFICADO: Ahora abre letras en pantalla completa
                                    onOpenFullscreenLyrics()
                                },
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2) {
                                        playerConnection.player.seekBack()
                                    } else {
                                        playerConnection.player.seekForward()
                                    }
                                },
                            )
                        },
                )
            }
        }

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Lyrics(
                sliderPositionProvider = sliderPositionProvider,
            )
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .padding(32.dp)
                    .align(Alignment.Center),
        ) {
            error?.let { error ->
                PlaybackError(
                    error = error,
                    retry = playerConnection.player::prepare,
                )
            }
        }
    }
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    var thumbnailCornerRadiusV2: Float
        get() = prefs.getFloat("THUMBNAIL_CORNER_RADIUS", 16f)
        set(value) = prefs.edit() { putFloat("THUMBNAIL_CORNER_RADIUS", value) }
}