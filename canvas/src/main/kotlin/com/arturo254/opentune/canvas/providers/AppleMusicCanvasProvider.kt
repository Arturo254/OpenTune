/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Proveedor de canvas de Apple Music — usa el JWT público del web player.
 * Retorna streams HLS (.m3u8).
 *
 * Estrategias de extracción (en orden):
 *  1. editorialVideo en la respuesta de búsqueda directa
 *  2. Lookup completo del álbum con ?extend=editorialVideo
 */

package com.arturo254.opentune.canvas.providers

import com.arturo254.opentune.canvas.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object AppleMusicCanvasProvider {

    // JWT público de solo lectura del web player de Apple Music
    private const val APPLE_MUSIC_TOKEN =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IldlYlBsYXlLaWQifQ" +
                ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzc0NDU2MzgyLCJleHAiOjE3ODE3" +
                "MTM5ODIsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
                ".4n8qYF4qa18sL1E0G9A3qX35cD8wQ-IJcS9Bh8ZT8JV_yLBtVq46B-9-2ZS3EvWHuw3yK9BYFYAhAdTaDm38vQ"

    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
                register(ContentType.Text.JavaScript, KotlinxSerializationConverter(json))
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

    private data class CacheEntry(val value: CanvasArtwork?, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 horas

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("song", song, artist, album ?: "", storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchAndFetchMotion(song, artist, album, storefront, "songs")
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    suspend fun getByAlbumArtist(
        album: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val key = cacheKey("album", album, artist, storefront)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val result = searchAndFetchMotion(album, artist, album, storefront, "albums")
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    // -------------------------------------------------------------------------
    // Implementación interna
    // -------------------------------------------------------------------------

    private suspend fun searchAndFetchMotion(
        term: String,
        artist: String,
        album: String?,
        storefront: String,
        type: String,
    ): CanvasArtwork? = runCatching {

        var query = if (term.contains(artist, ignoreCase = true)) term else "$artist $term"
        if (!album.isNullOrBlank() && !query.contains(album, ignoreCase = true)) {
            query = "$query $album"
        }

        val response = client.get("$AMP_BASE_URL/v1/catalog/$storefront/search") {
            header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            parameter("term", query)
            parameter("types", type)
            parameter("limit", "10")
            parameter("extend", "editorialVideo")
            parameter("include", "albums")
        }
        if (response.status != HttpStatusCode.OK) return@runCatching null

        val root = response.body<JsonObject>()
        val results = root["results"]?.jsonObject
            ?.get(type)?.jsonObject
            ?.get("data")?.jsonArray ?: return@runCatching null

        // Puntuar y filtrar resultados
        val scoredResults = results.mapNotNull { item ->
            val obj = item.jsonObject
            val attributes = obj["attributes"]?.jsonObject ?: return@mapNotNull null
            val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
            val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""
            val resultCollectionName =
                attributes["collectionName"]?.jsonPrimitive?.contentOrNull ?: ""

            // Blacklist: playlists / sets / curaciones editoriales
            val nameLower = resultName.lowercase(Locale.ROOT)
            val collLower = resultCollectionName.lowercase(Locale.ROOT)
            val blacklisted = listOf(
                "playlist", "set list", "essentials", "dj mix", "mixed",
                "apple music", "today's hits", "session"
            ).any { nameLower.contains(it) || collLower.contains(it) }
            if (blacklisted) return@mapNotNull null

            if (!artistMatches(artist, resultArtistName)) return@mapNotNull null

            var score = if (resultArtistName.equals(artist, ignoreCase = true)) 10 else 5

            val nameMatch = resultName.equals(term, ignoreCase = true)
            val nameFuzzy = resultName.contains(term, ignoreCase = true) || term.contains(
                resultName,
                ignoreCase = true
            )
            score += when {
                nameMatch -> 15
                nameFuzzy -> 7
                else -> -10
            }

            listOf("deluxe", "expanded", "remastered", "remix", "version", "edit", "mix", "bonus")
                .forEach { word ->
                    val inTerm = term.contains(word, ignoreCase = true)
                    val inResult = resultName.contains(word, ignoreCase = true)
                    if (inTerm && inResult) score += 5
                    else if (!inTerm && inResult) score -= 3
                }

            if (!album.isNullOrBlank() && resultCollectionName.isNotBlank()) {
                score += when {
                    resultCollectionName.equals(album, ignoreCase = true) -> 20
                    resultCollectionName.contains(album, ignoreCase = true) ||
                            album.contains(resultCollectionName, ignoreCase = true) -> 10

                    else -> 0
                }
            }

            score to item
        }.sortedByDescending { it.first }

        for ((score, item) in scoredResults) {
            if (score < 12) continue

            val obj = item.jsonObject
            val attributes = obj["attributes"]?.jsonObject ?: continue
            val resultArtistName = attributes["artistName"]?.jsonPrimitive?.contentOrNull ?: ""
            val itemType = obj["type"]?.jsonPrimitive?.contentOrNull

            // Resolver albumId
            var targetAlbumId: String? = null
            if (itemType == "songs") {
                targetAlbumId = obj["relationships"]?.jsonObject
                    ?.get("albums")?.jsonObject
                    ?.get("data")?.jsonArray
                    ?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                    ?: attributes["collectionId"]?.jsonPrimitive?.contentOrNull

                // Fallback: parsear desde URL
                if (targetAlbumId == null) {
                    attributes["url"]?.jsonPrimitive?.contentOrNull?.let { url ->
                        val id = url.substringAfter("/album/", "")
                            .substringBefore("?")
                            .substringAfterLast("/", "")
                        if (id.isNotBlank() && id.all { it.isDigit() }) targetAlbumId = id
                    }
                }
            } else if (itemType == "albums") {
                targetAlbumId = obj["id"]?.jsonPrimitive?.contentOrNull
            }

            if (targetAlbumId == null || targetAlbumId!!.startsWith("pl.")) continue

            // ¿Tiene editorialVideo en la búsqueda misma?
            attributes["editorialVideo"]?.jsonObject?.let { ev ->
                extractEditorialVideoUrl(ev)?.let { hlsUrl ->
                    val nameAttr = attributes["name"]?.jsonPrimitive?.contentOrNull
                    val collAttr = attributes["collectionName"]?.jsonPrimitive?.contentOrNull
                    val resolvedAlbum = if (itemType == "songs") collAttr else nameAttr
                    return@runCatching CanvasArtwork(
                        name = nameAttr,
                        artist = resultArtistName,
                        albumId = targetAlbumId,
                        albumName = resolvedAlbum,
                        animated = hlsUrl,
                    )
                }
            }

            // Lookup completo del álbum
            val fetched = fetchMotionArtwork(
                albumId = targetAlbumId!!,
                storefront = storefront,
                fallbackArtist = resultArtistName,
                titleOverride = if (itemType == "songs") attributes["name"]?.jsonPrimitive?.contentOrNull else null,
                artistOverride = if (itemType == "songs") resultArtistName else null,
            )
            if (fetched != null) return@runCatching fetched
        }
        null
    }.onFailure { if (it is CancellationException) throw it }.getOrNull()

    private suspend fun fetchMotionArtwork(
        albumId: String,
        storefront: String,
        fallbackArtist: String?,
        titleOverride: String? = null,
        artistOverride: String? = null,
    ): CanvasArtwork? {
        if (albumId.startsWith("pl.")) return null

        return runCatching {
            val response = client.get("$AMP_BASE_URL/v1/catalog/$storefront/albums/$albumId") {
                header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("extend", "editorialVideo")
                parameter("include", "tracks")
            }
            if (response.status != HttpStatusCode.OK) return@runCatching null

            val root = response.body<JsonObject>()
            val data = root["data"]?.jsonArray
            if (data.isNullOrEmpty()) return@runCatching null

            val albumObj = data.firstOrNull()?.jsonObject ?: return@runCatching null
            val attributes = albumObj["attributes"]?.jsonObject
            val albumName = attributes?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
            val artistName =
                attributes?.get("artistName")?.jsonPrimitive?.contentOrNull ?: fallbackArtist

            // Blacklist
            val nameLower = albumName.lowercase(Locale.ROOT)
            val blacklisted = listOf(
                "playlist", "set list", "essentials", "dj mix", "mixed",
                "apple music", "today's hits", "session"
            ).any { nameLower.contains(it) }
            if (blacklisted) return@runCatching null

            val finalTitle = titleOverride ?: albumName
            val finalArtist = artistOverride ?: artistName

            attributes?.get("editorialVideo")?.jsonObject?.let { ev ->
                extractEditorialVideoUrl(ev)?.let { hlsUrl ->
                    return@runCatching CanvasArtwork(
                        name = finalTitle,
                        artist = finalArtist,
                        albumId = albumId,
                        albumName = albumName,
                        animated = hlsUrl,
                    )
                }
            }
            null
        }.onFailure { if (it is CancellationException) throw it }.getOrNull()
    }

    private fun extractEditorialVideoUrl(ev: JsonObject): String? {
        val assets = listOf(
            ev["motionDetailRaw"]?.jsonObject,
            ev["motionDetailSquare"]?.jsonObject,
            ev["motionDetailTall"]?.jsonObject,
            ev["motionDetailStatic"]?.jsonObject,
        ).filterNotNull()

        for (asset in assets) {
            val url = asset["video"]?.jsonPrimitive?.contentOrNull
                ?: asset["videoUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["hlsUrl"]?.jsonPrimitive?.contentOrNull
                ?: asset["url"]?.jsonPrimitive?.contentOrNull
            if (!url.isNullOrBlank()) return url
        }
        return null
    }

    private fun cacheKey(prefix: String, vararg parts: String) =
        "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }

    private fun artistMatches(requested: String, returned: String): Boolean {
        val delimiters = Regex(
            "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
            RegexOption.IGNORE_CASE,
        )
        val req = requested.split(delimiters).map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
        val ret = returned.split(delimiters).map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
        return req.any { r -> ret.any { it.contains(r) || r.contains(it) } }
    }
}
