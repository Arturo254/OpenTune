package com.arturo254.opentune.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.innertube.YouTube
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.PlaylistSong
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.DefaultDialog
import com.arturo254.opentune.ui.component.PlaylistListItem
import com.arturo254.opentune.ui.component.TextFieldDialog
import com.arturo254.opentune.ui.component.MenuItemData
import com.arturo254.opentune.ui.component.MenuGroup
import com.arturo254.opentune.ui.component.NewAction
import com.arturo254.opentune.ui.component.NewActionGrid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime


@Composable
fun PlaylistMenu(
    playlist: Playlist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    autoPlaylist: Boolean? = false,
    downloadPlaylist: Boolean? = false,
    songList: List<Song>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlist(playlist.id).collectAsState(initial = playlist)
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    LaunchedEffect(Unit) {
        if (autoPlaylist == false) {
            database.playlistSongs(playlist.id).collect {
                songs = it.map(PlaylistSong::song)
            }
        } else {
            if (songList != null) {
                songs = songList
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist.playlist.isEditable == true

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_playlist)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue =
                TextFieldValue(
                    playlist.playlist.name,
                    TextRange(playlist.playlist.name.length),
                ),
            onDone = { name ->
                onDismiss()
                database.query {
                    update(
                        playlist.playlist.copy(
                            name = name,
                            lastUpdateTime = LocalDateTime.now()
                        )
                    )
                }
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                }
            },
        )
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.playlist.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.transaction {
                            if (playlist.playlist.bookmarkedAt != null) {
                                update(playlist.playlist.toggleLike())
                            }
                            delete(playlist.playlist)
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            playlist.playlist.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { playlistId ->
                    YouTube.addPlaylistToPlaylist(playlistId, playlist.id)
                }
            }
            songs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    PlaylistListItem(
        playlist = playlist,
        trailingContent = {
            if (playlist.playlist.isEditable != true) {
                IconButton(
                    onClick = {
                        database.query {
                            dbPlaylist?.playlist?.toggleLike()?.let { update(it) }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null
                    )
                }
            }
        },
    )

    Spacer(modifier = Modifier.height(20.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        // Grid de acciones principales
        item {
            NewActionGrid(
                actions = buildList {
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.play),
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = playlist.playlist.name,
                                        items = songs.map { it.toMediaItem() },
                                    ),
                                )
                            }
                        )
                    )

                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.shuffle),
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = playlist.playlist.name,
                                        items = songs.shuffled().map { it.toMediaItem() },
                                    ),
                                )
                            }
                        )
                    )

                    playlist.playlist.browseId?.let { browseId ->
                        add(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.radio),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = stringResource(R.string.start_radio),
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        YouTube.playlist(browseId).getOrNull()?.playlist?.let { playlistItem ->
                                            playlistItem.radioEndpoint?.let { radioEndpoint ->
                                                withContext(Dispatchers.Main) {
                                                    playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                                }
                                            }
                                        }
                                    }
                                    onDismiss()
                                }
                            )
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        // Grupo: Reproducción
        item {
            MenuGroup(
                items = listOf(
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.play_next)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_play),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            coroutineScope.launch {
                                playerConnection.playNext(songs.map { it.toMediaItem() })
                            }
                            onDismiss()
                        }
                    ),
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.add_to_queue)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            onDismiss()
                            playerConnection.addToQueue(songs.map { it.toMediaItem() })
                        }
                    ),
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.add_to_playlist)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { showChoosePlaylistDialog = true }
                    )
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Grupo: Descarga (solo si no es downloadPlaylist)
        if (downloadPlaylist != true) {
            item {
                MenuGroup(
                    items = listOf(
                        when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.remove_download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
                                )
                            }

                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    icon = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
                                )
                            }

                            else -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = {
                                        songs.forEach { song ->
                                            val downloadRequest =
                                                DownloadRequest
                                                    .Builder(song.id, song.id.toUri())
                                                    .setCustomCacheKey(song.id)
                                                    .setData(song.song.title.toByteArray())
                                                    .build()
                                            DownloadService.sendAddDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                downloadRequest,
                                                false,
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // Grupo: Gestión
        item {
            MenuGroup(
                items = buildList {
                    if (editable && autoPlaylist != true) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.edit)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.edit),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = { showEditDialog = true }
                            )
                        )
                    }

                    if (autoPlaylist != true) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.delete)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = { showDeletePlaylistDialog = true }
                            )
                        )
                    }

                    playlist.playlist.shareLink?.let { shareLink ->
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.share)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.share),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    val intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareLink)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}