package com.arturo254.opentune.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.tryOrNull
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.PreferenceEntry
import com.arturo254.opentune.ui.component.SettingsGeneralCategory
import com.arturo254.opentune.ui.component.SettingsPage
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.menu.OnlinePlaylistAdder
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.ui.utils.formatFileSize
import com.arturo254.opentune.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("LogNotTimber")
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerCache = LocalPlayerConnection.current?.service?.playerCache

    // Statuses
    var uploadStatus by remember { mutableStateOf<UploadStatus?>(null) }
    var showVisitorDataDialog by remember { mutableStateOf(false) }
    var showVisitorDataResetDialog by remember { mutableStateOf(false) }
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by remember { mutableStateOf(false) }
    var isProgressStarted by remember { mutableStateOf(false) }
    var progressPercentage by remember { mutableIntStateOf(0) }

    // NEW: Status to control automatic upload to the cloud
    var enableCloudUpload by remember {
        mutableStateOf(
            context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
                .getBoolean("enable_cloud_upload", false)
        )
    }

    // Cache stats
    var playerCacheSize by remember { mutableLongStateOf(tryOrNull { playerCache?.cacheSpace } ?: 0L) }
    var isClearing by remember { mutableStateOf(false) }

    val animatedPlayerCacheSize by animateFloatAsState(
        targetValue = if (playerCacheSize > 0) 1f else 0f,
        label = "playerCacheProgress"
    )

    // Update cache size
    LaunchedEffect(playerCache) {
        while (true) {
            delay(1000)
            playerCacheSize = tryOrNull { playerCache?.cacheSpace } ?: 0L
        }
    }

    // Launchers
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri)

                // MODIFIED: Only upload to the cloud if the user has enabled it.
                if (enableCloudUpload) {
                    coroutineScope.launch {
                        uploadStatus = UploadStatus.Uploading
                        val fileUrl = uploadBackupToFilebin(context, uri)
                        uploadStatus = if (fileUrl != null) {
                            UploadStatus.Success(fileUrl)
                        } else {
                            UploadStatus.Failure
                        }
                    }
                }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(context, uri)
            }
        }

    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val result = viewModel.importPlaylistFromCsv(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }

    val importM3uLauncherOnline =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val result = viewModel.loadM3UOnline(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }

    SettingsPage(
        title = stringResource(R.string.backup_restore),
        navController = navController,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.backup_restore),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.cloud_upload_title)) },
                    icon = { Icon(painterResource(R.drawable.cloud_lock), null) },
                    checked = enableCloudUpload,
                    description = stringResource(
                        if (enableCloudUpload) {
                            R.string.cloud_upload_enabled_description
                        } else {
                            R.string.cloud_upload_disabled_description
                        }
                    ),
                    onCheckedChange = { isEnabled ->
                        enableCloudUpload = isEnabled
                        // Save preference
                        context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("enable_cloud_upload", isEnabled)
                            .apply()
                    }
                )},
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.backup)) },
                    icon = { Icon(painterResource(R.drawable.backup), null) },
                    description = stringResource(if (enableCloudUpload) R.string.backup_with_cloud else R.string.backup_description),
                    isEnabled = uploadStatus !is UploadStatus.Uploading,
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupLauncher.launch(
                            "${context.getString(R.string.app_name)}_${
                                LocalDateTime.now().format(formatter)
                            }.backup"
                        )
                    }
                )},
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.restore)) },
                    icon = { Icon(painterResource(R.drawable.restore), null) },
                    description = stringResource(R.string.restore_description),
                    isEnabled = uploadStatus !is UploadStatus.Uploading,
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/octet-stream"))
                    }
                )},
                {AnimatedVisibility(
                    visible = uploadStatus != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {MinimalUploadStatus(uploadStatus) {
                    copyToClipboard(context, (uploadStatus as UploadStatus.Success).fileUrl)
                }}}
            )
        )

        // VISITOR_DATA Card
        MinimalVisitorDataCard(
            playerCacheSize = playerCacheSize,
            progress = animatedPlayerCacheSize,
            isClearing = isClearing,
            onResetClick = { showVisitorDataResetDialog = true },
            onInfoClick = { showVisitorDataDialog = true }
        )
    }

    // Dialogs
    if (showVisitorDataDialog) {
        MinimalInfoDialog(
            icon = painterResource(R.drawable.info),
            title = stringResource(R.string.visitor_data_info_title),
            message = stringResource(R.string.visitor_data_info_intro) + "\n\n" +
                    stringResource(R.string.visitor_data_info_problems) + "\n\n" +
                    stringResource(R.string.visitor_data_info_solution),
            onDismiss = { showVisitorDataDialog = false }
        )
    }

    if (showVisitorDataResetDialog) {
        MinimalConfirmDialog(
            icon = painterResource(R.drawable.replay),
            title = stringResource(R.string.visitor_data_reset_title),
            message = stringResource(R.string.visitor_data_reset_message),
            confirmText = stringResource(R.string.visitor_data_reset_confirm),
            onConfirm = {
                isClearing = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        // Clear song cache
                        playerCache?.keys?.toList()?.forEach { key ->
                            tryOrNull { playerCache.removeResource(key) }
                        }

                        // Reset VISITOR_DATA
                        viewModel.resetVisitorData(context)

                        delay(500)

                        withContext(Dispatchers.Main) {
                            playerCacheSize = 0L
                            isClearing = false
                            showVisitorDataResetDialog = false

                            delay(300)
                            restartApp(context)
                        }
                    } catch (e: Exception) {
                        Log.e("BackupRestore", "Error al restablecer VISITOR_DATA", e)
                        withContext(Dispatchers.Main) {
                            isClearing = false
                            showVisitorDataResetDialog = false
                        }
                    }
                }
            },
            onDismiss = { showVisitorDataResetDialog = false }
        )
    }

    OnlinePlaylistAdder(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { newVal -> isProgressStarted = newVal },
        onPercentageChange = { newPercentage -> progressPercentage = newPercentage }
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    if (isProgressStarted) {
        MinimalLoadingOverlay(progress = progressPercentage)
    }
}


@Composable
private fun MinimalVisitorDataCard(
    playerCacheSize: Long,
    progress: Float,
    isClearing: Boolean,
    onResetClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.replay),
                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.visitor_data_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = stringResource(R.string.visitor_data_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Cache indicator
            if (playerCacheSize > 0 || isClearing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { if (isClearing) 0f else progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isClearing) {
                                stringResource(R.string.cache_clearing)
                            } else {
                                stringResource(R.string.song_cache)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatFileSize(playerCacheSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInfoClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isClearing
                ) {
                    Icon(
                        painter = painterResource(R.drawable.help),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.info_button), style = MaterialTheme.typography.labelLarge)
                }

                FilledTonalButton(
                    onClick = onResetClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isClearing
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.replay),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.reset_button), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MinimalUploadStatus(
    uploadStatus: UploadStatus?,
    onCopyClick: () -> Unit
) {
    when (uploadStatus) {
        is UploadStatus.Uploading -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.uploading_backup),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is UploadStatus.Success -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.backup_success),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = uploadStatus.fileUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Button(
                        onClick = onCopyClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.content_copy),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.copy_link))
                    }
                }
            }
        }

        is UploadStatus.Failure -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.error),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.backup_upload_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        null -> {}
    }
}

@Composable
private fun MinimalLoadingOverlay(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.processing_songs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MinimalInfoDialog(
    icon: Painter,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.understood))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun MinimalConfirmDialog(
    icon: Painter,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// Filebin upload function (no significant changes)
@SuppressLint("LogNotTimber")
suspend fun uploadBackupToFilebin(
    context: Context,
    uri: Uri,
    progressCallback: (Float) -> Unit = {}
): String? {
    return withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_backup_${System.currentTimeMillis()}.backup")

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null

            inputStream.use { input ->
                val fileSize = try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && cursor.moveToFirst()) {
                            cursor.getLong(sizeIndex)
                        } else {
                            input.available().toLong()
                        }
                    } ?: input.available().toLong()
                } catch (e: Exception) {
                    Log.w("BackupRestore", "The file size could not be obtained: ${e.message}")
                    input.available().toLong()
                }

                var totalBytesRead = 0L
                tempFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (fileSize > 0) {
                            progressCallback(totalBytesRead / fileSize.toFloat() * 0.5f)
                        }
                    }
                }
            }

            val binId = UUID.randomUUID().toString().substring(0, 8)

            val fileRequestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
                override fun contentLength() = tempFile.length()

                override fun writeTo(sink: BufferedSink) {
                    tempFile.inputStream().use { input ->
                        val fileSize = tempFile.length().toFloat()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            sink.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            val uploadProgress = 0.5f + (totalBytesRead / fileSize * 0.5f)
                            progressCallback(uploadProgress)
                        }
                    }
                }
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val fileName = tempFile.name
            val request = Request.Builder()
                .url("https://filebin.net/$binId/$fileName")
                .put(fileRequestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e("BackupRestore", "Error in server response: ${response.code}")
                return@withContext null
            }

            return@withContext "https://filebin.net/$binId/$fileName"

        } catch (e: Exception) {
            Log.e("BackupRestore", "Error during upload", e)
            return@withContext null
        } finally {
            if (tempFile.exists()) {
                tryOrNull { tempFile.delete() }
            }
        }
    }
}

@SuppressLint("LogNotTimber")
fun copyToClipboard(context: Context, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Backup URL", text)
        clipboard.setPrimaryClip(clip)
    } catch (e: Exception) {
        Log.e("BackupRestore", "Error copying to clipboard: ${e.message}")
    }
}

sealed class UploadStatus {
    data object Uploading : UploadStatus()
    data class Success(val fileUrl: String) : UploadStatus()
    data object Failure : UploadStatus()
}

fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)

    intent?.let {
        val componentName = it.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.setPackage(context.packageName)

        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}