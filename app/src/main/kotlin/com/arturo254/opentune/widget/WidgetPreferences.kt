/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.arturo254.opentune.constants.WidgetBackgroundMode
import com.arturo254.opentune.constants.WidgetBackgroundModeKey
import com.arturo254.opentune.constants.WidgetCornerRadiusKey
import com.arturo254.opentune.constants.WidgetScrimOpacityKey
import com.arturo254.opentune.constants.WidgetShowProgressBarKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore propio y separado del store principal de la app para las preferencias
 * VISUALES del widget de home screen (fondo, esquinas, scrim, barra de progreso).
 *
 * Se usa un store dedicado en vez del DataStore general de la app porque:
 * 1. El receiver del widget (GlanceAppWidgetReceiver) puede ejecutarse en un
 *    proceso separado del de la Activity; aislar el store evita condiciones de
 *    carrera o acoplamientos accidentales con el resto de los ajustes de la app.
 * 2. Mantiene PlayerWidgetState/PlayerWidgetUpdater (estado de REPRODUCCIÓN)
 *    completamente separados de la CONFIGURACIÓN visual, que vive aquí.
 */
private val Context.widgetDataStore by preferencesDataStore(name = "widget_settings")

object WidgetPreferences {

    fun flow(context: Context): Flow<Preferences> = context.widgetDataStore.data

    fun backgroundModeFlow(context: Context): Flow<WidgetBackgroundMode> =
        context.widgetDataStore.data.map { prefs ->
            prefs[WidgetBackgroundModeKey]
                ?.let { raw -> runCatching { WidgetBackgroundMode.valueOf(raw) }.getOrNull() }
                ?: WidgetBackgroundMode.BLUR
        }

    fun scrimOpacityFlow(context: Context): Flow<Float> =
        context.widgetDataStore.data.map { it[WidgetScrimOpacityKey] ?: 0.32f }

    fun cornerRadiusFlow(context: Context): Flow<Float> =
        context.widgetDataStore.data.map { it[WidgetCornerRadiusKey] ?: 24f }

    fun showProgressBarFlow(context: Context): Flow<Boolean> =
        context.widgetDataStore.data.map { it[WidgetShowProgressBarKey] ?: true }

    suspend fun setBackgroundMode(context: Context, mode: WidgetBackgroundMode) {
        context.widgetDataStore.edit { it[WidgetBackgroundModeKey] = mode.name }
        WidgetPreferencesSync.notifyChanged(context)
    }

    suspend fun setScrimOpacity(context: Context, value: Float) {
        context.widgetDataStore.edit { it[WidgetScrimOpacityKey] = value }
        WidgetPreferencesSync.notifyChanged(context)
    }

    suspend fun setCornerRadius(context: Context, value: Float) {
        context.widgetDataStore.edit { it[WidgetCornerRadiusKey] = value }
        WidgetPreferencesSync.notifyChanged(context)
    }

    suspend fun setShowProgressBar(context: Context, value: Boolean) {
        context.widgetDataStore.edit { it[WidgetShowProgressBarKey] = value }
        WidgetPreferencesSync.notifyChanged(context)
    }
}
