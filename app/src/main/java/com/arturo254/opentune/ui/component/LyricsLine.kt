package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.lyrics.LyricsEntry
import com.arturo254.opentune.constants.AppleMusicLyricsBlurKey
import com.arturo254.opentune.ui.screens.settings.LyricsPosition
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun LyricsLine(
    entry: LyricsEntry,
    isSynced: Boolean,
    isActive: Boolean,
    distanceFromCurrent: Int,
    lyricsTextPosition: LyricsPosition,
    textColor: Color,
    textSize: Float,
    lineSpacing: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,

    isAutoScrollActive: Boolean,
    modifier: Modifier = Modifier
) {

    val (appleMusicLyricsBlur) = rememberPreference(AppleMusicLyricsBlurKey, true)

    val blurRadius by animateFloatAsState(
        targetValue = if (!appleMusicLyricsBlur || !isAutoScrollActive || isActive || !isSynced || isSelectionModeActive)
            0f
        else
            6f,
        animationSpec = tween(durationMillis = 600),
        label = "blur"
    )


    val animatedScale by animateFloatAsState(
        targetValue = when {
            !isSynced || isActive -> 1.05f
            distanceFromCurrent == 1 -> 1f
            else -> 0.95f
        },
        animationSpec = tween(durationMillis = 400)
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = when {
            !isSynced || (isSelectionModeActive && isSelected) -> 1f
            isActive -> 1f
            distanceFromCurrent == 1 -> 0.7f
            distanceFromCurrent == 2 -> 0.4f
            else -> 0.2f
        },
        animationSpec = tween(durationMillis = 400)
    )

    val itemModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(
            enabled = true,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            if (isSelected && isSelectionModeActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else Color.Transparent
        )
        .padding(horizontal = 24.dp, vertical = lineSpacing.dp)
        .graphicsLayer {
            this.alpha = animatedAlpha
            this.scaleX = animatedScale
            this.scaleY = animatedScale
        }
        .blur(blurRadius.dp)

    Column(
        modifier = itemModifier,
        horizontalAlignment = when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    ) {
        if (isActive && isSynced) {
            val fillProgress = remember { Animatable(0f) }
            val pulseProgress = remember { Animatable(0f) }

            LaunchedEffect(entry.time) {
                fillProgress.snapTo(0f)
                fillProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1200,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            LaunchedEffect(Unit) {
                while (true) {
                    pulseProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 3000,
                            easing = LinearEasing
                        )
                    )
                    pulseProgress.snapTo(0f)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> Alignment.CenterStart
                    LyricsPosition.CENTER -> Alignment.Center
                    LyricsPosition.RIGHT -> Alignment.CenterEnd
                }
            ) {
                LaunchedEffect(fillProgress.value, pulseProgress.value) {
                    launch {
                        val fill = fillProgress.value
                        val pulse = pulseProgress.value
                        val pulseEffect = (sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f)
                        val glowIntensity = (fill + pulseEffect).coerceIn(0f, 1.2f)

                        val glowBrush = Brush.horizontalGradient(
                            0.0f to textColor.copy(alpha = 0.3f),
                            (fill * 0.7f).coerceIn(0f, 1f) to textColor.copy(alpha = 0.9f),
                            fill to textColor,
                            (fill + 0.1f).coerceIn(0f, 1f) to textColor.copy(alpha = 0.7f),
                            1.0f to textColor.copy(alpha = if (fill >= 1f) 1f else 0.3f)
                        )

                        val styledText = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = textColor.copy(alpha = 0.8f * glowIntensity),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 28f * (1f + pulseEffect)
                                    ),
                                    brush = glowBrush
                                )
                            ) {
                                append(entry.text)
                            }
                        }

                        val bounceScale = if (fill < 0.3f) {
                            1f + (sin(fill * 3.33f * Math.PI.toFloat()) * 0.03f)
                        } else {
                            1f
                        }
                    }
                }

                Text(
                    text = entry.text,
                    fontSize = textSize.sp,
                    textAlign = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> TextAlign.Left
                        LyricsPosition.CENTER -> TextAlign.Center
                        LyricsPosition.RIGHT -> TextAlign.Right
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Línea inactiva
            Text(
                text = entry.text,
                fontSize = textSize.sp,
                color = textColor.copy(alpha = if (isSynced) 0.7f else 1f),
                textAlign = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> TextAlign.Left
                    LyricsPosition.CENTER -> TextAlign.Center
                    LyricsPosition.RIGHT -> TextAlign.Right
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}