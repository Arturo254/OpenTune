/*
 * OpenTune Project (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.canvas

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CachedCanvas(
    val id: String,
    val artist: String,
    val album: String,
    val song: String?,
    val url: String,
    val type: String,
    val cachedAt: Long,
    val expiresAt: Long,
    val filePath: String
)

object CanvasCacheManager {

    private const val CACHE_DIR = "canvas_video_cache"
    private const val METADATA_FILE = "canvas_metadata.json"

    private var cacheDir: File? = null
    private var metadataFile: File? = null

    private val memoryCache = ConcurrentHashMap<String, CachedCanvas>()
    private val urlCache = ConcurrentHashMap<String, String>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir!!.exists()) {
            cacheDir!!.mkdirs()
        }
        metadataFile = File(cacheDir, METADATA_FILE)

        loadMetadata()
        isInitialized = true

        Timber.d("🎵 CanvasCache - Inicializado. Tamaño: ${memoryCache.size}")
    }

    private fun ensureInitialized(): Boolean {
        if (!isInitialized) {
            Timber.e("🎵 CanvasCache - No inicializado! Llama a init(context) primero.")
            return false
        }
        return true
    }

    suspend fun cacheCanvas(
        id: String,
        artist: String,
        album: String,
        song: String?,
        url: String,
        videoData: ByteArray
    ): Boolean {
        if (!ensureInitialized()) return false

        return withContext(Dispatchers.IO) {
            try {
                val dir = cacheDir ?: return@withContext false
                val fileName = "${id}.${getFileExtension(url)}"
                val videoFile = File(dir, fileName)

                videoFile.writeBytes(videoData)

                // ✅ SIN EXPIRACIÓN
                val expiresAt = Long.MAX_VALUE

                val cached = CachedCanvas(
                    id = id,
                    artist = artist,
                    album = album,
                    song = song,
                    url = url,
                    type = getFileExtension(url),
                    cachedAt = System.currentTimeMillis(),
                    expiresAt = expiresAt,
                    filePath = videoFile.absolutePath
                )

                memoryCache[id] = cached
                urlCache[url] = id

                saveMetadata()

                Timber.d("🎵 CanvasCache - Canvas guardado: $id (${videoData.size / 1024} KB)")
                Timber.d("🎵 CanvasCache - ⚠️ Nunca expirará (a menos que se limpie manualmente)")
                true
            } catch (e: Exception) {
                Timber.e(e, "🎵 CanvasCache - Error guardando canvas: $id")
                false
            }
        }
    }

    suspend fun getCachedCanvas(id: String): CachedCanvas? {
        if (!ensureInitialized()) return null

        return withContext(Dispatchers.IO) {
            val cached = memoryCache[id]

            if (cached != null) {
                val videoFile = File(cached.filePath)
                if (videoFile.exists()) {
                    // ✅ Nunca expira (o verifica si es Long.MAX_VALUE)
                    if (cached.expiresAt == Long.MAX_VALUE || System.currentTimeMillis() < cached.expiresAt) {
                        Timber.d("🎵 CanvasCache - Hit: $id")
                        return@withContext cached
                    } else {
                        Timber.d("🎵 CanvasCache - Expirado: $id")
                        removeFromCache(id)
                        return@withContext null
                    }
                } else {
                    removeFromCache(id)
                    return@withContext null
                }
            }

            Timber.d("🎵 CanvasCache - Miss: $id")
            null
        }
    }

    suspend fun getCachedCanvasByUrl(url: String): CachedCanvas? {
        if (!ensureInitialized()) return null

        return withContext(Dispatchers.IO) {
            val id = urlCache[url]
            if (id != null) {
                return@withContext getCachedCanvas(id)
            }
            null
        }
    }

    suspend fun isCached(id: String): Boolean {
        if (!ensureInitialized()) return false
        return withContext(Dispatchers.IO) {
            memoryCache.containsKey(id) && File(memoryCache[id]?.filePath ?: "").exists()
        }
    }

    suspend fun isCachedByUrl(url: String): Boolean {
        if (!ensureInitialized()) return false
        return withContext(Dispatchers.IO) {
            val id = urlCache[url]
            id != null && isCached(id)
        }
    }

    suspend fun removeFromCache(id: String) {
        if (!ensureInitialized()) return

        withContext(Dispatchers.IO) {
            val cached = memoryCache.remove(id)
            if (cached != null) {
                urlCache.remove(cached.url)
                val videoFile = File(cached.filePath)
                if (videoFile.exists()) {
                    videoFile.delete()
                }
                saveMetadata()
                Timber.d("🎵 CanvasCache - Eliminado: $id")
            }
        }
    }

    suspend fun clearCache() {
        if (!ensureInitialized()) return

        withContext(Dispatchers.IO) {
            memoryCache.clear()
            urlCache.clear()

            cacheDir?.listFiles()?.forEach { file ->
                if (file.name != METADATA_FILE) {
                    file.delete()
                }
            }

            metadataFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }

            saveMetadata()
            Timber.d("🎵 CanvasCache - Cache limpiada")
        }
    }

    suspend fun getCacheSize(): Long {
        if (!ensureInitialized()) return 0L

        return withContext(Dispatchers.IO) {
            var size = 0L
            cacheDir?.listFiles()?.forEach { file ->
                if (file.name != METADATA_FILE) {
                    size += file.length()
                }
            }
            size
        }
    }

    suspend fun getCacheCount(): Int {
        if (!ensureInitialized()) return 0

        return withContext(Dispatchers.IO) {
            memoryCache.size
        }
    }

    fun isInitialized(): Boolean = isInitialized

    private fun loadMetadata() {
        try {
            val file = metadataFile ?: return
            if (file.exists()) {
                val jsonString = file.readText()
                val list = json.decodeFromString<List<CachedCanvas>>(jsonString)

                val now = System.currentTimeMillis()
                val valid = list.filter {
                    it.expiresAt == Long.MAX_VALUE || it.expiresAt > now
                }

                valid.forEach { cached ->
                    val videoFile = File(cached.filePath)
                    if (videoFile.exists()) {
                        memoryCache[cached.id] = cached
                        urlCache[cached.url] = cached.id
                    }
                }

                Timber.d("🎵 CanvasCache - Cargados ${memoryCache.size} metadatos")

                if (valid.size != list.size) {
                    saveMetadata()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "🎵 CanvasCache - Error cargando metadatos")
        }
    }

    private fun saveMetadata() {
        try {
            val list = memoryCache.values.toList()
            val jsonString = json.encodeToString(list)
            metadataFile?.writeText(jsonString)
            Timber.d("🎵 CanvasCache - Guardados ${list.size} metadatos")
        } catch (e: Exception) {
            Timber.e(e, "🎵 CanvasCache - Error guardando metadatos")
        }
    }

    private fun getFileExtension(url: String): String {
        val lastDot = url.lastIndexOf('.')
        return if (lastDot > 0) {
            url.substring(lastDot + 1).takeIf { it.length <= 5 } ?: "mp4"
        } else {
            "mp4"
        }
    }
}