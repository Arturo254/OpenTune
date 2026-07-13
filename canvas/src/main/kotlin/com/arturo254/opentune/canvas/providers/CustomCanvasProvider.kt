/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Proveedor de canvas personalizado - Usa la API de OpenTune Canvas Studio
 * URL: https://opentune-canvas.vercel.app/api
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

object CustomCanvasProvider {

    // URL de tu API en Vercel
    private const val API_BASE_URL = "https://opentune-canvas.vercel.app/api"

    // Cliente HTTP
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            install(ContentEncoding) { gzip(); deflate() }
            install(HttpCache)
            expectSuccess = false
        }
    }

    // Cache en memoria
    private data class CacheEntry(val value: CanvasArtwork?, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 horas

    /**
     * Busca un Canvas en tu API personalizada
     * @param song Nombre de la canción (opcional)
     * @param artist Nombre del artista (obligatorio)
     * @param album Nombre del álbum (obligatorio)
     */
    suspend fun getBySongArtist(
        song: String? = null,
        artist: String,
        album: String,
    ): CanvasArtwork? {
        val key = cacheKey(artist, album, song)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
            Timber.d("🎵 CustomCanvas - Cache hit")
            return it.value
        }

        Timber.d("🎵 CustomCanvas - Buscando: artist=$artist, album=$album, song=$song")

        val result = searchCanvas(artist, album, song)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    /**
     * Busca Canvas por Artista + Álbum (sin canción)
     */
    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
    ): CanvasArtwork? {
        return getBySongArtist(
            song = null,
            artist = artist,
            album = album
        )
    }

    /**
     * Realiza la búsqueda en la API
     */
    private suspend fun searchCanvas(
        artist: String,
        album: String,
        song: String? = null
    ): CanvasArtwork? {
        return runCatching {
            Timber.d("🎵 CustomCanvas - Consultando API: $API_BASE_URL/search")

            val response = client.get("$API_BASE_URL/search") {
                parameter("artist", artist)
                parameter("album", album)
                song?.let { parameter("song", it) }
            }

            if (response.status != HttpStatusCode.OK) {
                Timber.d("🎵 CustomCanvas - Error: ${response.status}")
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val found = root["found"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false

            if (!found) {
                Timber.d("🎵 CustomCanvas - No encontrado en la API")
                return@runCatching null
            }

            val data = root["data"]?.jsonObject ?: return@runCatching null
            val url = data["url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
            val artistName = data["artist"]?.jsonPrimitive?.contentOrNull ?: artist
            val albumName = data["album"]?.jsonPrimitive?.contentOrNull ?: album
            val songName = data["song"]?.jsonPrimitive?.contentOrNull ?: song

            Timber.d("🎵 CustomCanvas - ✅ Encontrado: $url")

            return CanvasArtwork(
                name = songName,
                artist = artistName,
                albumName = albumName,
                animated = url,
                videoUrl = url,
                // Si la API devuelve formato vertical, se puede mapear aquí
                // animatedVertical = data["tallUrl"]?.jsonPrimitive?.contentOrNull
            )
        }.getOrNull()
    }

    private fun cacheKey(artist: String, album: String, song: String?): String =
        "custom|${artist.lowercase()}|${album.lowercase()}|${song?.lowercase() ?: ""}"
}