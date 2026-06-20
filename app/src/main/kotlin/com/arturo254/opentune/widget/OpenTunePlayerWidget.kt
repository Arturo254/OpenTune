/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.WidgetBackgroundMode
import com.arturo254.opentune.widget.PlayerWidgetActions.openAppIntent
import kotlinx.coroutines.flow.first

/** Snapshot inmutable de las preferencias visuales configurables desde WidgetSettings. */
private data class WidgetUiPrefs(
    val backgroundMode: WidgetBackgroundMode,
    val scrimOpacity: Float,
    val cornerRadius: Dp,
    val showProgressBar: Boolean,
)

class OpenTunePlayerWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        // Se leen UNA VEZ por composición (provideGlance ya es suspend). El widget
        // se vuelve a recomponer cada vez que WidgetPreferencesSync.notifyChanged()
        // llama a update() tras un cambio en WidgetSettings, así que no hace falta
        // observar el Flow en vivo dentro del propio Composable de Glance.
        val uiPrefs = WidgetUiPrefs(
            backgroundMode = WidgetPreferences.backgroundModeFlow(context).first(),
            scrimOpacity = WidgetPreferences.scrimOpacityFlow(context).first(),
            cornerRadius = WidgetPreferences.cornerRadiusFlow(context).first().dp,
            showProgressBar = WidgetPreferences.showProgressBarFlow(context).first(),
        )

        provideContent {
            val state = PlayerWidgetState.fromPreferences(currentState<Preferences>())
            GlanceTheme(colors = OpenTuneWidgetColors) {
                PlayerWidgetContent(state = state, uiPrefs = uiPrefs)
            }
        }
    }
}

private val OpenTuneWidgetColors =
    ColorProviders(
        light =
            lightColorScheme(
                primary = Color(0xFF006A6A),
                onPrimary = Color.White,
                surface = Color(0xFFF7FAFA),
                onSurface = Color(0xFF171D1D),
                surfaceVariant = Color(0xFFDCE5E4),
                onSurfaceVariant = Color(0xFF3F4948),
            ),
        dark =
            darkColorScheme(
                primary = Color(0xFF76D1D0),
                onPrimary = Color(0xFF003737),
                surface = Color(0xFF101414),
                onSurface = Color(0xFFE0E3E2),
                surfaceVariant = Color(0xFF3F4948),
                onSurfaceVariant = Color(0xFFC0C9C8),
            ),
    )

@SuppressLint("RestrictedApi")
@Composable
private fun PlayerWidgetContent(state: PlayerWidgetState, uiPrefs: WidgetUiPrefs) {
    val context = LocalContext.current
    val title = state.title.ifBlank { context.getString(R.string.app_name) }
    val artist = state.artist.ifBlank { "OpenTune" }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(uiPrefs.cornerRadius)
            .clickable(actionStartActivity(openAppIntent(context))),
    ) {
        // Capa 1: fondo. Respeta el modo elegido en WidgetSettings, con el mismo
        // fallback en cascada si el dato necesario no está disponible todavía.
        WidgetBackground(state = state, mode = uiPrefs.backgroundMode)

        // Capa 2: scrim semitransparente, intensidad configurable, para que
        // texto/iconos mantengan contraste sin importar el fondo elegido.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black.copy(alpha = uiPrefs.scrimOpacity))),
        ) {}

        // Capa 3: contenido real del widget.
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkBox(bitmap = state.artworkBitmap)
                Spacer(modifier = GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = title,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = artist,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.78f))),
                    )
                }
            }

            if (uiPrefs.showProgressBar) {
                Spacer(modifier = GlanceModifier.height(10.dp))
                ProgressBar(progress = state.progress)
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    icon = R.drawable.skip_previous,
                    contentDescription = "Previous",
                    enabled = state.hasPrevious,
                    action = actionRunCallback<PreviousWidgetAction>(),
                )
                Spacer(modifier = GlanceModifier.width(18.dp))
                PlayPauseButton(isPlaying = state.isPlaying)
                Spacer(modifier = GlanceModifier.width(18.dp))
                ControlButton(
                    icon = R.drawable.skip_next,
                    contentDescription = "Next",
                    enabled = state.hasNext,
                    action = actionRunCallback<NextWidgetAction>(),
                )
            }
        }
    }
}

/**
 * Fondo del widget. Respeta el modo elegido en WidgetSettings, con fallback en
 * cascada si el dato necesario para ese modo no está disponible todavía
 * (p. ej. eligió BLUR pero el blur aún no se calculó, o falló por API < 31):
 * 1. BLUR -> bitmap difuminado; si es null, cae a color dominante.
 * 2. DOMINANT_COLOR -> color extraído con Palette; si es null, cae a surface.
 * 3. SOLID -> siempre el color "surface" plano del tema.
 */
@SuppressLint("RestrictedApi")
@Composable
private fun WidgetBackground(state: PlayerWidgetState, mode: WidgetBackgroundMode) {
    val blur = state.backgroundBlurBitmap
    val dominant = state.dominantColor

    when (mode) {
        WidgetBackgroundMode.BLUR -> {
            when {
                blur != null -> {
                    Image(
                        provider = ImageProvider(blur),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                }

                dominant != null -> SolidBackground(ColorProvider(Color(dominant)))
                else -> SolidBackground(GlanceTheme.colors.surface)
            }
        }

        WidgetBackgroundMode.DOMINANT_COLOR -> {
            if (dominant != null) {
                SolidBackground(ColorProvider(Color(dominant)))
            } else {
                SolidBackground(GlanceTheme.colors.surface)
            }
        }

        WidgetBackgroundMode.SOLID -> {
            SolidBackground(GlanceTheme.colors.surface)
        }
    }
}

@Composable
private fun SolidBackground(color: ColorProvider) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(color),
    ) {}
}

/**
 * Barra de progreso NO interactiva (Glance no soporta sliders arrastrables en
 * RemoteViews). RowScope.defaultWeight() en Glance no acepta un valor de peso
 * (siempre es 1), así que el ancho del "fill" se calcula en Kotlin a partir del
 * ancho real del widget (LocalSize) y se aplica con .width(Dp) directamente.
 */
@SuppressLint("RestrictedApi")
@Composable
private fun ProgressBar(progress: Float) {
    val clamped = progress.coerceIn(0f, 1f)
    val widgetWidth = LocalSize.current.width
    // Resta el padding horizontal de la Column contenedora (14.dp a cada lado).
    val trackWidth = (widgetWidth - 28.dp).coerceAtLeast(0.dp)
    val fillWidth = (trackWidth * clamped)

    Box(
        modifier = GlanceModifier
            .width(trackWidth)
            .height(4.dp)
            .background(ColorProvider(Color.White.copy(alpha = 0.28f)))
            .cornerRadius(2.dp),
    ) {
        Box(
            modifier = GlanceModifier
                .width(fillWidth)
                .height(4.dp)
                .background(ColorProvider(Color.White))
                .cornerRadius(2.dp),
        ) {}
    }
}


@SuppressLint("RestrictedApi")
@Composable
private fun ArtworkBox(bitmap: Bitmap?) {
    Box(
        modifier = GlanceModifier
            .size(64.dp)
            .background(ColorProvider(Color.White.copy(alpha = 0.16f)))
            .cornerRadius(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "Album art",
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(18.dp),
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_music_placeholder),
                contentDescription = null,
                modifier = GlanceModifier.size(46.dp),
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun PlayPauseButton(isPlaying: Boolean) {
    Box(
        modifier =
            GlanceModifier
                .size(52.dp)
                .background(ColorProvider(Color.White))
                .cornerRadius(26.dp)
                .clickable(actionRunCallback<PlayPauseWidgetAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            colorFilter = ColorFilter.tint(ColorProvider(Color.Black)),
            modifier = GlanceModifier.size(28.dp),
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun ControlButton(
    icon: Int,
    contentDescription: String,
    enabled: Boolean,
    action: Action,
) {
    val tint =
        if (enabled) {
            ColorProvider(Color.White)
        } else {
            ColorProvider(Color.White.copy(alpha = 0.35f))
        }
    val backgroundColor = ColorProvider(Color.White.copy(alpha = 0.16f))
    val modifier =
        if (enabled) {
            GlanceModifier
                .size(44.dp)
                .background(backgroundColor)
                .cornerRadius(22.dp)
                .clickable(action)
        } else {
            GlanceModifier
                .size(44.dp)
                .background(backgroundColor)
                .cornerRadius(22.dp)
        }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(24.dp),
        )
    }
}