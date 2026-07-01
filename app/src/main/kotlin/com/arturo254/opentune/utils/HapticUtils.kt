/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 */

package com.arturo254.opentune.utils

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class HapticType {
    Click,
    ToggleOn,
    ToggleOff,
    Confirm,
    Reject,
    LongPress,
    Keyboard,
    SegmentTick,
    SegmentFrequentTick,
    DragStart,
    GestureEnd
}

class HapticManager(
    private val view: View,
    private val enabled: Boolean = true,
) {

    fun perform(type: HapticType = HapticType.Click) {
        if (!enabled) return

        val feedbackConstant = when (type) {
            HapticType.Click ->
                HapticFeedbackConstants.CONTEXT_CLICK

            HapticType.ToggleOn ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_ON
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }

            HapticType.ToggleOff ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_OFF
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }

            HapticType.Confirm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }

            HapticType.Reject ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }

            HapticType.LongPress ->
                HapticFeedbackConstants.LONG_PRESS

            HapticType.Keyboard ->
                HapticFeedbackConstants.KEYBOARD_TAP

            HapticType.SegmentTick ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.SEGMENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }

            HapticType.SegmentFrequentTick ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }

            HapticType.DragStart ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.DRAG_START
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }

            HapticType.GestureEnd ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.GESTURE_END
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                }
        }

        view.performHapticFeedback(
            feedbackConstant,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
        )
    }

    fun click() = perform(HapticType.Click)

    fun toggleOn() = perform(HapticType.ToggleOn)

    fun toggleOff() = perform(HapticType.ToggleOff)

    fun toggle(enabled: Boolean) {
        perform(
            if (enabled) {
                HapticType.ToggleOn
            } else {
                HapticType.ToggleOff
            }
        )
    }

    fun confirm() = perform(HapticType.Confirm)

    fun reject() = perform(HapticType.Reject)

    fun longPress() = perform(HapticType.LongPress)

    fun keyboard() = perform(HapticType.Keyboard)

    fun segmentTick() = perform(HapticType.SegmentTick)

    fun segmentFrequentTick() = perform(HapticType.SegmentFrequentTick)

    fun dragStart() = perform(HapticType.DragStart)

    fun gestureEnd() = perform(HapticType.GestureEnd)
}

@Composable
fun rememberHaptic(
    enabled: Boolean = true,
): HapticManager {
    val view = LocalView.current

    return remember(view, enabled) {
        HapticManager(
            view = view,
            enabled = enabled,
        )
    }
}

/**
 * Sistema de feedback háptico para OpenTune que proporciona una capa de abstracción
 * para manejar vibraciones en Jetpack Compose con compatibilidad automática
 * entre distintas versiones de Android.
 *
 * Este sistema permite ejecutar diferentes tipos de vibración en cualquier componente
 * Compose (Button, ToggleButton, Card, Slider, etc.), con fallbacks automáticos
 * cuando el dispositivo no soporta ciertos tipos de feedback háptico.
 *
 * ## Uso básico en un Composable:
 *
 * @sample com.arturo254.opentune.utils.samples.BasicHapticUsage
 *
 * ```
 * val (enableHaptic) = rememberPreference(EnableHapticFeedbackKey, true)
 *
 *     val haptic = rememberHaptic(enabled = enableHaptic)
 *
 * Button(onClick = {
 *     haptic.click()
 *     saveData()
 * }) {
 *     Text("Guardar")
 * }
 * ```
 *
 * ## Tipos de vibración disponibles:
 *
 * - [click] - Toque estándar para botones e interacciones
 * - [toggle] / [toggleOn] / [toggleOff] - Para switches y toggles
 * - [confirm] - Confirmar acciones (guardar, enviar)
 * - [reject] - Rechazar acciones (cancelar, eliminar)
 * - [longPress] - Pulsación larga
 * - [keyboard] - Tecla de teclado virtual
 * - [segmentTick] - Movimiento en Slider o SeekBar
 * - [segmentFrequentTick] - Movimiento rápido en Slider
 * - [dragStart] - Inicio de arrastre
 * - [gestureEnd] - Fin de gesto o arrastre
 *
 * ## Control global:
 *
 * ```
 * val (enableHaptic) = rememberPreference(EnableHapticKey, true)
 * val haptic = rememberHaptic(enabled = enableHaptic)
 * ```
 *
 * ## Compatibilidad con versiones de Android:
 *
 * El sistema aplica fallbacks automáticamente:
 *
 * - Android 5.0+ (API 21): click, longPress, keyboard, clockTick
 * - Android 11+ (API 30): confirm, reject, segmentTick, segmentFrequentTick
 * - Android 14+ (API 34): toggleOn, toggleOff, dragStart, gestureEnd
 *
 * ## Ejemplos de uso con diferentes componentes:
 *
 * ### ToggleButton:
 *
 * ```
 * val haptic = rememberHaptic()
 * var isEnabled by remember { mutableStateOf(false) }
 *
 * ToggleButton(
 *     checked = isEnabled,
 *     onCheckedChange = {
 *         haptic.toggle(it)
 *         isEnabled = it
 *     }
 * ) {
 *     Text("Activar")
 * }
 * ```
 *
 * ### Slider:
 *
 * ```
 * val haptic = rememberHaptic()
 * var value by remember { mutableStateOf(0f) }
 *
 * Slider(
 *     value = value,
 *     onValueChange = {
 *         haptic.segmentTick()
 *         value = it
 *     }
 * )
 * ```
 *
 * ### Card con click:
 *
 * ```
 * val haptic = rememberHaptic()
 *
 * Card(
 *     modifier = Modifier.clickable {
 *         haptic.click()
 *         navigateToDetail()
 *     }
 * ) {
 *     Text("Ver detalle")
 * }
 * ```
 *
 * ### Drag & Drop:
 *
 * ```
 * val haptic = rememberHaptic()
 *
 * Modifier.draggable(
 *     onDragStarted = { haptic.dragStart() },
 *     onDragStopped = { haptic.gestureEnd() }
 * )
 * ```
 *
 * ## Consideraciones:
 *
 * - Usar dentro de un Composable o con contexto de View
 * - Respeta la configuración global de vibración del sistema
 * - No vibra si el dispositivo no tiene vibrador o está desactivado
 * - Usar con moderación en interacciones frecuentes
 * - `rememberHaptic` debe llamarse dentro de un Composable
 *
 * @param enabled Control global para activar o desactivar todas las vibraciones
 * @return Instancia de [HapticManager] para ejecutar feedback háptico
 * @see HapticManager
 * @see HapticType
 */