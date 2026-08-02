/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.library

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.CONTENT_TYPE_HEADER
import com.arturo254.opentune.constants.CONTENT_TYPE_SONG
import com.arturo254.opentune.constants.FolderFilterKey
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.utils.LocalMediaScanner
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.launch
import java.io.File

private val audioPermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalSongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Preferencias persistentes para el filtro de carpetas
    val (savedFolders, saveFolders) = rememberPreference(FolderFilterKey, "")

    // Estado para el BottomSheet
    var showFilterSheet by remember { mutableStateOf(false) }
    var showScanningPopup by remember { mutableStateOf(false) }
    var availableFolders by remember { mutableStateOf<List<String>>(emptyList()) }

    // Estado de selección de carpetas con persistencia inicial
    var selectedFolders by remember(savedFolders) {
        mutableStateOf(savedFolders.split(",").filter { it.isNotBlank() }.toSet())
    }

    // Estado para "Todas las carpetas"
    var showAllFolders by remember(savedFolders) {
        mutableStateOf(savedFolders.isBlank())
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var isScanning by remember { mutableStateOf(false) }

    // Cargar carpetas disponibles al iniciar y cuando cambie la base de datos
    LaunchedEffect(Unit) {
        availableFolders = LocalMediaScanner.getAvailableFolders(database)
    }

    fun scan() {
        if (!hasPermission || isScanning) return
        isScanning = true
        coroutineScope.launch {
            runCatching {
                val count = LocalMediaScanner.scan(context, database)
                availableFolders = LocalMediaScanner.getAvailableFolders(database)
            }
            isScanning = false
            showScanningPopup = false
        }
    }

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            if (granted) scan()
        }

    LaunchedEffect(hasPermission) {
        if (hasPermission) scan()
    }

    // Obtener todas las canciones
    val allSongs by database.localSongs().collectAsState(initial = emptyList())

    // Filtrar canciones por carpetas seleccionadas con persistencia
    val songs by remember(allSongs, selectedFolders, showAllFolders) {
        derivedStateOf {
            if (showAllFolders || selectedFolders.isEmpty()) {
                allSongs
            } else {
                allSongs.filter { songEntity ->
                    extractFolderFromSong(songEntity)?.let { folderName ->
                        selectedFolders.contains(folderName)
                    } ?: false
                }
            }
        }
    }

    // Contador de canciones
    val songCount by remember(songs) {
        derivedStateOf { songs.size }
    }

    val lazyListState = rememberLazyListState()

    // Mostrar BottomSheet de filtros
    if (showFilterSheet) {
        FolderFilterBottomSheet(
            availableFolders = availableFolders,
            selectedFolders = if (showAllFolders) availableFolders.toSet() else selectedFolders,
            onDismiss = { showFilterSheet = false },
            onApply = { selected ->
                val finalSelection =
                    if (selected.size >= availableFolders.size || selected.isEmpty()) {
                        showAllFolders = true
                        emptySet()
                    } else {
                        showAllFolders = false
                        selected
                    }
                selectedFolders = finalSelection

                // Guardar preferencias de forma persistente
                saveFolders(finalSelection.joinToString(","))
                showFilterSheet = false
            }
        )
    }

    // Popup de escaneo / carga
    if (showScanningPopup) {
        Dialog(onDismissRequest = { showScanningPopup = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .width(300.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.scanning_media),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.please_wait_scanning),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Indicador de carga superior (linear)
        if (isScanning) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header con filtros
            item(key = "filter", contentType = CONTENT_TYPE_HEADER) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        FilterChip(
                            label = {
                                Text(
                                    stringResource(R.string.filter_on_device),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = true,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            onClick = onDeselect,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón de ajustes - Siempre interactuable si ya tenemos carpetas o estamos escaneando
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (availableFolders.isNotEmpty()) {
                                            showFilterSheet = true
                                        } else if (isScanning) {
                                            showScanningPopup = true
                                        } else {
                                            showScanningPopup = true
                                            scan()
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scan()
                                    }
                                ),
                            color = if (!showAllFolders) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.settings),
                                    contentDescription = stringResource(R.string.filter_folders),
                                    modifier = Modifier.size(20.dp),
                                    tint = if (!showAllFolders) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (!showAllFolders && selectedFolders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedFolders.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sección de permiso
            if (!hasPermission) {
                item(key = "permission", contentType = CONTENT_TYPE_HEADER) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.folder),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.local_music_permission_rationale),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { requestPermissionLauncher.launch(audioPermission) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }
            }
            // Mensaje de vacío
            else if (songs.isEmpty() && !isScanning) {
                item(key = "empty", contentType = CONTENT_TYPE_HEADER) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (!showAllFolders) {
                                stringResource(R.string.no_songs_in_selected_folders)
                            } else {
                                stringResource(R.string.no_local_music_found)
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!showAllFolders) {
                            Button(
                                onClick = {
                                    showAllFolders = true
                                    selectedFolders = emptySet()
                                    saveFolders("")
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(stringResource(R.string.show_all_folders))
                            }
                        }
                    }
                }
            }
            // Header con información
            else if (songs.isNotEmpty()) {
                item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!showAllFolders && selectedFolders.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.folder),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${selectedFolders.size} ${stringResource(R.string.folders_selected)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }

                            Text(
                                text = pluralStringResource(
                                    R.plurals.n_song,
                                    songCount,
                                    songCount
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!showAllFolders && selectedFolders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Lista de canciones
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG },
            ) { index, song ->
                SongListItem(
                    song = song,
                    isActive = mediaMetadata?.id == song.id,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = context.getString(R.string.filter_on_device),
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ),
                )
            }
        }
    }
}

/**
 * Extrae el nombre de la carpeta de una canción a partir de su localPath
 */
private fun extractFolderFromSong(song: com.arturo254.opentune.db.entities.Song): String? {
    val localPath = song.song.localPath
    if (localPath.isNullOrBlank()) return null

    return try {
        val uri = android.net.Uri.parse(localPath)
        val filePath = uri.path
        if (filePath.isNullOrBlank()) return null

        val file = File(filePath)
        val parent = file.parent
        if (parent.isNullOrBlank()) return null

        File(parent).name
    } catch (e: Exception) {
        null
    }
}
