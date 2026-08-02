/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.library

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.CONTENT_TYPE_HEADER
import com.arturo254.opentune.constants.CONTENT_TYPE_SONG
import com.arturo254.opentune.constants.SelectedLocalFoldersKey
import com.arturo254.opentune.extensions.toMediaItem
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

    var selectedFolders by rememberPreference(SelectedLocalFoldersKey, emptySet())
    var showFilterSheet by remember { mutableStateOf(false) }
    var showScanningPopup by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }
    var isScanning by remember { mutableStateOf(false) }

    fun scan() {
        if (!hasPermission || isScanning) return
        isScanning = true
        coroutineScope.launch {
            runCatching {
                LocalMediaScanner.scan(context, database)
            }
            isScanning = false
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

    val allSongs by database.localSongs().collectAsState(initial = emptyList())

    val availableFolders by remember(allSongs) {
        derivedStateOf {
            allSongs.mapNotNull { songItem ->
                extractFolderNameFromLocalPath(songItem.song.localPath)
            }.distinct().sorted()
        }
    }

    val songs by remember(allSongs, selectedFolders) {
        derivedStateOf {
            if (selectedFolders.isEmpty() || selectedFolders.contains("Todas")) {
                allSongs
            } else {
                allSongs.filter { song ->
                    val folderName = extractFolderNameFromLocalPath(song.song.localPath)
                    folderName != null && selectedFolders.contains(folderName)
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()

    AnimatedVisibility(
        visible = showFilterSheet,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        label = "foldersSheetVisibility",
    ) {
        FolderFilterBottomSheet(
            availableFolders = availableFolders,
            selectedFolders = selectedFolders,
            onDismiss = { showFilterSheet = false },
            onApply = { selected ->
                selectedFolders = selected
                showFilterSheet = false
            }
        )
    }

    ScanningDialog(
        visible = showScanningPopup,
        isScanning = isScanning,
        onDismiss = { showScanningPopup = false },
        onRescan = { scan() },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            stickyHeader(key = "filter", contentType = CONTENT_TYPE_HEADER) {
                LocalDeviceHeader(
                    hasPermission = hasPermission,
                    selectedFolders = selectedFolders,
                    availableFolders = availableFolders,
                    isScanning = isScanning,
                    onCloseFilter = onDeselect,
                    onFilterFoldersClick = {
                        if (availableFolders.isNotEmpty()) {
                            showFilterSheet = true
                        } else {
                            showScanningPopup = true
                            scan()
                        }
                    },
                    onScanClick = {
                        showScanningPopup = true
                        scan()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!hasPermission) {
                item(key = "permission", contentType = CONTENT_TYPE_HEADER) {
                    PermissionCard(onClick = { requestPermissionLauncher.launch(audioPermission) })
                }
            } else if (songs.isEmpty() && !isScanning) {
                item(key = "empty", contentType = CONTENT_TYPE_HEADER) {
                    EmptySongsCard(
                        isFiltered = selectedFolders.isNotEmpty() && !selectedFolders.contains("Todas"),
                        onScan = { scan() }
                    )
                }
            }

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
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = context.getString(R.string.filter_on_device),
                                    items = songs.map { it.toMediaItem() },
                                    startIndex = index,
                                ),
                            )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocalDeviceHeader(
    hasPermission: Boolean,
    selectedFolders: Set<String>,
    availableFolders: List<String>,
    isScanning: Boolean,
    onCloseFilter: () -> Unit,
    onFilterFoldersClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleText = stringResource(R.string.filter_on_device)
    val filterFoldersText = stringResource(R.string.filter_folders)
    val scanText = stringResource(R.string.scan_local_music)
    val allFoldersText = stringResource(R.string.all_folders)

    val subtitle by remember(selectedFolders, allFoldersText) {
        derivedStateOf {
            if (selectedFolders.isEmpty() || selectedFolders.contains("Todas")) {
                allFoldersText
            } else {
                selectedFolders.sorted().joinToString(separator = " • ")
            }
        }
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isScanning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                      else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "headerContainerColor",
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isScanning) 36.dp else 28.dp,
        label = "headerCornerRadius",
    )

    Surface(
        color = animatedContainerColor,
        shape = RoundedCornerShape(animatedCornerRadius),
        tonalElevation = 3.dp,
        modifier = modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    label = { 
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        ) 
                    },
                    selected = true,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    onClick = onCloseFilter,
                    shape = RoundedCornerShape(20.dp),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                )

                // Botón de Ajustes (Icono 'tune' de M3)
                FilledTonalIconButton(
                    onClick = onFilterFoldersClick,
                    enabled = hasPermission && (availableFolders.isNotEmpty() || !isScanning),
                    shape = MaterialTheme.shapes.medium,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.tune),
                        contentDescription = filterFoldersText,
                    )
                }
            }

            // Subtítulo con estilo de píldora interactiva
            AnimatedContent(targetState = subtitle, label = "foldersSubtitle") { value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.folder),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.local_music_permission_rationale),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.grant_permission), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptySongsCard(isFiltered: Boolean, onScan: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = if (isFiltered) stringResource(R.string.no_songs_in_selected_folders)
                       else stringResource(R.string.no_local_music_found),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onScan,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.scan_local_music), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun extractFolderNameFromLocalPath(localPath: String?): String? {
    if (localPath.isNullOrBlank()) return null
    val uri = runCatching { Uri.parse(localPath) }.getOrNull() ?: return null
    val filePath = uri.path?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { File(filePath).parentFile?.name }.getOrNull()
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
private fun ScanningDialog(
    visible: Boolean,
    isScanning: Boolean,
    onDismiss: () -> Unit,
    onRescan: () -> Unit,
) {
    if (!visible) return

    val title = stringResource(R.string.searching_for_media)
    val message = stringResource(R.string.please_wait_scanning)
    val cancel = stringResource(R.string.cancel)
    val rescan = stringResource(R.string.scan_local_music)

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                CircularWavyProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    overflowIndicator = {
                        ButtonGroupDefaults.OverflowIndicator(it)
                    }
                ) {
                    clickableItem(
                        label = cancel,
                        onClick = onDismiss
                    )

                    clickableItem(
                        label = rescan,
                        enabled = !isScanning,
                        onClick = onRescan
                    )
                }
            }
        }
    }
}