/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.constants.SwipeSensitivityKey
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.utils.rememberPreference
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.remember
import com.arturo254.opentune.constants.EnableLiquidGlassKey
import com.arturo254.opentune.ui.component.LocalBackdrop
import com.arturo254.opentune.ui.component.drawBackdropCustomShape


@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState
) {
    NewMiniPlayer(
        position = position,
        duration = duration,
        modifier = modifier,
        pureBlack = pureBlack,
        navController = navController,
        state = state
    )
}

@Composable
private fun NewMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.arturo254.opentune.constants.SwipeThumbnailKey, true)
    val enableLiquidGlass by rememberPreference(EnableLiquidGlassKey, defaultValue = false)

    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }
    val backdrop = LocalBackdrop.current

    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .let {
                    if (enableLiquidGlass && backdrop != null) {
                        it.drawBackdropCustomShape(
                            backdrop = backdrop,
                            layer = layer,
                            luminanceAnimation = luminanceAnimation.value,
                            shape = RoundedCornerShape(32.dp)
                        )
                    } else it
                }
                .clip(RoundedCornerShape(32.dp))
                .background(color = if (enableLiquidGlass) Color.Transparent else backgroundColor)
        ) {
            NewMiniPlayerContent(
                pureBlack = pureBlack,
                position = position,
                duration = duration,
                playerConnection = playerConnection,
                navController = navController,
                state = state
            )
        }
    }
}