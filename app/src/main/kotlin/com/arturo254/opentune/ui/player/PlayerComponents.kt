@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import me.saket.squiggles.SquigglySlider
import com.arturo254.opentune.R
import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.component.BottomSheetPageState
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.MenuState
import com.arturo254.opentune.ui.component.PlayerSliderTrack
import com.arturo254.opentune.ui.component.ResizableIconButton
import com.arturo254.opentune.ui.menu.PlayerMenu
import com.arturo254.opentune.ui.theme.PlayerBackgroundColorUtils
import com.arturo254.opentune.ui.component.PlayerSliderColors
import com.arturo254.opentune.ui.component.V8DeviceSelector
import com.arturo254.opentune.ui.utils.ShowMediaInfo
import com.arturo254.opentune.ui.utils.highRes
import com.arturo254.opentune.utils.makeTimeString
import com.skydoves.cloudy.cloudy
import timber.log.Timber

@Composable
fun PlayerTitleSection(
    mediaMetadata: MediaMetadata,
    textBackgroundColor: Color,
    navController: NavController,
    state: BottomSheetState,
    clipboardManager: ClipboardManager,
    context: Context
) {
    AnimatedContent(
        targetState = mediaMetadata.title,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "",
    ) { title ->
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textBackgroundColor,
            modifier =
                Modifier
                    .basicMarquee()
                    .combinedClickable(
                        enabled = true,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            if (mediaMetadata.album != null) {
                                state.snapTo(state.collapsedBound)
                                navController.navigate("album/${mediaMetadata.album.id}")
                            }
                        },
                        onLongClick = {
                            val clip = ClipData.newPlainText("Copied Title", title)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied Title", Toast.LENGTH_SHORT).show()
                        }
                    ),
        )
    }

    Spacer(Modifier.height(6.dp))

    val annotatedString = buildAnnotatedString {
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val tag = "artist_${artist.id.orEmpty()}"
            pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
            withStyle(SpanStyle(color = textBackgroundColor, fontSize = 16.sp)) {
                append(artist.name)
            }
            pop()
            if (index != mediaMetadata.artists.lastIndex) append(", ")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee()
            .padding(end = 12.dp)
    ) {
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        var clickOffset by remember { mutableStateOf<Offset?>(null) }
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.titleMedium.copy(color = textBackgroundColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val tapPosition = event.changes.firstOrNull()?.position
                            if (tapPosition != null) {
                                clickOffset = tapPosition
                            }
                        }
                    }
                }
                .combinedClickable(
                    enabled = true,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        val tapPosition = clickOffset
                        val layout = layoutResult
                        if (tapPosition != null && layout != null) {
                            val offset = layout.getOffsetForPosition(tapPosition)
                            annotatedString.getStringAnnotations(offset, offset)
                                .firstOrNull()
                                ?.let { ann ->
                                    val artistId = ann.item
                                    if (artistId.isNotBlank()) {
                                        navController.navigate("artist/$artistId")
                                        state.collapseSoft()
                                    }
                                }
                        }
                    },
                    onLongClick = {
                        val clip = ClipData.newPlainText("Copied Artist", annotatedString)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied Artist", Toast.LENGTH_SHORT).show()
                    }
                )
        )
    }
}

@Composable
fun PlayerTopActions(
    mediaMetadata: MediaMetadata,
    playerDesignStyle: PlayerDesignStyle,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    state: BottomSheetState,
    bottomSheetPageState: BottomSheetPageState,
    context: Context,
    currentSongLiked: Boolean
) {
    when (playerDesignStyle) {
        PlayerDesignStyle.V2 -> {
            val shareShape = RoundedCornerShape(
                topStart = 50.dp, bottomStart = 50.dp,
                topEnd = 10.dp, bottomEnd = 10.dp
            )

            val favShape = RoundedCornerShape(
                topStart = 10.dp, bottomStart = 10.dp,
                topEnd = 50.dp, bottomEnd = 50.dp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(shareShape)
                        .background(textButtonColor)
                        .clickable {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                ) {
                    Image(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(favShape)
                        .background(textButtonColor)
                        .clickable {
                            playerConnection.toggleLike()
                        }
                ) {
                    Image(
                        painter = painterResource(
                            if (currentSongLiked)
                                R.drawable.favorite
                            else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
            }
        }

        PlayerDesignStyle.V3, PlayerDesignStyle.V5 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { playerConnection.toggleLike() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSongLiked) R.drawable.favorite
                            else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (currentSongLiked)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        else textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        PlayerDesignStyle.V4 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    onClick = { playerConnection.toggleLike() },
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentSongLiked)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                    else textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(
                                if (currentSongLiked) R.drawable.favorite
                                else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            tint = if (currentSongLiked)
                                MaterialTheme.colorScheme.error
                            else textBackgroundColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // More menu button - cinematic glass card
                Surface(
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = {
                                    mediaMetadata.id.let {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(it)
                                        }
                                    }
                                },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(44.dp)
                        .width(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        PlayerDesignStyle.V1 -> {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(textButtonColor)
                        .clickable {
                            val intent =
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                    )
                                }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
            ) {
                Image(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconButtonColor),
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(textButtonColor)
                        .clickable {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = {
                                        mediaMetadata.id.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it)
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
            ) {
                Image(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconButtonColor),
                )
            }
        }

        PlayerDesignStyle.V6 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    shape = RoundedCornerShape(
                        topStart = 50.dp, bottomStart = 50.dp,
                        topEnd = 6.dp, bottomEnd = 6.dp
                    ),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(42.dp)
                        .width(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = { playerConnection.toggleLike() },
                    shape = RoundedCornerShape(50),
                    color = if (currentSongLiked)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                    else textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(42.dp)
                        .width(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(
                                if (currentSongLiked) R.drawable.favorite
                                else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            tint = if (currentSongLiked)
                                MaterialTheme.colorScheme.error
                            else textBackgroundColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = {
                                    mediaMetadata.id.let {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(it)
                                        }
                                    }
                                },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    shape = RoundedCornerShape(
                        topStart = 6.dp, bottomStart = 6.dp,
                        topEnd = 50.dp, bottomEnd = 50.dp
                    ),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(42.dp)
                        .width(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        PlayerDesignStyle.V7 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentSongLiked) textBackgroundColor.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { playerConnection.toggleLike() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSongLiked) R.drawable.favorite
                            else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = if (currentSongLiked) 1f else 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(mediaMetadata.id)
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        PlayerDesignStyle.V8 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentSongLiked) textBackgroundColor.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { playerConnection.toggleLike() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSongLiked) R.drawable.favorite_filled
                            else R.drawable.favorite_outline
                        ),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = if (currentSongLiked) 1f else 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Menú
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(mediaMetadata.id)
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    sliderStyle: SliderStyle,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    textButtonColor: Color,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val safeDuration = if (duration <= 0L) 0f else duration.toFloat()
    val safeValue = (sliderPosition ?: position).toFloat().coerceIn(0f, maxOf(0f, safeDuration))

    StyledPlaybackSlider(
        sliderStyle = sliderStyle,
        value = safeValue,
        valueRange = 0f..maxOf(1f, safeDuration),
        onValueChange = { onValueChange(it.toLong()) },
        onValueChangeFinished = onValueChangeFinished,
        activeColor = textButtonColor,
        isPlaying = isPlaying,
        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledPlaybackSlider(
    sliderStyle: SliderStyle,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    activeColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    when (sliderStyle) {
        SliderStyle.Standard -> {
            Slider(
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                colors = PlayerSliderColors.standardSliderColors(activeColor),
                modifier = modifier
            )
        }

        SliderStyle.Wavy -> {
            SquigglySlider(
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                colors = PlayerSliderColors.wavySliderColors(activeColor),
                modifier = modifier,
                squigglesSpec = SquigglySlider.SquigglesSpec(
                    amplitude = if (isPlaying) 2.dp else 0.dp,
                    strokeWidth = 6.dp
                )
            )
        }

        SliderStyle.Thick -> {
            Slider(
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                colors = PlayerSliderColors.thickSliderColors(activeColor),
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = PlayerSliderColors.thickSliderColors(activeColor),
                        trackHeight = 12.dp
                    )
                },
                modifier = modifier
            )
        }

        SliderStyle.Circular -> {
            SquigglySlider(
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                colors = PlayerSliderColors.circularSliderColors(activeColor),
                modifier = modifier,
                squigglesSpec = SquigglySlider.SquigglesSpec(
                    amplitude = if (isPlaying) 2.dp else 0.dp,
                    strokeWidth = 6.dp
                )
            )
        }

        SliderStyle.Simple -> {
            Slider(
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                colors = PlayerSliderColors.simpleSliderColors(activeColor),
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = PlayerSliderColors.simpleSliderColors(activeColor),
                        trackHeight = 3.dp
                    )
                },
                modifier = modifier
            )
        }
    }
}

@Composable
fun PlayerTimeLabel(
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    textBackgroundColor: Color,
    showRemainingTime: Boolean = false,
    centerContent: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding + 4.dp),
    ) {
        Text(
            text = makeTimeString(sliderPosition ?: position),
            style = MaterialTheme.typography.labelMedium,
            color = textBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        if (centerContent != null) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                centerContent()
            }
        }

        Text(
            text = if (duration != C.TIME_UNSET) {
                if (showRemainingTime) {
                    val remaining = duration - (sliderPosition ?: position)
                    "-${makeTimeString(remaining.coerceAtLeast(0))}"
                } else {
                    makeTimeString(duration)
                }
            } else "",
            style = MaterialTheme.typography.labelMedium,
            color = textBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
fun PlayerPlaybackControls(
    playerDesignStyle: PlayerDesignStyle,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    playPauseRoundness: androidx.compose.ui.unit.Dp,
    playerConnection: PlayerConnection,
    currentSongLiked: Boolean
) {
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    when (playerDesignStyle) {
        PlayerDesignStyle.V2 -> {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val maxW = maxWidth
                val playButtonHeight = maxW / 6f
                val playButtonWidth = playButtonHeight * 1.6f
                val sideButtonHeight = playButtonHeight * 0.8f
                val sideButtonWidth = sideButtonHeight * 1.3f

                val backInteraction = remember { MutableInteractionSource() }
                val playInteraction = remember { MutableInteractionSource() }
                val nextInteraction = remember { MutableInteractionSource() }

                val isBackPressed by backInteraction.collectIsPressedAsState()
                val isPlayPressed by playInteraction.collectIsPressedAsState()
                val isNextPressed by nextInteraction.collectIsPressedAsState()

                val playWeight by animateFloatAsState(
                    targetValue = if (isPlayPressed) 1.6f else 1.2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "playWeightV2"
                )

                val sideWeight by animateFloatAsState(
                    targetValue = if (isBackPressed || isNextPressed) 0.9f else 0.7f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "sideWeightV2"
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalIconButton(
                        onClick = playerConnection::seekToPrevious,
                        enabled = canSkipPrevious,
                        interactionSource = backInteraction,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier
                            .weight(sideWeight)
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FilledIconButton(
                        onClick = {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                        interactionSource = playInteraction,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier
                            .weight(playWeight)
                            .size(width = playButtonWidth, height = playButtonHeight)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        if (isLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(42.dp),
                                color = iconButtonColor,
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    FilledTonalIconButton(
                        onClick = playerConnection::seekToNext,
                        enabled = canSkipNext,
                        interactionSource = nextInteraction,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = textButtonColor,
                            contentColor = iconButtonColor
                        ),
                        modifier = Modifier
                            .weight(sideWeight)
                            .size(width = sideButtonWidth, height = sideButtonHeight)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        PlayerDesignStyle.V3 -> {
            val shuffleInteraction = remember { MutableInteractionSource() }
            val backInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            val repeatInteraction = remember { MutableInteractionSource() }

            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
            val isBackPressed by backInteraction.collectIsPressedAsState()
            val isPlayPressed by playInteraction.collectIsPressedAsState()
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()

            val playSize by animateDpAsState(
                targetValue = if (isPlayPressed) 76.dp else 70.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playSizeV3"
            )

            val sideScale by animateFloatAsState(
                targetValue = if (isBackPressed || isNextPressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "sideScaleV3"
            )

            val extraScale by animateFloatAsState(
                targetValue = if (isShufflePressed || isRepeatPressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "extraScaleV3"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = shuffleInteraction,
                                indication = null
                            ) {
                                playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                            }
                            .graphicsLayer(scaleX = extraScale, scaleY = extraScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (shuffleModeEnabled) 1f else 0.4f
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(textBackgroundColor.copy(alpha = 0.08f))
                            .clickable(
                                enabled = canSkipPrevious,
                                interactionSource = backInteraction,
                                indication = null
                            ) {
                                playerConnection.seekToPrevious()
                            }
                            .graphicsLayer(scaleX = sideScale, scaleY = sideScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 0.9f else 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(playSize)
                            .clip(RoundedCornerShape(50))
                            .background(textBackgroundColor)
                            .clickable(
                                interactionSource = playInteraction,
                                indication = null
                            ) {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = icBackgroundColor,
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                tint = icBackgroundColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(textBackgroundColor.copy(alpha = 0.08f))
                            .clickable(
                                enabled = canSkipNext,
                                interactionSource = nextInteraction,
                                indication = null
                            ) {
                                playerConnection.seekToNext()
                            }
                            .graphicsLayer(scaleX = sideScale, scaleY = sideScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(alpha = if (canSkipNext) 0.9f else 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = repeatInteraction,
                                indication = null
                            ) {
                                playerConnection.player.toggleRepeatMode()
                            }
                            .graphicsLayer(scaleX = extraScale, scaleY = extraScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.4f else 1f
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        PlayerDesignStyle.V4 -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                val baseLarge = 56.dp
                val baseSmall = 46.dp
                val baseGap = 12.dp
                val baseLargeIcon = 28.dp
                val baseSmallIcon = 22.dp
                val baseLargeRadius = 18.dp
                val baseSmallRadius = 16.dp
                val centerSize = 88.dp
                val centerPadding = 40.dp
                val sideTotal = (maxWidth - centerSize - centerPadding) / 2f
                val scale =
                    ((sideTotal - baseGap) / (baseLarge + baseSmall)).coerceAtMost(1f).coerceAtLeast(0.6f)
                val large = baseLarge * scale
                val small = baseSmall * scale
                val gap = baseGap * scale
                val largeIcon = baseLargeIcon * scale
                val smallIcon = baseSmallIcon * scale
                val largeRadius = baseLargeRadius * scale
                val smallRadius = baseSmallRadius * scale

                val shuffleInteraction = remember { MutableInteractionSource() }
                val backInteraction = remember { MutableInteractionSource() }
                val playInteraction = remember { MutableInteractionSource() }
                val nextInteraction = remember { MutableInteractionSource() }
                val repeatInteraction = remember { MutableInteractionSource() }

                val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
                val isBackPressed by backInteraction.collectIsPressedAsState()
                val isPlayPressed by playInteraction.collectIsPressedAsState()
                val isNextPressed by nextInteraction.collectIsPressedAsState()
                val isRepeatPressed by repeatInteraction.collectIsPressedAsState()

                val playScale by animateFloatAsState(
                    targetValue = if (isPlayPressed) 0.92f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "playScaleV4"
                )

                val sideScale by animateFloatAsState(
                    targetValue = if (isBackPressed || isNextPressed) 0.92f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "sideScaleV4"
                )

                val extraScale by animateFloatAsState(
                    targetValue = if (isShufflePressed || isRepeatPressed) 0.92f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "extraScaleV4"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                            },
                            interactionSource = shuffleInteraction,
                            shape = RoundedCornerShape(smallRadius),
                            color = textBackgroundColor.copy(
                                alpha = if (shuffleModeEnabled) 0.2f else 0.08f
                            ),
                            modifier = Modifier
                                .size(small)
                                .graphicsLayer(scaleX = extraScale, scaleY = extraScale)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (shuffleModeEnabled) 1f else 0.6f
                                    ),
                                    modifier = Modifier.size(smallIcon)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(gap))

                        Surface(
                            onClick = { playerConnection.seekToPrevious() },
                            enabled = canSkipPrevious,
                            interactionSource = backInteraction,
                            shape = RoundedCornerShape(largeRadius),
                            color = textBackgroundColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(large)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (canSkipPrevious) 1f else 0.4f
                                    ),
                                    modifier = Modifier.size(largeIcon)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                        interactionSource = playInteraction,
                        shape = RoundedCornerShape(28.dp),
                        color = textButtonColor,
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .size(88.dp)
                            .graphicsLayer(scaleX = playScale, scaleY = playScale)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = icBackgroundColor,
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        when {
                                            playbackState == STATE_ENDED -> R.drawable.replay
                                            isPlaying -> R.drawable.pause
                                            else -> R.drawable.play
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = icBackgroundColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { playerConnection.seekToNext() },
                            enabled = canSkipNext,
                            interactionSource = nextInteraction,
                            shape = RoundedCornerShape(largeRadius),
                            color = textBackgroundColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(large)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (canSkipNext) 1f else 0.4f
                                    ),
                                    modifier = Modifier.size(largeIcon)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(gap))

                        Surface(
                            onClick = { playerConnection.player.toggleRepeatMode() },
                            interactionSource = repeatInteraction,
                            shape = RoundedCornerShape(smallRadius),
                            color = textBackgroundColor.copy(
                                alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 0.2f else 0.08f
                            ),
                            modifier = Modifier
                                .size(small)
                                .graphicsLayer(scaleX = extraScale, scaleY = extraScale)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> R.drawable.repeat
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = textBackgroundColor.copy(
                                        alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.6f else 1f
                                    ),
                                    modifier = Modifier.size(smallIcon)
                                )
                            }
                        }
                    }
                }
            }
        }

        PlayerDesignStyle.V1, PlayerDesignStyle.V5 -> {
            val repeatInteraction = remember { MutableInteractionSource() }
            val backInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            val likeInteraction = remember { MutableInteractionSource() }

            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
            val isBackPressed by backInteraction.collectIsPressedAsState()
            val isPlayPressed by playInteraction.collectIsPressedAsState()
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val isLikePressed by likeInteraction.collectIsPressedAsState()

            val playSize by animateDpAsState(
                targetValue = if (isPlayPressed) 76.dp else 72.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playSizeV1V5"
            )

            val playAlpha by animateFloatAsState(
                targetValue = if (isPlayPressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playAlphaV1V5"
            )

            val sideScale by animateFloatAsState(
                targetValue = if (isBackPressed || isNextPressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "sideScaleV1V5"
            )

            val extraScale by animateFloatAsState(
                targetValue = if (isRepeatPressed || isLikePressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "extraScaleV1V5"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        },
                        color = textBackgroundColor,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f)
                            .graphicsLayer(scaleX = extraScale, scaleY = extraScale),
                        onClick = {
                            playerConnection.player.toggleRepeatMode()
                        },
                        interactionSource = repeatInteraction
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_previous,
                        enabled = canSkipPrevious,
                        color = textBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale),
                        onClick = playerConnection::seekToPrevious,
                        interactionSource = backInteraction
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier =
                        Modifier
                            .size(playSize)
                            .clip(RoundedCornerShape(playPauseRoundness))
                            .alpha(playAlpha)
                            .background(textButtonColor)
                            .clickable(
                                interactionSource = playInteraction,
                                indication = null
                            ) {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                ) {
                    if (isLoading) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                            color = iconButtonColor,
                        )
                    } else {
                        Image(
                            painter =
                                painterResource(
                                    if (playbackState ==
                                        STATE_ENDED
                                    ) {
                                        R.drawable.replay
                                    } else if (isPlaying) {
                                        R.drawable.pause
                                    } else {
                                        R.drawable.play
                                    },
                                ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconButtonColor),
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .size(36.dp),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_next,
                        enabled = canSkipNext,
                        color = textBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale),
                        onClick = playerConnection::seekToNext,
                        interactionSource = nextInteraction
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border,
                        color = if (currentSongLiked) MaterialTheme.colorScheme.error else textBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .align(Alignment.Center)
                                .graphicsLayer(scaleX = extraScale, scaleY = extraScale),
                        onClick = playerConnection::toggleLike,
                        interactionSource = likeInteraction
                    )
                }
            }
        }

        PlayerDesignStyle.V6 -> {
            val shuffleInteraction = remember { MutableInteractionSource() }
            val backInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            val repeatInteraction = remember { MutableInteractionSource() }

            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
            val isBackPressed by backInteraction.collectIsPressedAsState()
            val isPlayPressed by playInteraction.collectIsPressedAsState()
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()

            val playSize by animateDpAsState(
                targetValue = if (isPlayPressed) 92.dp else 88.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playSizeV6"
            )

            val playHeight by animateDpAsState(
                targetValue = if (isPlayPressed) 84.dp else 80.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playHeightV6"
            )

            val sideScale by animateFloatAsState(
                targetValue = if (isBackPressed || isNextPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "sideScaleV6"
            )

            val extraScale by animateFloatAsState(
                targetValue = if (isShufflePressed || isRepeatPressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "extraScaleV6"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = textBackgroundColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { playerConnection.seekToPrevious() },
                            enabled = canSkipPrevious,
                            interactionSource = backInteraction,
                            shape = RoundedCornerShape(
                                topStart = 22.dp, bottomStart = 22.dp,
                                topEnd = 8.dp, bottomEnd = 8.dp
                            ),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = if (canSkipPrevious) 1f else 0.4f
                                    ),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            onClick = {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                            interactionSource = playInteraction,
                            shape = RoundedCornerShape(28.dp),
                            color = textButtonColor,
                            modifier = Modifier
                                .size(width = playSize, height = playHeight)
                                .graphicsLayer(
                                    scaleX = playSize / 88.dp,
                                    scaleY = playHeight / 80.dp
                                )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularWavyProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = iconButtonColor,
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(
                                            when {
                                                playbackState == STATE_ENDED -> R.drawable.replay
                                                isPlaying -> R.drawable.pause
                                                else -> R.drawable.play
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = iconButtonColor,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            onClick = { playerConnection.seekToNext() },
                            enabled = canSkipNext,
                            interactionSource = nextInteraction,
                            shape = RoundedCornerShape(
                                topStart = 8.dp, bottomStart = 8.dp,
                                topEnd = 22.dp, bottomEnd = 22.dp
                            ),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = if (canSkipNext) 1f else 0.4f
                                    ),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        onClick = {
                            playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                        },
                        interactionSource = shuffleInteraction,
                        shape = RoundedCornerShape(50),
                        color = if (shuffleModeEnabled)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else textBackgroundColor.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer(scaleX = extraScale, scaleY = extraScale)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                tint = if (shuffleModeEnabled)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else textBackgroundColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Surface(
                        onClick = { playerConnection.player.toggleRepeatMode() },
                        interactionSource = repeatInteraction,
                        shape = RoundedCornerShape(50),
                        color = if (repeatMode != Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else textBackgroundColor.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer(scaleX = extraScale, scaleY = extraScale)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                        else -> R.drawable.repeat
                                    }
                                ),
                                contentDescription = null,
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else textBackgroundColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        PlayerDesignStyle.V7 -> {
            // NO SPRING ANIMATIONS - Mantener como está
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                Surface(
                    onClick = playerConnection::seekToPrevious,
                    enabled = canSkipPrevious,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (canSkipPrevious) 1f else 0.4f
                            ),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                color = textBackgroundColor,
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                tint = textBackgroundColor,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                }

                Surface(
                    onClick = { playerConnection.seekToNext() },
                    enabled = canSkipNext,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (canSkipNext) 1f else 0.4f
                            ),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        else -> {
            // Default/fallback para V8 y futuros estilos
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                // Skip Previous
                Surface(
                    onClick = { playerConnection.seekToPrevious() },
                    enabled = canSkipPrevious,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (canSkipPrevious) 1f else 0.4f
                            ),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Play/Pause
                Surface(
                    onClick = {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                    shape = CircleShape,
                    color = textBackgroundColor,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = Color.Black,
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    when {
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        isPlaying -> R.drawable.pause
                                        else -> R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Skip Next
                Surface(
                    onClick = { playerConnection.seekToNext() },
                    enabled = canSkipNext,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (canSkipNext) 1f else 0.4f
                            ),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wrapper composable that combines all player control components.
 * This replaces the large inline controlsContent lambda in BottomSheetPlayer
 * to reduce JIT compilation overhead.
 */
@Composable
fun PlayerControlsContent(
    mediaMetadata: MediaMetadata,
    playerDesignStyle: PlayerDesignStyle,
    sliderStyle: SliderStyle,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    menuState: MenuState,
    bottomSheetPageState: BottomSheetPageState,
    clipboardManager: ClipboardManager,
    context: Context,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    currentFormat: FormatEntity? = null,
    onResetTimer: () -> Unit = {},
) {
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true

    val playPauseRoundness by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "playPauseRoundness",
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            PlayerTitleSection(
                mediaMetadata = mediaMetadata,
                textBackgroundColor = textBackgroundColor,
                navController = navController,
                state = state,
                clipboardManager = clipboardManager,
                context = context
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        PlayerTopActions(
            mediaMetadata = mediaMetadata,
            playerDesignStyle = playerDesignStyle,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            textBackgroundColor = textBackgroundColor,
            playerConnection = playerConnection,
            navController = navController,
            menuState = menuState,
            state = state,
            bottomSheetPageState = bottomSheetPageState,
            context = context,
            currentSongLiked = currentSongLiked
        )
    }

    Spacer(Modifier.height(12.dp))

    PlayerSlider(
        sliderStyle = sliderStyle,
        sliderPosition = sliderPosition,
        position = position,
        duration = duration,
        isPlaying = isPlaying,
        textButtonColor = textButtonColor,
        onValueChange = onSliderValueChange,
        onValueChangeFinished = onSliderValueChangeFinished
    )

    Spacer(Modifier.height(4.dp))

    PlayerTimeLabel(
        sliderPosition = sliderPosition,
        position = position,
        duration = duration,
        textBackgroundColor = textBackgroundColor,
        showRemainingTime = playerDesignStyle == PlayerDesignStyle.V7,
        centerContent = if (playerDesignStyle == PlayerDesignStyle.V7 && currentFormat != null) {
            {
                val codec = currentFormat.mimeType.substringAfter("/").uppercase()
                val label = when {
                    codec.contains("FLAC") || codec.contains("ALAC") -> "Lossless"
                    codec.contains("OPUS") -> codec
                    codec.contains("AAC") -> codec
                    codec.contains("MP4A") -> "AAC"
                    codec.contains("VORBIS") -> "Vorbis"
                    else -> codec
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = textBackgroundColor.copy(alpha = 0.12f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.graphic_eq),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = textBackgroundColor.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = textBackgroundColor.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        } else null,
    )

    Spacer(Modifier.height(12.dp))

    PlayerPlaybackControls(
        playerDesignStyle = playerDesignStyle,
        playbackState = playbackState,
        isPlaying = isPlaying,
        isLoading = isLoading,
        repeatMode = repeatMode,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        textButtonColor = textButtonColor,
        iconButtonColor = iconButtonColor,
        textBackgroundColor = textBackgroundColor,
        icBackgroundColor = icBackgroundColor,
        playPauseRoundness = playPauseRoundness,
        playerConnection = playerConnection,
        currentSongLiked = currentSongLiked
    )
}

@Composable
fun PlayerBackground(
    playerBackground: PlayerBackgroundStyle,
    mediaMetadata: MediaMetadata?,
    gradientColors: List<Color>,
    disableBlur: Boolean,
    blurRadius: Float,
    playerCustomImageUri: String,
    playerCustomBlur: Float,
    playerCustomContrast: Float,
    playerCustomBrightness: Float
) {
    val effectiveBlurRadius = blurRadius.coerceIn(0f, 48f)
    val shouldApplyBlur = !disableBlur && effectiveBlurRadius > 0f
    Box(modifier = Modifier.fillMaxSize()) {
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR -> {
                AnimatedContent(
                    targetState = mediaMetadata?.thumbnailUrl,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { thumbnailUrl ->
                    if (thumbnailUrl != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = thumbnailUrl.highRes(),
                                contentDescription = "Blurred background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().let {
                                    if (shouldApplyBlur) it.blur(radius = effectiveBlurRadius.dp) else it
                                }
                            )
                            val overlayStops = PlayerBackgroundColorUtils.buildBlurOverlayStops(gradientColors)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = overlayStops))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.08f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.GRADIENT -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val gradientColorStops = if (colors.size >= 3) {
                                arrayOf(
                                    0.0f to colors[0].copy(alpha = 0.92f),
                                    0.5f to colors[1].copy(alpha = 0.75f),
                                    1.0f to colors[2].copy(alpha = 0.65f)
                                )
                            } else {
                                arrayOf(
                                    0.0f to colors[0].copy(alpha = 0.9f),
                                    0.6f to colors[0].copy(alpha = 0.55f),
                                    1.0f to Color.Black.copy(alpha = 0.7f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.18f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.COLORING -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        val baseColor = PlayerBackgroundColorUtils.ensureComfortableColor(colors.first())
                        val gradientStops = PlayerBackgroundColorUtils.buildColoringStops(baseColor)
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(baseColor))
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientStops))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.BLUR_GRADIENT -> {
                AnimatedContent(
                    targetState = mediaMetadata?.thumbnailUrl,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { thumbnailUrl ->
                    if (thumbnailUrl != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = thumbnailUrl.highRes(),
                                contentDescription = "Blurred background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().let {
                                    if (shouldApplyBlur) it.blur(radius = effectiveBlurRadius.dp) else it
                                }
                            )
                            val gradientColorStops =
                                PlayerBackgroundColorUtils.buildBlurGradientStops(gradientColors)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.05f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.CUSTOM -> {
                AnimatedContent(
                    targetState = playerCustomImageUri,
                    transitionSpec = {
                        fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
                    },
                    label = ""
                ) { uri ->
                    if (uri.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val blurPx = playerCustomBlur
                            val contrastVal = playerCustomContrast
                            val brightnessVal = playerCustomBrightness

                            val t = (1f - contrastVal) * 128f + (brightnessVal - 1f) * 255f
                            val matrix = floatArrayOf(
                                contrastVal, 0f, 0f, 0f, t,
                                0f, contrastVal, 0f, 0f, t,
                                0f, 0f, contrastVal, 0f, t,
                                0f, 0f, 0f, 1f, 0f,
                            )

                            val cm = ColorMatrix(matrix)

                            AsyncImage(
                                model = Uri.parse(uri),
                                contentDescription = "Custom background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().let {
                                    if (disableBlur) it else it.blur(radius = blurPx.dp)
                                },
                                colorFilter = ColorFilter.colorMatrix(cm)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            PlayerBackgroundStyle.GLOW -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1200)) togetherWith fadeOut(tween(1200))
                    },
                    label = ""
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    val width = size.width
                                    val height = size.height

                                    val baseColor = Color(0xFF050505)

                                    val color1 = colors.getOrElse(0) { Color.DarkGray }
                                    val color2 = colors.getOrElse(1) { color1 }
                                    val color3 = colors.getOrElse(2) { color2 }
                                    val color4 = colors.getOrElse(3) { color1 }
                                    val color5 = colors.getOrElse(4) { color2 }
                                    val color6 = colors.getOrElse(5) { color3 }

                                    val brush1 = Brush.radialGradient(
                                        colors = listOf(
                                            color1.copy(alpha = 0.8f),
                                            color1.copy(alpha = 0.5f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.2f, height * 0.25f),
                                        radius = width * 1.2f
                                    )

                                    val brush2 = Brush.radialGradient(
                                        colors = listOf(
                                            color2.copy(alpha = 0.75f),
                                            color2.copy(alpha = 0.45f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.85f, height * 0.8f),
                                        radius = width * 1.1f
                                    )

                                    val brush3 = Brush.radialGradient(
                                        colors = listOf(
                                            color3.copy(alpha = 0.7f),
                                            color3.copy(alpha = 0.4f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.9f, height * 0.15f),
                                        radius = width * 1.0f
                                    )

                                    val brush4 = Brush.radialGradient(
                                        colors = listOf(
                                            color4.copy(alpha = 0.65f),
                                            color4.copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.1f, height * 0.9f),
                                        radius = width * 1.0f
                                    )

                                    val brush5 = Brush.radialGradient(
                                        colors = listOf(
                                            color5.copy(alpha = 0.6f),
                                            color5.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.5f, height * 0.1f),
                                        radius = width * 0.9f
                                    )

                                    val brush6 = Brush.radialGradient(
                                        colors = listOf(
                                            color6.copy(alpha = 0.6f),
                                            color6.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.5f, height * 0.95f),
                                        radius = width * 0.9f
                                    )

                                    onDrawBehind {
                                        drawRect(color = baseColor)
                                        drawRect(brush = brush1)
                                        drawRect(brush = brush2)
                                        drawRect(brush = brush3)
                                        drawRect(brush = brush4)
                                        drawRect(brush = brush5)
                                        drawRect(brush = brush6)
                                    }
                                }
                        )
                    }
                }
            }

            PlayerBackgroundStyle.GLOW_ANIMATED -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(1200)) togetherWith fadeOut(tween(1200))
                    },
                    label = "GlowAnimatedContent"
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        val infiniteTransition = rememberInfiniteTransition(label = "GlowAnimation")

                        val progress by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(20000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "glowProgress"
                        )

                        fun rotatedColorAt(index: Int): Color {
                            val size = colors.size
                            val idx = index.toFloat() + progress * size
                            val a = kotlin.math.floor(idx).toInt() % size
                            val b = (a + 1) % size
                            val frac = idx - kotlin.math.floor(idx)
                            return androidx.compose.ui.graphics.lerp(colors.getOrElse(a) { Color.DarkGray }, colors.getOrElse(b) { Color.DarkGray }, frac)
                        }

                        fun oscillate(min: Float, max: Float, phase: Float, speed: Float = 1f): Float {
                            val v = kotlin.math.sin(2f * kotlin.math.PI.toFloat() * (progress * speed + phase)).toFloat()
                            return min + (max - min) * ((v + 1f) * 0.5f)
                        }

                        val color1 = rotatedColorAt(0)
                        val color2 = rotatedColorAt(1)
                        val color3 = rotatedColorAt(2)
                        val color4 = rotatedColorAt(3)
                        val color5 = rotatedColorAt(4)
                        val color6 = rotatedColorAt(5)

                        val o1x = oscillate(0.0f, 1.0f, 0.00f, 1.0f)
                        val o1y = oscillate(0.0f, 0.5f, 0.07f, 1.0f)
                        val r1 = oscillate(0.8f, 1.6f, 0.12f, 1.0f)

                        val o2x = oscillate(1.0f, 0.0f, 0.2f, 1.0f)
                        val o2y = oscillate(0.5f, 1.0f, 0.25f, 1.0f)
                        val r2 = oscillate(0.7f, 1.5f, 0.18f, 1.0f)

                        val o3x = oscillate(0.2f, 0.8f, 0.33f, 1.0f)
                        val o3y = oscillate(0.8f, 0.2f, 0.36f, 1.0f)
                        val r3 = oscillate(0.6f, 1.4f, 0.29f, 1.0f)

                        val o4x = oscillate(0.3f, 0.7f, 0.44f, 1.0f)
                        val o4y = oscillate(0.2f, 0.8f, 0.41f, 1.0f)
                        val r4 = oscillate(0.9f, 1.7f, 0.47f, 1.0f)

                        val o5x = oscillate(0.4f, 0.6f, 0.55f, 1.0f)
                        val o5y = oscillate(0.0f, 1.0f, 0.51f, 1.0f)
                        val r5 = oscillate(0.7f, 1.5f, 0.58f, 1.0f)

                        val o6x = oscillate(0.0f, 1.0f, 0.66f, 1.0f)
                        val o6y = oscillate(0.5f, 0.7f, 0.62f, 1.0f)
                        val r6 = oscillate(0.8f, 1.8f, 0.69f, 1.0f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    val width = size.width
                                    val height = size.height
                                    val baseColor = Color(0xFF050505)

                                    val brush1 = Brush.radialGradient(
                                        colors = listOf(
                                            color1.copy(alpha = 0.85f),
                                            color1.copy(alpha = 0.5f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * o1x, height * o1y),
                                        radius = width * r1
                                    )
                                    val brush2 = Brush.radialGradient(
                                        colors = listOf(
                                            color2.copy(alpha = 0.8f),
                                            color2.copy(alpha = 0.45f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * o2x, height * o2y),
                                        radius = width * r2
                                    )
                                    val brush3 = Brush.radialGradient(
                                        colors = listOf(
                                            color3.copy(alpha = 0.75f),
                                            color3.copy(alpha = 0.4f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * o3x, height * o3y),
                                        radius = width * r3
                                    )
                                    val brush4 = Brush.radialGradient(
                                        colors = listOf(
                                            color4.copy(alpha = 0.7f),
                                            color4.copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * o4x, height * o4y),
                                        radius = width * r4
                                    )
                                    val brush5 = Brush.radialGradient(
                                        colors = listOf(
                                            color5.copy(alpha = 0.65f),
                                            color5.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * o5x, height * o5y),
                                        radius = width * r5
                                    )
                                    val brush6 = Brush.radialGradient(
                                        colors = listOf(
                                            color6.copy(alpha = 0.6f),
                                            color6.copy(alpha = 0.25f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * o6x, height * o6y),
                                        radius = width * r6
                                    )

                                    onDrawBehind {
                                        drawRect(color = baseColor)
                                        drawRect(brush = brush1)
                                        drawRect(brush = brush2)
                                        drawRect(brush = brush3)
                                        drawRect(brush = brush4)
                                        drawRect(brush = brush5)
                                        drawRect(brush = brush6)
                                    }
                                }
                        )
                    }
                }
            }

            else -> {
                // DEFAULT or other modes - no background
            }
        }
    }
}


// ============================================================
// V8 - Apple Music Style Components
// ============================================================

@Composable
fun V8PlayerBackdrop(
    thumbnailUrl: String?,
    canvasArtwork: CanvasArtwork? = null,
    isPlaying: Boolean = false,
    disableBlur: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val cloudyRadius = 100
    val blurMaskStart = 0.42f
    val blurMaskMid = 0.55f
    val blurMaskSolid = 0.72f
    val baseArtworkScale = if (disableBlur) 1.02f else 1.05f
    val baseArtworkAlpha = if (disableBlur) 0.65f else 0.75f

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = {
                fadeIn(tween(800)) togetherWith fadeOut(tween(800))
            },
            label = label,
        ) { artworkUrl ->
            if (artworkUrl != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (canvasArtwork != null && !canvasArtwork.preferredAnimationUrl.isNullOrBlank()) {
                        // ✅ Canvas con RESIZE_MODE_COVER (igual que la imagen estática)
                        CanvasArtworkPlayer(
                            primaryUrl = canvasArtwork.animated,
                            fallbackUrl = canvasArtwork.videoUrl,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize(),
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL  // ← FILL para cubrir todo el espacio
                        )
                        // Overlay con gradiente
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            0.15f to Color.Black.copy(alpha = 0.05f),
                                            0.45f to Color.Black.copy(alpha = 0.25f),
                                            0.70f to Color.Black.copy(alpha = 0.50f),
                                            1f to Color.Black.copy(alpha = 0.85f),
                                        )
                                    )
                                )
                        )
                    } else {
                        // Imagen estática (fallback)
                        AsyncImage(
                            model = artworkUrl.highRes(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = baseArtworkScale
                                    scaleY = baseArtworkScale
                                    alpha = baseArtworkAlpha
                                }
                        )

                        val painter = rememberAsyncImagePainter(
                            model = artworkUrl.highRes()
                        )

                        val imageSize = painter.intrinsicSize

                        val contentScale = remember(imageSize) {
                            if (imageSize.width <= 0 || imageSize.height <= 0) {
                                ContentScale.Crop
                            } else {
                                val aspectRatio = imageSize.width / imageSize.height

                                when {
                                    aspectRatio > 1.35f -> ContentScale.FillHeight
                                    aspectRatio < 0.75f -> ContentScale.FillWidth
                                    else -> ContentScale.Crop
                                }
                            }
                        }

                        if (!disableBlur) {
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = contentScale,
                                alignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .cloudy(radius = cloudyRadius)
                                    .drawWithCache {
                                        val blurMask = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0f to Color.Transparent,
                                                blurMaskStart to Color.Transparent,
                                                blurMaskMid to Color.Black.copy(alpha = 0.5f),
                                                blurMaskSolid to Color.Black,
                                                1f to Color.Black
                                            )
                                        )

                                        onDrawWithContent {
                                            drawContent()

                                            drawRect(
                                                brush = blurMask,
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }

        // Gradiente siempre presente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.20f to Color.Transparent,
                            0.50f to Color.Black.copy(alpha = 0.15f),
                            0.75f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.70f),
                        )
                    )
                )
        )
    }
}

@Composable
fun V8PlayerControlsContent(
    mediaMetadata: MediaMetadata,
    playerDesignStyle: PlayerDesignStyle,
    sliderStyle: SliderStyle,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    menuState: MenuState,
    bottomSheetPageState: BottomSheetPageState,
    clipboardManager: ClipboardManager,
    context: Context,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    currentFormat: FormatEntity? = null,
    onResetTimer: () -> Unit = {},
    nextUpMetadata: MediaMetadata? = null,
    onExpandQueue: () -> Unit = {},
    onShowLyrics: () -> Unit = {},
    playerVolume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // === TOP ROW: Title + Like/Menu (mismo layout/medidas que V7) ===
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Título
                AnimatedContent(
                    targetState = mediaMetadata.title,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "v8_title"
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textBackgroundColor,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Artista
                val artistsText = remember(mediaMetadata.artists) {
                    mediaMetadata.artists.joinToString(separator = ", ") { it.name }
                }
                AnimatedContent(
                    targetState = artistsText,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "v8_artist"
                ) { artists ->
                    Text(
                        text = artists,
                        style = MaterialTheme.typography.titleMedium,
                        color = textBackgroundColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Botones superiores: Like + Menú (38dp, ~10% menos que V7)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentSongLiked) textBackgroundColor.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { playerConnection.toggleLike() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSongLiked) R.drawable.favorite_filled
                            else R.drawable.favorite_outline
                        ),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = if (currentSongLiked) 1f else 0.7f),
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Menú
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(mediaMetadata.id)
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val safeDuration = if (duration <= 0L) 0f else duration.toFloat()
        val safeValue = (sliderPosition ?: position).toFloat().coerceIn(0f, maxOf(0f, safeDuration))
        Slider(
            value = safeValue,
            valueRange = 0f..maxOf(1f, safeDuration),
            onValueChange = { onSliderValueChange(it.toLong()) },
            onValueChangeFinished = onSliderValueChangeFinished,
            colors = PlayerSliderColors.thickSliderColors(textBackgroundColor),
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                val fraction = ((sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                    .coerceIn(0f, 1f)
                GlassTrack(
                    fraction = fraction,
                    trackHeight = 10.dp,
                    tint = textBackgroundColor,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
        )

        Spacer(Modifier.height(4.dp))

        // === TIME ROW: tiempo transcurrido / restante + formato (igual que V7) ===
        PlayerTimeLabel(
            sliderPosition = sliderPosition,
            position = position,
            duration = duration,
            textBackgroundColor = textBackgroundColor,
            showRemainingTime = true,
            centerContent = currentFormat?.let { format ->
                {
                    val codec = format.mimeType.substringAfter("/").uppercase()
                    val label = when {
                        codec.contains("FLAC") || codec.contains("ALAC") -> "Lossless"
                        codec.contains("OPUS") -> codec
                        codec.contains("AAC") -> codec
                        codec.contains("MP4A") -> "AAC"
                        codec.contains("VORBIS") -> "Vorbis"
                        else -> codec
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = textBackgroundColor.copy(alpha = 0.12f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.graphic_eq),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = textBackgroundColor.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        )

        // === A CONTINUACIÓN: preview del siguiente track, rasgo clásico de Apple Music ===
        if (nextUpMetadata != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExpandQueue() }
                    .padding(horizontal = PlayerHorizontalPadding, vertical = 2.dp)
            ) {
                AsyncImage(
                    model = nextUpMetadata.thumbnailUrl?.highRes(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = buildString {
                        append("A continuación: ")
                        append(nextUpMetadata.title)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = textBackgroundColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // === PLAYBACK CONTROLS: tamaños de V7 reducidos ~12% ===
        V8PlaybackControls(
            playbackState = playbackState,
            isPlaying = isPlaying,
            isLoading = isLoading,
            repeatMode = repeatMode,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            textBackgroundColor = textBackgroundColor,
            playerConnection = playerConnection,
            shuffleModeEnabled = shuffleModeEnabled
        )

        Spacer(Modifier.height(14.dp))

        // === VOLUME SLIDER (más corta en los lados que el slider de tiempo) ===
        V8VolumeSlider(
            volume = playerVolume,
            onVolumeChange = onVolumeChange,
            activeColor = textBackgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding + 8.dp)
        )

    }
}

@Composable
fun V8VolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.volume_off),
            contentDescription = null,
            tint = activeColor.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )

        Slider(
            value = volume,
            valueRange = 0f..1f,
            onValueChange = onVolumeChange,
            colors = PlayerSliderColors.thickSliderColors(activeColor),
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                val fraction = ((sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                    .coerceIn(0f, 1f)
                GlassTrack(
                    fraction = fraction,
                    trackHeight = 10.dp,
                    tint = activeColor,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        )

        // Icono grande (volumen alto) a la derecha
        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = null,
            tint = activeColor.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun V8PlaybackControls(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textBackgroundColor: Color,
    playerConnection: PlayerConnection,
    shuffleModeEnabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        // Skip Previous
        Surface(
            onClick = { playerConnection.seekToPrevious() },
            enabled = canSkipPrevious,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Play/Pause - SIN FONDO (como Apple Music)
        Surface(
            onClick = {
                if (playbackState == Player.STATE_ENDED) {
                    playerConnection.player.seekTo(0, 0)
                    playerConnection.player.playWhenReady = true
                } else {
                    playerConnection.player.togglePlayPause()
                }
            },
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = textBackgroundColor,
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            when {
                                playbackState == Player.STATE_ENDED -> R.drawable.replay
                                isPlaying -> R.drawable.pause
                                else -> R.drawable.play
                            }
                        ),
                        contentDescription = null,
                        tint = textBackgroundColor,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Skip Next
        Surface(
            onClick = { playerConnection.seekToNext() },
            enabled = canSkipNext,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipNext) 1f else 0.4f),
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

@Composable
fun QueueCollapsedContentV8(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    onShowLyrics: () -> Unit,
    onExpandQueue: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        // Lyrics (izquierda)
        Surface(
            onClick = onShowLyrics,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.lyrics_apple),
                    contentDescription = "Lyrics",
                    tint = textBackgroundColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            V8DeviceSelector(
                textBackgroundColor = textBackgroundColor,
                modifier = Modifier.size(36.dp)
            )
        }

        // Queue (derecha)
        Surface(
            onClick = onExpandQueue,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Queue",
                    tint = textBackgroundColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


/**
 * Track con efecto "glass" (vidrio esmerilado translúcido), estilo Apple Music.
 * Capa base translúcida + blur sutil + borde tenue + relleno de progreso con highlight.
 */
@Composable
fun GlassTrack(
    fraction: Float,
    trackHeight: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(trackHeight)
            .clip(shape)
    ) {
        // Capa base "glass": translúcida + blur + borde tenue
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 6.dp)
                .background(tint.copy(alpha = 0.16f))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 0.75.dp, color = tint.copy(alpha = 0.3f), shape = shape)
        )

        // Relleno de progreso (vidrio más opaco + highlight superior), sin blur
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.95f),
                            tint.copy(alpha = 0.75f),
                        )
                    )
                )
        )
    }
}