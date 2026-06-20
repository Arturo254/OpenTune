/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import timber.log.Timber

/**
 * Tras editar WidgetPreferences (DataStore), Glance no recompone el widget por sí
 * solo: solo lo hace cuando su propio PreferencesGlanceStateDefinition cambia, o
 * cuando se llama update() explícitamente. Este helper fuerza ese refresco para
 * que los ajustes de WidgetSettings se reflejen de inmediato en el home screen.
 */
object WidgetPreferencesSync {
    suspend fun notifyChanged(context: Context) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(OpenTunePlayerWidget::class.java)
            val widget = OpenTunePlayerWidget()
            glanceIds.forEach { glanceId ->
                widget.update(context, glanceId)
            }
        }.onFailure {
            Timber.tag("WidgetPreferencesSync")
                .w(it, "Unable to refresh widget after preference change")
        }
    }
}
