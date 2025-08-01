package com.arturo254.opentune.ui.menu

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.innertube.YouTube
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.LocalSyncUtils
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.PlaylistSongMap
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.DefaultDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime


@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionSongMenu(
    songSelection: List<Song>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
    songPosition: List<PlaylistSongMap>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val syncUtils = LocalSyncUtils.current

    val allInLibrary by remember {
        mutableStateOf(
            songSelection.all {
                it.song.inLibrary != null
            },
        )
    }

    val allLiked by remember(songSelection) {
        mutableStateOf(
            songSelection.isNotEmpty() && songSelection.all {
                it.song.liked
            },
        )
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
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

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                songSelection.forEach { song ->
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, song.id)
                    }
                }
            }
            songSelection.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
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
                        songSelection.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
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

    LazyColumn(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.play)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.playQueue(
                        ListQueue(
                            title = "Selection",
                            items = songSelection.map { it.toMediaItem() },
                        ),
                    )
                    clearAction()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.shuffle)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.playQueue(
                        ListQueue(
                            title = "Selection",
                            items = songSelection.shuffled().map { it.toMediaItem() },
                        ),
                    )
                    clearAction()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
                    clearAction()
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_playlist)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_add),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    showChoosePlaylistDialog = true
                }
            )
        }
        item {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(
                            if (allInLibrary) R.string.remove_from_library else R.string.add_to_library
                        )
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(
                            if (allInLibrary) R.drawable.library_add_check else R.drawable.library_add
                        ),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    if (allInLibrary) {
                        database.query {
                            songSelection.forEach { song ->
                                inLibrary(song.id, null)
                            }
                        }
                    } else {
                        database.transaction {
                            songSelection.forEach { song ->
                                insert(song.toMediaMetadata())
                                inLibrary(song.id, LocalDateTime.now())
                            }
                        }
                    }
                }
            )
        }
        item {
            when (downloadState) {
                Download.STATE_COMPLETED -> {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.remove_download),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.offline),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            showRemoveDownloadDialog = true
                        }
                    )
                }

                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.downloading)) },
                        leadingContent = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        },
                        modifier = Modifier.clickable {
                            showRemoveDownloadDialog = true
                        }
                    )
                }

                else -> {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.download)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            songSelection.forEach { song ->
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
        }
        item {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(
                            if (allLiked) R.string.dislike_all else R.string.like_all
                        )
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(
                            if (allLiked) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    val allLiked = songSelection.all { it.song.liked }
                    onDismiss()
                    database.query {
                        songSelection.forEach { song ->
                            if ((!allLiked && !song.song.liked) || allLiked) {
                                update(song.song.toggleLike())
                            }
                        }
                    }
                }
            )
        }
        if (songPosition?.size != 0) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.delete)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        var i = 0
                        database.query {
                            songPosition?.forEach { cur ->
                                move(cur.playlistId, cur.position - i, Int.MAX_VALUE)
                                delete(cur.copy(position = Int.MAX_VALUE))
                                i++
                            }
                        }
                        clearAction()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionMediaMetadataMenu(
    songSelection: List<MediaMetadata>,
    currentItems: List<Timeline.Window>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val allLiked by remember(songSelection) {
        mutableStateOf(songSelection.isNotEmpty() && songSelection.all { it.liked })
    }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    val notAddedList by remember { mutableStateOf(mutableListOf<Song>()) }
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            songSelection.map {
                runBlocking {
                    withContext(Dispatchers.IO) {
                        database.insert(it)
                    }
                }
                it.id
            }
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
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

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(onClick = {
                    showRemoveDownloadDialog = false
                    songSelection.forEach { song ->
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
                    }
                }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Fondo blur + translúcido
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        ) {
            // Animación suave de aparición
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.95f)
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(25.dp)),
                    shape = RoundedCornerShape(25.dp),
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            top = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues()
                                .calculateBottomPadding(),
                        ),
                    ) {
                        if (currentItems.isNotEmpty()) {
                            item {
                                ListItem(
                                    headlineContent = { Text(text = stringResource(R.string.delete)) },
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.delete),
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        onDismiss()
                                        var i = 0
                                        currentItems.forEach { cur ->
                                            if (playerConnection.player.availableCommands.contains(
                                                    Player.COMMAND_CHANGE_MEDIA_ITEMS
                                                )
                                            ) {
                                                playerConnection.player.removeMediaItem(cur.firstPeriodIndex - i++)
                                            }
                                        }
                                        clearAction()
                                    }
                                )
                            }
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.play)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onDismiss()
                                    playerConnection.playQueue(
                                        ListQueue(
                                            "Selection",
                                            songSelection.map { it.toMediaItem() })
                                    )
                                    clearAction()
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.shuffle)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onDismiss()
                                    playerConnection.playQueue(
                                        ListQueue(
                                            "Selection",
                                            songSelection.shuffled().map { it.toMediaItem() })
                                    )
                                    clearAction()
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.queue_music),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onDismiss()
                                    playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
                                    clearAction()
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.add_to_playlist)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_add),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    showChoosePlaylistDialog = true
                                }
                            )
                        }
                        item {
                            ListItem(
                                headlineContent = { Text(text = stringResource(R.string.like_all)) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(
                                            if (allLiked) R.drawable.favorite else R.drawable.favorite_border
                                        ),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    database.query {
                                        if (allLiked) {
                                            songSelection.forEach {
                                                update(
                                                    it.toSongEntity().toggleLike()
                                                )
                                            }
                                        } else {
                                            songSelection.filter { !it.liked }.forEach {
                                                update(it.toSongEntity().toggleLike())
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        item {
                            when (downloadState) {
                                Download.STATE_COMPLETED -> {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = stringResource(R.string.remove_download),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            showRemoveDownloadDialog = true
                                        }
                                    )
                                }

                                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                    ListItem(
                                        headlineContent = { Text(text = stringResource(R.string.downloading)) },
                                        leadingContent = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            showRemoveDownloadDialog = true
                                        }
                                    )
                                }

                                else -> {
                                    ListItem(
                                        headlineContent = { Text(text = stringResource(R.string.download)) },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            songSelection.forEach { song ->
                                                val request = DownloadRequest.Builder(
                                                    song.id,
                                                    song.id.toUri()
                                                )
                                                    .setCustomCacheKey(song.id)
                                                    .setData(song.title.toByteArray())
                                                    .build()
                                                DownloadService.sendAddDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    request,
                                                    false
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
