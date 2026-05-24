/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.math.max

private const val PlayerArtworkHighResPx = 1080

private val wHPathRegex = Regex("w\\d+-h\\d+")
private val wHParamRegex = Regex("=w(\\d+)-h(\\d+)")
private val sParamRegex = Regex("=s(\\d+)")
private val brokenSAppendRegex = Regex("-s\\d+")

// Patrón para i.ytimg.com
private val ytimgViRegex = Regex("/vi/([a-zA-Z0-9_-]+)/")
private val ytimgQualityRegex = Regex("/(maxresdefault|sddefault|hqdefault|mqdefault|default)\\.jpg$")

// ──────────────────────────────────────────────────────────────────────────────
//  PRECARGA MANUAL - CACHÉ EN MEMORIA Y DISCO
// ──────────────────────────────────────────────────────────────────────────────

object ThumbnailCache {
    // Caché en memoria (máximo 30 imágenes)
    private val memoryCache = mutableMapOf<String, Bitmap>()
    private var diskCacheDir: File? = null

    // OkHttpClient para descargas
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun init(context: android.content.Context) {
        diskCacheDir = File(context.cacheDir, "thumbnails_cache")
        if (!diskCacheDir!!.exists()) {
            diskCacheDir!!.mkdirs()
        }
        cleanOldCache()
    }

    private fun cleanOldCache() {
        val dir = diskCacheDir ?: return
        val files = dir.listFiles() ?: return
        val maxSize = 50L * 1024 * 1024 // 50MB máximo
        var totalSize = 0L
        val fileSizes = mutableListOf<Pair<File, Long>>()

        files.forEach { file ->
            val size = file.length()
            totalSize += size
            fileSizes.add(file to size)
        }

        if (totalSize > maxSize) {
            fileSizes.sortedBy { it.first.lastModified() }.forEach { (file, size) ->
                if (totalSize <= maxSize) return
                file.delete()
                totalSize -= size
            }
        }
    }

    private fun getCacheKey(url: String, width: Int, height: Int): String {
        return "${url.hashCode()}_${width}x${height}"
    }

    fun getFromMemory(url: String, width: Int, height: Int): Bitmap? {
        return memoryCache[getCacheKey(url, width, height)]
    }

    fun getFromDisk(url: String, width: Int, height: Int): Bitmap? {
        val dir = diskCacheDir ?: return null
        val file = File(dir, "${getCacheKey(url, width, height)}.jpg")
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    fun save(url: String, width: Int, height: Int, bitmap: Bitmap) {
        val key = getCacheKey(url, width, height)

        // Guardar en memoria
        if (memoryCache.size > 30) {
            val toRemove = memoryCache.keys.firstOrNull()
            toRemove?.let { memoryCache.remove(it) }
        }
        memoryCache[key] = bitmap

        // Guardar en disco
        val dir = diskCacheDir ?: return
        val file = File(dir, "$key.jpg")
        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }
        } catch (e: Exception) {
            // Ignorar errores de guardado
        }
    }

    fun clear() {
        memoryCache.clear()
        diskCacheDir?.listFiles()?.forEach { it.delete() }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  FUNCIONES PÚBLICAS
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Inicializa el sistema de caché de thumbnails
 * Llamar desde Application o al inicio de la app
 */
fun initThumbnailCache(context: android.content.Context) {
    ThumbnailCache.init(context)
}

/**
 * Obtiene una thumbnail en alta calidad con precarga y caché
 * @param url URL original de la thumbnail
 * @param videoId ID del video (opcional, se extrae automáticamente si no se proporciona)
 * @param preferredWidth Ancho deseado (default 1080)
 * @param preferredHeight Alto deseado (default 1080)
 * @return Bitmap de alta calidad o null si falla
 */
suspend fun getHighQualityThumbnail(
    url: String?,
    videoId: String? = null,
    preferredWidth: Int = 1080,
    preferredHeight: Int = 1080
): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isNullOrBlank()) return@withContext null

    // 1. Verificar caché en memoria
    ThumbnailCache.getFromMemory(url, preferredWidth, preferredHeight)?.let {
        return@withContext it
    }

    // 2. Verificar caché en disco
    ThumbnailCache.getFromDisk(url, preferredWidth, preferredHeight)?.let { bitmap ->
        ThumbnailCache.save(url, preferredWidth, preferredHeight, bitmap)
        return@withContext bitmap
    }

    // 3. Intentar descargar en alta calidad
    try {
        val highQualityUrl = buildHighQualityUrl(url, videoId, preferredWidth)
        val bitmap = downloadBitmap(highQualityUrl)

        if (bitmap != null) {
            val resized = resizeBitmapIfNeeded(bitmap, preferredWidth, preferredHeight)
            ThumbnailCache.save(url, preferredWidth, preferredHeight, resized)
            return@withContext resized
        }
    } catch (e: Exception) {
        // Fallback al método original
    }

    // 4. Fallback: usar resize() normal
    return@withContext null
}

/**
 * Versión de highRes mejorada que intenta múltiples calidades
 * Esta es la función que ya existe, pero mejorada
 */
fun String.highRes(): String = resize(PlayerArtworkHighResPx, PlayerArtworkHighResPx)

/**
 * Función resize mejorada con soporte para i.ytimg.com
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    val isGoogleCdn = contains("googleusercontent.com") || contains("ggpht.com")
    val isYtimg = contains("i.ytimg.com")

    // ── Google CDN (ggpht.com / googleusercontent.com) ──
    if (isGoogleCdn) {
        val w = width ?: height!!
        val h = height ?: width!!

        if (wHPathRegex.containsMatchIn(this)) {
            return replace(wHPathRegex, "w$w-h$h")
        }

        wHParamRegex.find(this)?.let {
            return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
        }

        sParamRegex.find(this)?.let { match ->
            val before = substring(0, match.range.first)
            val after = substring(match.range.last + 1)
            return "${before}=s${maxOf(w, h)}${after.replace(brokenSAppendRegex, "")}"
        }

        return this
    }

    // ── i.ytimg.com MEJORADO ─────────────────────────────────────
    if (isYtimg) {
        val targetSize = maxOf(width ?: 0, height ?: 0, PlayerArtworkHighResPx)

        // Extraer el ID del video
        val videoIdMatch = ytimgViRegex.find(this)
        if (videoIdMatch != null) {
            val videoId = videoIdMatch.groupValues[1]

            // Determinar la mejor calidad disponible
            val quality = when {
                targetSize >= 1080 -> "maxresdefault"  // 1920x1080
                targetSize >= 480 -> "sddefault"      // 640x480
                targetSize >= 320 -> "hqdefault"      // 480x360
                targetSize >= 180 -> "mqdefault"      // 320x180
                else -> "default"                      // 120x90
            }

            return "https://i.ytimg.com/vi/$videoId/$quality.jpg"
        }

        // Fallback: intentar reemplazar la calidad existente
        return ytimgQualityRegex.replace(this) { matchResult ->
            val quality = when {
                targetSize >= 1080 -> "maxresdefault.jpg"
                targetSize >= 480 -> "sddefault.jpg"
                targetSize >= 320 -> "hqdefault.jpg"
                targetSize >= 180 -> "mqdefault.jpg"
                else -> "default.jpg"
            }
            "/$quality"
        }
    }

    return this
}

// ──────────────────────────────────────────────────────────────────────────────
//  FUNCIONES PRIVADAS DE PRECARGA
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Construye URL de máxima calidad
 */
private fun buildHighQualityUrl(originalUrl: String, videoId: String?, preferredSize: Int): String {
    // Si es i.ytimg.com, construir URL de máxima calidad
    if (originalUrl.contains("i.ytimg.com")) {
        val extractedVideoId = videoId ?: extractVideoIdFromUrl(originalUrl)
        if (extractedVideoId != null) {
            return when {
                preferredSize >= 1080 -> "https://i.ytimg.com/vi/$extractedVideoId/maxresdefault.jpg"
                preferredSize >= 640 -> "https://i.ytimg.com/vi/$extractedVideoId/sddefault.jpg"
                else -> "https://i.ytimg.com/vi/$extractedVideoId/hqdefault.jpg"
            }
        }
    }

    // Si es ggpht.com, usar resize normal
    if (originalUrl.contains("ggpht.com")) {
        return originalUrl.resize(preferredSize, preferredSize)
    }

    return originalUrl
}

/**
 * Descarga bitmap desde URL
 */
private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"

        // Verificar si existe el recurso
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            return@withContext null
        }

        val inputStream = connection.inputStream
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        connection.disconnect()

        return@withContext bitmap
    } catch (e: Exception) {
        return@withContext null
    }
}

/**
 * Redimensiona bitmap si es necesario
 */
private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= maxWidth && height <= maxHeight) {
        return bitmap
    }

    val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

/**
 * Extrae videoId de URL de YouTube
 */
private fun extractVideoIdFromUrl(url: String): String? {
    val patterns = listOf(
        Regex("/vi/([a-zA-Z0-9_-]+)/"),
        Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]+)")
    )

    patterns.forEach { pattern ->
        pattern.find(url)?.let {
            return it.groupValues[1]
        }
    }

    return null
}

/**
 * Limpia toda la caché de thumbnails
 */
fun clearThumbnailCache() {
    ThumbnailCache.clear()
}