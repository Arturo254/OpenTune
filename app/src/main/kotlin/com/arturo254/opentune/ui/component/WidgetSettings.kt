/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.WidgetBackgroundMode
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.widget.WidgetPreferences
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backgroundMode by WidgetPreferences.backgroundModeFlow(context)
        .collectAsStateWithLifecycle(initialValue = WidgetBackgroundMode.BLUR)
    val scrimOpacity by WidgetPreferences.scrimOpacityFlow(context)
        .collectAsStateWithLifecycle(initialValue = 0.32f)
    val cornerRadius by WidgetPreferences.cornerRadiusFlow(context)
        .collectAsStateWithLifecycle(initialValue = 24f)
    val showProgressBar by WidgetPreferences.showProgressBarFlow(context)
        .collectAsStateWithLifecycle(initialValue = true)

    val onBackgroundModeChange: (WidgetBackgroundMode) -> Unit = { mode ->
        scope.launch { WidgetPreferences.setBackgroundMode(context, mode) }
    }
    val onScrimOpacityChange: (Float) -> Unit = { value ->
        scope.launch { WidgetPreferences.setScrimOpacity(context, value) }
    }
    val onCornerRadiusChange: (Float) -> Unit = { value ->
        scope.launch { WidgetPreferences.setCornerRadius(context, value) }
    }
    val onShowProgressBarChange: (Boolean) -> Unit = { value ->
        scope.launch { WidgetPreferences.setShowProgressBar(context, value) }
    }

    val availableBackgroundModes = WidgetBackgroundMode.entries.filter {
        it != WidgetBackgroundMode.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.widget_preview),
        )

        WidgetLivePreview(
            backgroundMode = backgroundMode,
            scrimOpacity = scrimOpacity,
            cornerRadius = cornerRadius.dp,
            showProgressBar = showProgressBar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.widget_background),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            availableBackgroundModes.forEach { mode ->
                WidgetBackgroundModeCard(
                    mode = mode,
                    selected = backgroundMode == mode,
                    onClick = { onBackgroundModeChange(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Text(
                text = stringResource(R.string.widget_blur_unavailable_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        WidgetSliderRow(
            title = stringResource(R.string.widget_scrim_intensity),
            value = scrimOpacity,
            valueRange = 0f..0.7f,
            valueText = { "${(it * 100).roundToInt()}%" },
            onValueChangeFinished = onScrimOpacityChange,
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.widget_shape),
        )

        WidgetSliderRow(
            title = stringResource(R.string.widget_corner_radius),
            value = cornerRadius,
            valueRange = 0f..32f,
            valueText = { "${it.roundToInt()}dp" },
            onValueChangeFinished = onCornerRadiusChange,
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.widget_content),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.widget_show_progress_bar)) },
            description = stringResource(R.string.widget_show_progress_bar_desc),
            icon = { Icon(painterResource(R.drawable.buttons), null) },
            checked = showProgressBar,
            onCheckedChange = onShowProgressBarChange,
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.widget_settings_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

/**
 * Vista previa en vivo del widget dentro de la propia pantalla de Settings,
 * usando datos de ejemplo (sin tocar PlayerWidgetState real) para que la
 * persona vea el efecto de cada ajuste sin salir de la app.
 */
@Composable
private fun WidgetLivePreview(
    backgroundMode: WidgetBackgroundMode,
    scrimOpacity: Float,
    cornerRadius: Dp,
    showProgressBar: Boolean,
    modifier: Modifier = Modifier,
) {
    val previewBackground = when (backgroundMode) {
        WidgetBackgroundMode.BLUR -> MaterialTheme.colorScheme.tertiaryContainer
        WidgetBackgroundMode.DOMINANT_COLOR -> MaterialTheme.colorScheme.primaryContainer
        WidgetBackgroundMode.SOLID -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(previewBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimOpacity)),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
            )
            Spacer(modifier = Modifier.padding(start = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.9f)),
                )
                Spacer(modifier = Modifier.padding(top = 6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.7f)),
                )
                if (showProgressBar) {
                    Spacer(modifier = Modifier.padding(top = 10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.28f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetBackgroundModeCard(
    mode: WidgetBackgroundMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(widgetBackgroundModePreviewColor(mode)),
        )
        Text(
            text = widgetBackgroundModeLabel(mode),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun widgetBackgroundModePreviewColor(mode: WidgetBackgroundMode) = when (mode) {
    WidgetBackgroundMode.BLUR -> MaterialTheme.colorScheme.tertiaryContainer
    WidgetBackgroundMode.DOMINANT_COLOR -> MaterialTheme.colorScheme.primaryContainer
    WidgetBackgroundMode.SOLID -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun widgetBackgroundModeLabel(mode: WidgetBackgroundMode): String = when (mode) {
    WidgetBackgroundMode.BLUR -> stringResource(R.string.widget_background_blur)
    WidgetBackgroundMode.DOMINANT_COLOR -> stringResource(R.string.widget_background_dominant_color)
    WidgetBackgroundMode.SOLID -> stringResource(R.string.widget_background_solid)
}

@Composable
private fun WidgetSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
) {
    // El valor que llega desde el DataStore (Flow) solo se actualiza DESPUÉS de
    // soltar el slider, así que durante el arrastre se usa un estado local para
    // que el slider y el texto respondan al instante sin escribir en cada frame.
    var localValue by remember(value) { mutableFloatStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = valueText(localValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = localValue,
            valueRange = valueRange,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChangeFinished(localValue) },
        )
    }
}