/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Proveedor de fondo animado para la pantalla de artista (Apple Music editorial video).
 */

package com.arturo254.opentune.canvas.providers

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object AppleMusicArtistBackgroundProvider {

    // JWT público de solo lectura del web player de Apple Music (By Vivi Music)
    private const val APPLE_MUSIC_TOKEN =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ" +
                ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzgxMDMyODU1LCJleHAiOjE3ODQw" +
                "NTY4NTUsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
                ".fiMFcJWkfSlxKP9NVA0UW9CbItD1Rge0SISuepz203XcpU762OqdCpU9M-YkmtKkjRmaIWtjsfGgqZPrlMonpA"

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

    private data class CacheEntry(val videoUrl: String?, val expiresAtMs: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 horas

    suspend fun getByArtistName(
        artistName: String,
        storefront: String = "us",
    ): String? {
        if (artistName.isBlank()) return null
        val key = "artist|${artistName.trim().lowercase(Locale.ROOT)}|$storefront"
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }
            ?.let { return it.videoUrl }

        val result = searchAndFetchArtistMotion(artistName, storefront)
        cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        return result
    }

    private suspend fun searchAndFetchArtistMotion(
        artistName: String,
        storefront: String,
    ): String? = runCatching {
        val response = client.get("$AMP_BASE_URL/v1/catalog/$storefront/search") {
            header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            parameter("term", artistName)
            parameter("types", "artists")
            parameter("limit", "3")
        }
        if (response.status != HttpStatusCode.OK) return@runCatching null

        val root = response.body<JsonObject>()
        val results = root["results"]?.jsonObject
            ?.get("artists")?.jsonObject
            ?.get("data")?.jsonArray ?: return@runCatching null

        val scored = results.mapNotNull { item ->
            val obj = item.jsonObject
            val attrs = obj["attributes"]?.jsonObject ?: return@mapNotNull null
            val resultName = attrs["name"]?.jsonPrimitive?.contentOrNull ?: ""

            if (!resultName.contains(artistName, ignoreCase = true) &&
                !artistName.contains(resultName, ignoreCase = true)
            ) return@mapNotNull null

            val score = if (resultName.equals(artistName, ignoreCase = true)) 10 else 5
            score to obj
        }.sortedByDescending { it.first }

        for ((score, obj) in scored) {
            if (score < 4) continue
            val artistId = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
            val url = fetchArtistMotionByAppleId(artistId, storefront)
            if (url != null) return@runCatching url
        }
        null
    }.getOrNull()

    private suspend fun fetchArtistMotionByAppleId(
        artistId: String,
        storefront: String,
    ): String? = runCatching {
        val response = client.get("$AMP_BASE_URL/v1/catalog/$storefront/artists/$artistId") {
            header("Authorization", "Bearer $APPLE_MUSIC_TOKEN")
            header("Origin", "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            parameter("extend", "editorialVideo,editorialArtwork")
        }
        if (response.status != HttpStatusCode.OK) return@runCatching null

        val root = response.body<JsonObject>()
        val data = root["data"]?.jsonArray
        if (data.isNullOrEmpty()) return@runCatching null

        val artistObj = data.firstOrNull()?.jsonObject ?: return@runCatching null
        val attributes = artistObj["attributes"]?.jsonObject

        // editorialVideo primero
        attributes?.get("editorialVideo")?.jsonObject?.let { ev ->
            extractEditorialVideoUrl(ev)?.let { return@runCatching it }
        }
        // Fallback a editorialArtwork
        attributes?.get("editorialArtwork")?.jsonObject?.let { ea ->
            extractEditorialVideoUrl(ea)?.let { return@runCatching it }
        }
        null
    }.getOrNull()

    private fun extractEditorialVideoUrl(data: JsonObject): String? {
        val preferredKeys = listOf(
            "motionDetailRaw", "motionDetailTall", "motionDetailSquare",
            "motionSquareVideo1x1", "motionTallVideo3x4"
        )
        for (key in preferredKeys) {
            val url = data[key]?.jsonObject?.get("video")?.jsonPrimitive?.contentOrNull
            if (!url.isNullOrBlank()) return url
        }
        // Fallback genérico
        for ((_, value) in data) {
            val url = (value as? JsonObject)?.get("video")?.jsonPrimitive?.contentOrNull
            if (!url.isNullOrBlank()) return url
        }
        return null
    }
}
