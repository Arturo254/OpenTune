/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Canvas artwork model unificado — compatible con todos los proveedores.
 */

package com.arturo254.opentune.canvas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val albumName: String? = null,
    val static: String? = null,
    /** URL horizontal/cuadrada animada (HLS .m3u8 o .mp4). */
    val animated: String? = null,
    /** URL vertical animada (9:16). Disponible en algunos proveedores. */
    val animatedVertical: String? = null,
    /** Alias legacy — algunos proveedores retornan la URL aquí. */
    val videoUrl: String? = null,
    val videoUrlVertical: String? = null,
) {
    /** URL de animación preferida (horizontal). */
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl

    /** URL de animación vertical (portrait). */
    val preferredVerticalAnimationUrl: String?
        get() = animatedVertical ?: videoUrlVertical
}
