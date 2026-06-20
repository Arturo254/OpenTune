/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import timber.log.Timber
import java.io.ByteArrayOutputStream

object PlayerWidgetUpdater {
    suspend fun update(
        context: Context,
        state: PlayerWidgetState,
    ) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(OpenTunePlayerWidget::class.java)

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(
                    context,
                    PreferencesGlanceStateDefinition,
                    glanceId
                ) { preferences ->
                    preferences.toMutablePreferences().apply {
                        this[PlayerWidgetStateKeys.Title] = state.title
                        this[PlayerWidgetStateKeys.Artist] = state.artist
                        state.thumbnailUrl?.let {
                            this[PlayerWidgetStateKeys.ThumbnailUrl] = it
                        } ?: remove(PlayerWidgetStateKeys.ThumbnailUrl)
                        this[PlayerWidgetStateKeys.IsPlaying] = state.isPlaying
                        this[PlayerWidgetStateKeys.HasPrevious] = state.hasPrevious
                        this[PlayerWidgetStateKeys.HasNext] = state.hasNext
                        this[PlayerWidgetStateKeys.DurationMs] = state.durationMs
                        this[PlayerWidgetStateKeys.PositionMs] = state.positionMs

                        state.artworkBitmap?.let { bitmap ->
                            this[PlayerWidgetStateKeys.ArtworkBytes] = bitmapToByteArray(bitmap)
                        } ?: remove(PlayerWidgetStateKeys.ArtworkBytes)

                        state.backgroundBlurBitmap?.let { bitmap ->
                            this[PlayerWidgetStateKeys.BackgroundBlurBytes] =
                                bitmapToByteArray(bitmap)
                        } ?: remove(PlayerWidgetStateKeys.BackgroundBlurBytes)

                        state.dominantColor?.let {
                            this[PlayerWidgetStateKeys.DominantColor] = it
                        } ?: remove(PlayerWidgetStateKeys.DominantColor)
                    }
                }
                OpenTunePlayerWidget().update(context, glanceId)
            }
        }.onFailure {
            Timber.tag("PlayerWidgetUpdater").w(it, "Unable to update OpenTune player widget")
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }
}