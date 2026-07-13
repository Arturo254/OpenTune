/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Orquestador de proveedores de canvas.
 * Usa Apple Music Artwork Provider como fuente principal.
 * Orden: Apple Music → Custom Canvas → Tidal (fallback)
 */

package com.arturo254.opentune.ui.player

import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.canvas.providers.AppleMusicArtworkProvider
import com.arturo254.opentune.canvas.providers.CustomCanvasProvider
import com.arturo254.opentune.canvas.providers.TidalCanvasProvider
import com.arturo254.opentune.constants.CanvasSource
import timber.log.Timber
import java.util.Locale

/**
 * Intenta obtener el canvas animado para la canción en reproducción.
 *
 * @param songTitleRaw  Título tal como viene del MediaMetadata (puede tener feat., etc.)
 * @param artistNameRaw Nombre del artista tal como viene del MediaMetadata
 * @param albumName     Nombre del álbum (opcional, mejora la precisión)
 * @param source        Fuente a usar; [CanvasSource.AUTO] prueba todas en orden
 */
internal suspend fun fetchCanvasArtworkForPlayback(
    songTitleRaw: String,
    artistNameRaw: String,
    albumName: String? = null,
    source: CanvasSource = CanvasSource.AUTO,
): CanvasArtwork? {
    val songTitle = normalizeCanvasSongTitle(songTitleRaw)
    val artistName = normalizeCanvasArtistName(artistNameRaw)

    Timber.d("🎵 Canvas Resolver - Buscando: $songTitle - $artistName")

    // Genera combinaciones (normalizado + raw) para mayor cobertura de búsqueda
    val candidates = linkedSetOf(
        Triple(songTitle, artistName, albumName),
        Triple(songTitleRaw, artistName, albumName),
        Triple(songTitle, artistNameRaw, albumName),
        Triple(songTitleRaw, artistNameRaw, albumName),
    ).filter { (song, artist, _) -> song.isNotBlank() && artist.isNotBlank() }

    return candidates.firstNotNullOfOrNull { (song, artist, album) ->
        fetchFromSource(song, artist, album, source)
            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
    }
}

private suspend fun fetchFromSource(
    song: String,
    artist: String,
    album: String?,
    source: CanvasSource,
): CanvasArtwork? {
    val result = when (source) {
        CanvasSource.AUTO -> {
            // 🔥 NUEVO ORDEN: Apple Music → Custom Canvas → Tidal (fallback)
            AppleMusicArtworkProvider.getBySongArtist(song, artist, album)
                ?: CustomCanvasProvider.getBySongArtist(song, artist, album ?: song)
                ?: TidalCanvasProvider.getBySongArtist(song, artist, album)
        }

        CanvasSource.APPLE_MUSIC -> {
            AppleMusicArtworkProvider.getBySongArtist(song, artist, album)
        }

        CanvasSource.CUSTOM -> {
            CustomCanvasProvider.getBySongArtist(song, artist, album ?: song)
        }

        CanvasSource.TIDAL -> {
            TidalCanvasProvider.getBySongArtist(song, artist, album)
        }
    }

    if (result != null) {
        Timber.d("🎵 Canvas Resolver - ✅ Encontrado en ${source.name}: ${result.preferredAnimationUrl}")
    } else {
        Timber.d("🎵 Canvas Resolver - ❌ No encontrado para: $song - $artist")
    }

    return result
}

// ---------------------------------------------------------------------------
// Funciones de normalización
// ---------------------------------------------------------------------------

internal fun normalizeCanvasSongTitle(raw: String): String =
    raw
        // Eliminar corchetes: [Official Video], [Remastered], etc.
        .replace(Regex("\\s*\\[[^]]*]"), "")
        // Eliminar feat. / ft. / featuring entre paréntesis
        .replace(
            Regex(
                "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        // Eliminar sufijos de tipo de contenido entre paréntesis
        .replace(
            Regex(
                "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        // Eliminar sufijos separados por guion
        .replace(
            Regex(
                "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun normalizeCanvasArtistName(raw: String): String =
    raw
        .split(
            Regex(
                "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                RegexOption.IGNORE_CASE,
            ),
            limit = 2,
        )
        .firstOrNull().orEmpty()
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun splitAndNormalizeArtists(raw: String): List<String> =
    raw.split(
        Regex(
            "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
            RegexOption.IGNORE_CASE,
        )
    )
        .map { it.replace(Regex("\\s+"), " ").trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }