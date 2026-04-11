/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.viewmodels

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.arturo254.opentune.R
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// Data class para el estado de subida a la nube
data class CloudUploadState(
    val isEnabled: Boolean = false,
    val isUploading: Boolean = false,
    val lastUploadUrl: String? = null,
    val lastError: String? = null,
    val uploadProgress: Int = 0
)

// Servicio de Filebin (sin autenticación)
class FilebinService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    
    companion object {
        private const val FILEBIN_API_URL = "https://filebin.net"
    }
    
    /**
     * Sube un archivo a Filebin y retorna la URL pública
     * @param file Archivo a subir
     * @param onProgress Callback para actualizar el progreso (0-100)
     * @return Result con la URL del archivo o el error
     */
    suspend fun uploadFile(
        file: File,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Crear un bin name único basado en timestamp y nombre de app
            val binName = "opentune_backup_${System.currentTimeMillis()}"
            val fileName = file.name
            
            // Construir la request multipart
            val mediaType = "application/octet-stream".toMediaType()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    object : okhttp3.RequestBody() {
                        override fun contentType(): okhttp3.MediaType? = mediaType
                        
                        override fun contentLength(): Long = file.length()
                        
                        override fun writeTo(sink: okio.BufferedSink) {
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            file.inputStream().use { input ->
                                var bytesRead: Int
                                var totalBytesRead = 0L
                                val fileSize = file.length()
                                
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    sink.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    if (fileSize > 0) {
                                        val progress = (totalBytesRead * 100 / fileSize).toInt()
                                        onProgress(progress)
                                    }
                                }
                            }
                        }
                    }
                )
                .build()
            
            val request = Request.Builder()
                .url("$FILEBIN_API_URL/$binName/$fileName")
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Filebin error: ${response.code} - ${response.message}")
                )
            }
            
            response.body?.string()?.let { responseBody ->
                // Parsear la respuesta JSON para obtener la URL
                val json = JSONObject(responseBody)
                val url = json.optString("url")
                
                if (url.isNotEmpty()) {
                    Result.success(url)
                } else {
                    // Si no viene URL en la respuesta, construirla manualmente
                    Result.success("$FILEBIN_API_URL/$binName/$fileName")
                }
            } ?: Result.failure(Exception("Empty response from Filebin"))
            
        } catch (e: Exception) {
            reportException(e)
            Result.failure(e)
        }
    }
    
    /**
     * Verifica si Filebin está disponible
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$FILEBIN_API_URL")
                .head()
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

// Extensión del ViewModel para manejar la subida a la nube
suspend fun BackupRestoreViewModel.uploadBackupToCloud(
    context: Context,
    backupFile: File,
    onProgress: (Int) -> Unit = {}
): Result<String> {
    val filebinService = FilebinService()
    return filebinService.uploadFile(backupFile, onProgress)
}

// Clave para DataStore
private val CLOUD_BACKUP_ENABLED_KEY = booleanPreferencesKey("cloud_backup_enabled")