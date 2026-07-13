/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Proveedor de canvas de Apple Music usando el servicio apple-music-animated-artworks
 * API: https://artwork.m8tec.top/api/v1/
 * Endpoints:
 *   - GET /artwork/search?artist=ARTIST&album=ALBUM&title=TRACK (opcional)
 *   - GET /artwork/url?url=APPLE_MUSIC_URL
 */

package com.arturo254.opentune.canvas.providers

import com.arturo254.opentune.canvas.models.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Proveedor de canvas de Apple Music usando el servicio apple-music-animated-artworks
 *
 * API Reference:
 *   Base URL: https://artwork.m8tec.top
 *
 *   GET /api/v1/artwork/search?artist=Linkin+Park&album=Living+Things
 *   GET /api/v1/artwork/url?url=https://music.apple.com/us/album/...
 *   GET /api/v1/artwork/history
 */
object AppleMusicArtworkProvider {

    // URL base del servicio
    private const val API_BASE_URL = "https://artwork.m8tec.top/api/v1"

    // Tiempo de espera para la descarga de URLs de Apple Music
    private const val APPLE_MUSIC_TIMEOUT_MS = 30_000L

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = APPLE_MUSIC_TIMEOUT_MS
                socketTimeoutMillis = APPLE_MUSIC_TIMEOUT_MS
            }
            install(ContentEncoding) { gzip(); deflate() }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private data class CacheEntry(val value: CanvasArtwork?, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 horas

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Obtiene el artwork animado por nombre de canción y artista
     * Usa el endpoint: GET /api/v1/artwork/search?artist=ARTIST&album=ALBUM&title=TRACK
     */
    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? {
        val key = cacheKey("song", song, artist, album ?: "")
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            Timber.d("🎵 Apple Music Artwork - Cache hit")
            return it.value
        }

        Timber.d("🎵 Apple Music Artwork - Buscando: $song - $artist")

        val result = searchArtwork(
            artist = artist,
            album = album ?: song, // Si no hay álbum, usar la canción como álbum
            title = song
        )
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    /**
     * Obtiene el artwork animado por nombre de álbum y artista
     * Usa el endpoint: GET /api/v1/artwork/search?artist=ARTIST&album=ALBUM
     */
    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
    ): CanvasArtwork? {
        val key = cacheKey("album", album, artist)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            Timber.d("🎵 Apple Music Artwork - Cache hit")
            return it.value
        }

        Timber.d("🎵 Apple Music Artwork - Buscando álbum: $album - $artist")

        val result = searchArtwork(
            artist = artist,
            album = album,
            title = null
        )
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    /**
     * Obtiene el artwork animado desde una URL de Apple Music
     * Usa el endpoint: GET /api/v1/artwork/url?url=APPLE_MUSIC_URL
     */
    suspend fun getByAppleMusicUrl(url: String): CanvasArtwork? {
        if (url.isBlank()) return null

        val key = "url|$url"
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            Timber.d("🎵 Apple Music Artwork - Cache hit por URL")
            return it.value
        }

        Timber.d("🎵 Apple Music Artwork - Buscando por URL: $url")

        val result = fetchArtworkByUrl(url)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    // -------------------------------------------------------------------------
    // Implementación interna
    // -------------------------------------------------------------------------

    /**
     * Busca artwork por artista, álbum y título opcional
     * GET /api/v1/artwork/search?artist=ARTIST&album=ALBUM&title=TRACK
     */
    private suspend fun searchArtwork(
        artist: String,
        album: String,
        title: String? = null,
    ): CanvasArtwork? {
        return runCatching {
            Timber.d("🎵 Apple Music Artwork - Search: artist=$artist, album=$album, title=$title")

            val response = client.get("$API_BASE_URL/artwork/search") {
                parameter("artist", artist)
                parameter("album", album)
                title?.let { parameter("title", it) }
            }

            if (response.status != HttpStatusCode.OK) {
                Timber.d("🎵 Apple Music Artwork - Error: ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            Timber.d("🎵 Apple Music Artwork - Response recibido")

            val artwork = parseArtworkResponse(root)
            if (artwork != null) {
                Timber.d("🎵 Apple Music Artwork - ✅ Encontrado: ${artwork.animated}")
            } else {
                Timber.d("🎵 Apple Music Artwork - ❌ No se encontró artwork")
            }
            artwork
        }.getOrNull()
    }

    /**
     * Obtiene artwork desde una URL de Apple Music
     * GET /api/v1/artwork/url?url=APPLE_MUSIC_URL
     */
    private suspend fun fetchArtworkByUrl(url: String): CanvasArtwork? {
        return runCatching {
            Timber.d("🎵 Apple Music Artwork - URL: $url")

            val response = client.get("$API_BASE_URL/artwork/url") {
                parameter("url", url)
            }

            if (response.status != HttpStatusCode.OK) {
                Timber.d("🎵 Apple Music Artwork - Error: ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            Timber.d("🎵 Apple Music Artwork - Response recibido por URL")

            val artwork = parseArtworkResponse(root)
            if (artwork != null) {
                Timber.d("🎵 Apple Music Artwork - ✅ Encontrado por URL: ${artwork.animated}")
            } else {
                Timber.d("🎵 Apple Music Artwork - ❌ No se encontró artwork por URL")
            }
            artwork
        }.getOrNull()
    }

    /**
     * Parsea la respuesta JSON del servicio
     *
     * La respuesta típica contiene:
     * {
     *   "name": "Album Name",
     *   "artistName": "Artist Name",
     *   "albumId": "1234567890",
     *   "albumName": "Album Name",
     *   "url": "https://.../master.m3u8",
     *   "tallUrl": "https://.../tall.m3u8",
     *   "static": "https://.../static.jpg"
     * }
     */
    private fun parseArtworkResponse(json: JsonObject): CanvasArtwork? {
        try {
            // Extraer datos principales
            val name = json["name"]?.jsonPrimitive?.contentOrNull
            val artist = json["artistName"]?.jsonPrimitive?.contentOrNull
            val albumId = json["albumId"]?.jsonPrimitive?.contentOrNull
            val albumName = json["albumName"]?.jsonPrimitive?.contentOrNull
                ?: json["album"]?.jsonPrimitive?.contentOrNull

            // Extraer URLs de videos - Prioridad: url (HLS) > tallUrl (vertical)
            val animatedUrl = json["url"]?.jsonPrimitive?.contentOrNull
                ?: json["hlsUrl"]?.jsonPrimitive?.contentOrNull
                ?: json["videoUrl"]?.jsonPrimitive?.contentOrNull

            val animatedVerticalUrl = json["tallUrl"]?.jsonPrimitive?.contentOrNull
                ?: json["verticalUrl"]?.jsonPrimitive?.contentOrNull
                ?: json["tall"]?.jsonPrimitive?.contentOrNull

            val staticUrl = json["static"]?.jsonPrimitive?.contentOrNull
                ?: json["staticUrl"]?.jsonPrimitive?.contentOrNull
                ?: json["image"]?.jsonPrimitive?.contentOrNull

            // Buscar en "data" si existe
            val data = json["data"]?.jsonObject
            val finalAnimatedUrl = animatedUrl
                ?: data?.get("url")?.jsonPrimitive?.contentOrNull
                ?: data?.get("hlsUrl")?.jsonPrimitive?.contentOrNull
            val finalTallUrl = animatedVerticalUrl
                ?: data?.get("tallUrl")?.jsonPrimitive?.contentOrNull
                ?: data?.get("verticalUrl")?.jsonPrimitive?.contentOrNull
            val finalStaticUrl = staticUrl
                ?: data?.get("static")?.jsonPrimitive?.contentOrNull
                ?: data?.get("image")?.jsonPrimitive?.contentOrNull

            return if (finalAnimatedUrl != null || finalTallUrl != null) {
                CanvasArtwork(
                    name = name ?: albumName,
                    artist = artist,
                    albumId = albumId,
                    albumName = albumName,
                    static = finalStaticUrl,
                    animated = finalAnimatedUrl,
                    animatedVertical = finalTallUrl,
                    videoUrl = finalAnimatedUrl,
                    videoUrlVertical = finalTallUrl
                )
            } else {
                Timber.d("🎵 Apple Music Artwork - No se encontraron URLs en la respuesta")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "🎵 Apple Music Artwork - Error parseando respuesta")
            return null
        }
    }

    private fun cacheKey(prefix: String, vararg parts: String) =
        "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
}