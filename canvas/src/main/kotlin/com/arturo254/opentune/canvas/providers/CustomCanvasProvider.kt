/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Proveedor de canvas personalizado - Usa la API de OpenTune Canvas Studio
 * corriendo en Cloudflare Workers + D1 + R2.
 *
 * Endpoint: GET {API_BASE_URL}?action=list
 * Devuelve el catálogo completo:
 *   { "success": true, "data": { "total": N, "canvases": [ {id, artist, album, song, url, ...}, ... ] } }
 * El matching por artista/álbum/canción se hace en cliente (normalizando
 * acentos y mayúsculas), porque el servidor no filtra.
 *
 * "song" en cada entrada es OPCIONAL: si viene vacío, el canvas aplica a
 * TODO el álbum (ej: subís un canvas para "111XPANTIA" de Fuerza Regida
 * sin especificar canción, y se muestra para cualquier track de ese álbum).
 */

package com.arturo254.opentune.canvas.providers

import com.arturo254.opentune.canvas.models.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object CustomCanvasProvider {

    // ✅ URL de tu Worker de Cloudflare
    private const val API_BASE_URL =
        "https://opentune-canvas-api.cervantesarturo777.workers.dev"
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    // Cliente HTTP
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(jsonParser) }
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

    // Representación interna de una entrada del catálogo
    private data class CanvasEntry(
        val artist: String,
        val album: String,
        val song: String,
        val url: String,
    )

    // Cache del catálogo completo (una sola entrada, TTL corto porque puede haber uploads nuevos)
    private data class ListCacheEntry(val entries: List<CanvasEntry>, val expiresAtMs: Long)

    private var listCache: ListCacheEntry? = null
    private val listCacheLock = Any()
    private const val LIST_CACHE_TTL_MS = 1000L * 60 * 10 // 10 minutos

    // Cache de resultados de búsqueda ya resueltos
    private data class CacheEntry(val value: CanvasArtwork?, val expiresAtMs: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 horas

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

    // -------------------------------------------------------------------------
    // Implementación interna
    // -------------------------------------------------------------------------

    private suspend fun searchCanvas(
        artist: String,
        album: String,
        song: String? = null,
    ): CanvasArtwork? {
        val entries = getCatalog() ?: return null
        if (entries.isEmpty()) return null

        val normArtist = normalize(artist)
        val normAlbum = normalize(album)
        val normSong = song?.let { normalize(it) }

        // Filtro base: artista + álbum deben coincidir.
        // Si "album" viene vacío (no se pudo resolver el álbum real), no se
        // fuerza ningún match para evitar falsos positivos.
        val candidates = entries.filter { entry ->
            artistMatches(normArtist, normalize(entry.artist)) &&
                    albumMatches(normAlbum, normalize(entry.album))
        }

        if (candidates.isEmpty()) {
            Timber.d("🎵 CustomCanvas - Sin coincidencias artist/album")
            return null
        }

        // Prioriza la entrada que además matchea la canción específica.
        // Si ninguna entrada tiene "song" o no matchea, cae a la primera
        // coincidencia de artista+álbum (canvas a nivel de álbum completo).
        val best = candidates.firstOrNull { entry ->
            !normSong.isNullOrBlank() && entry.song.isNotBlank() &&
                    (normalize(entry.song) == normSong ||
                            normalize(entry.song).contains(normSong) ||
                            normSong.contains(normalize(entry.song)))
        } ?: candidates.first()

        val matchType =
            if (best.song.isBlank()) "álbum (sin canción específica)" else "canción exacta"
        Timber.d("🎵 CustomCanvas - ✅ Encontrado por $matchType: ${best.url} (artista=${best.artist}, album=${best.album})")

        return CanvasArtwork(
            name = best.song.takeIf { it.isNotBlank() },
            artist = best.artist,
            albumName = best.album,
            animated = best.url,
            videoUrl = best.url,
        )
    }

    /**
     * Trae el catálogo completo (con cache corta) y lo mapea a [CanvasEntry].
     * Usa ?action=list contra el Worker.
     */
    private suspend fun getCatalog(): List<CanvasEntry>? {
        synchronized(listCacheLock) {
            listCache?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let {
                return it.entries
            }
        }

        val fetched = try {
            val response = client.get(API_BASE_URL) {
                parameter("action", "list")
            }

            if (response.status != HttpStatusCode.OK) {
                Timber.d("🎵 CustomCanvas - Error HTTP: ${response.status}")
                null
            } else {
                val rawBody = response.bodyAsText()
                val trimmed = rawBody.trimStart()

                // Blindaje: si por algún motivo no llega JSON (ej. mantenimiento
                // del Worker), evita reventar con excepción de parseo.
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    Timber.d(
                        "🎵 CustomCanvas - ⚠️ Respuesta no es JSON. Primeros 200 chars: ${
                            rawBody.take(
                                200
                            )
                        }"
                    )
                    null
                } else {
                    val root = jsonParser.parseToJsonElement(rawBody).jsonObject

                    val success =
                        root["success"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
                    if (!success) {
                        Timber.d("🎵 CustomCanvas - success=false en la respuesta")
                        null
                    } else {
                        val dataWrapper = root["data"]?.jsonObject
                        val canvasesArray = dataWrapper?.get("canvases")?.jsonArray

                        if (canvasesArray == null) {
                            Timber.d("🎵 CustomCanvas - ⚠️ No existe 'data.canvases'. Keys: ${dataWrapper?.keys}")
                            null
                        } else {
                            canvasesArray.mapNotNull { item ->
                                val obj = item.jsonObject
                                val url = obj["url"]?.jsonPrimitive?.contentOrNull
                                val entryArtist = obj["artist"]?.jsonPrimitive?.contentOrNull
                                if (url == null || entryArtist == null) {
                                    Timber.d("🎵 CustomCanvas - ⚠️ Entrada sin url/artist descartada: $obj")
                                    null
                                } else {
                                    CanvasEntry(
                                        artist = entryArtist,
                                        album = obj["album"]?.jsonPrimitive?.contentOrNull ?: "",
                                        song = obj["song"]?.jsonPrimitive?.contentOrNull ?: "",
                                        url = url,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "🎵 CustomCanvas - Excepción consultando catálogo")
            null
        }

        if (fetched != null) {
            synchronized(listCacheLock) {
                listCache = ListCacheEntry(fetched, System.currentTimeMillis() + LIST_CACHE_TTL_MS)
            }
        }
        Timber.d("🎵 CustomCanvas - Catálogo cargado: ${fetched?.size ?: -1} entradas")
        return fetched
    }

    // -------------------------------------------------------------------------
    // Normalización / matching
    // -------------------------------------------------------------------------

    /** Quita acentos, pasa a minúsculas y recorta espacios: "DINASTÍA" -> "dinastia" */
    private fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutAccents = decomposed.replace(Regex("\\p{Mn}+"), "")
        return withoutAccents.lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
    }

    private val artistSplitRegex = Regex(
        "(?:\\s*,\\s*|\\s*&\\s*|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
        RegexOption.IGNORE_CASE,
    )

    /** Compara artistas token por token (soporta "Tito Double P, Peso Pluma" vs "Peso Pluma") */
    private fun artistMatches(normRequested: String, normEntry: String): Boolean {
        if (normRequested == normEntry) return true
        val requestedTokens =
            normRequested.split(artistSplitRegex).map { it.trim() }.filter { it.isNotBlank() }
        val entryTokens =
            normEntry.split(artistSplitRegex).map { it.trim() }.filter { it.isNotBlank() }
        return requestedTokens.any { req ->
            entryTokens.any { ent -> ent.contains(req) || req.contains(ent) }
        }
    }

    /**
     * Compara álbumes. Si "normRequested" viene vacío (no se pudo resolver el
     * álbum real desde el reproductor), solo matchea si la entrada tampoco
     * tiene álbum — evita adivinar y generar falsos positivos.
     */
    private fun albumMatches(normRequested: String, normEntry: String): Boolean {
        if (normEntry.isBlank() || normRequested.isBlank()) return normEntry.isBlank() == normRequested.isBlank()
        return normEntry == normRequested || normEntry.contains(normRequested) || normRequested.contains(
            normEntry
        )
    }

    private fun cacheKey(artist: String, album: String, song: String?): String =
        "custom|${artist.lowercase()}|${album.lowercase()}|${song?.lowercase() ?: ""}"
}