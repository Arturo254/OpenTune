/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.screens.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.utils.ItemWrapper
import com.arturo254.opentune.utils.LocalMediaScanner
import kotlinx.coroutines.launch

private val audioPermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

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
            runCatching { LocalMediaScanner.scan(context, database) }
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

    val songs by database.localSongs().collectAsState(initial = emptyList())

    val wrappedSongs = songs.map { item -> ItemWrapper(item) }.toMutableList()
    var selection by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier.fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isScanning,
                onRefresh = { scan() },
            ),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "filter", contentType = CONTENT_TYPE_HEADER) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        label = { Text(stringResource(R.string.filter_on_device)) },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                        onClick = onDeselect,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "",
                            )
                        },
                    )
                }
            }

            if (!hasPermission) {
                item(key = "permission", contentType = CONTENT_TYPE_HEADER) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.local_music_permission_rationale),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = { requestPermissionLauncher.launch(audioPermission) }) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }
            } else if (songs.isEmpty() && !isScanning) {
                item(key = "empty", contentType = CONTENT_TYPE_HEADER) {
                    Text(
                        text = stringResource(R.string.no_local_music_found),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            } else {
                item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            itemsIndexed(
                items = wrappedSongs,
                key = { _, item -> item.item.song.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG },
            ) { index, songWrapper ->
                SongListItem(
                    song = songWrapper.item,
                    showInLibraryIcon = false,
                    isActive = songWrapper.item.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = songWrapper.item,
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
                    isSelected = songWrapper.isSelected && selection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (!selection) {
                                    if (songWrapper.item.id == mediaMetadata?.id) {
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
                                } else {
                                    songWrapper.isSelected = !songWrapper.isSelected
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!selection) selection = true
                                wrappedSongs.forEach { it.isSelected = false }
                                songWrapper.isSelected = true
                            },
                        )
                        .animateItem(),
                )
            }
        }

        PullToRefreshDefaults.Indicator(
            isRefreshing = isScanning,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}
