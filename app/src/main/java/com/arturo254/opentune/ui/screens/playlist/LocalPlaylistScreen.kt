package com.arturo254.opentune.ui.screens.playlist

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.SongItem
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.PlaylistEditLockKey
import com.arturo254.opentune.constants.PlaylistSongSortDescendingKey
import com.arturo254.opentune.constants.PlaylistSongSortType
import com.arturo254.opentune.constants.PlaylistSongSortTypeKey
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.PlaylistSong
import com.arturo254.opentune.db.entities.PlaylistSongMap
import com.arturo254.opentune.extensions.move
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.DefaultDialog
import com.arturo254.opentune.ui.component.DraggableScrollbar
import com.arturo254.opentune.ui.component.EmptyPlaceholder
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.component.SortHeader
import com.arturo254.opentune.ui.component.TextFieldDialog
import com.arturo254.opentune.ui.menu.PlaylistMenu
import com.arturo254.opentune.ui.menu.SelectionSongMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.utils.ItemWrapper
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.deletePlaylistImage
import com.arturo254.opentune.utils.getPlaylistImageUri
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.utils.saveCustomPlaylistImage
import com.arturo254.opentune.viewmodels.LocalPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()


    var searchQuery by remember { mutableStateOf(TextFieldValue()) }
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength =
        remember(songs) {
            songs.fastSumBy { it.song.song.duration }
        }
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSongSortTypeKey,
        PlaylistSongSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSongSortDescendingKey,
        true
    )
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs
            } else {
                songs.filter { song ->
                    song.song.song.title
                        .contains(query.text, ignoreCase = true) ||
                            song.song.artists
                                .fastAny { it.name.contains(query.text, ignoreCase = true) }
                }
            }
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    val wrappedSongs = filteredSongs.map { item -> ItemWrapper(item) }.toMutableList()
    var selection by remember {
        mutableStateOf(false)
    }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
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
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null
                    )
                },
                title = { Text(text = stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(
                    playlistEntity.name,
                    TextRange(playlistEntity.name.length)
                ),
                onDone = { name ->
                    database.query {
                        update(
                            playlistEntity.copy(
                                name = name,
                                lastUpdateTime = LocalDateTime.now()
                            )
                        )
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistEntity.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                },
            )
        }
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
                        playlist?.playlist!!.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        if (!editable) {
                            database.transaction {
                                playlist?.id?.let { clearPlaylist(it) }
                            }
                        }
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false
                            )
                        }
                    }
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
                    text = stringResource(
                        R.string.delete_playlist_confirm,
                        playlist?.playlist!!.name
                    ),
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
                        database.query {
                            playlist?.let { delete(it.playlist) }
                        }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    val headerItems = 2
    val lazyListState = rememberLazyListState()

    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        if (to.index >= headerItems && from.index >= headerItems) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                (from.index - headerItems) to (to.index - headerItems)
            } else {
                currentDragInfo.first to (to.index - headerItems)
            }
            mutableSongs.move(from.index - headerItems, to.index - headerItems)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId, from, to)
                }

                if (viewModel.playlist.value?.playlist?.browseId != null) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId = playlistSongMap.getOrNull(successorIndex)?.setVideoId

                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId ->
                            YouTube.moveSongPlaylist(
                                viewModel.playlist.value?.playlist?.browseId!!,
                                setVideoId,
                                successorSetVideoId.toString()
                            )
                        }
                    }
                }
                dragInfo = null
            }
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    var dismissJob: Job? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
            modifier = Modifier.fillMaxSize()
        ) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item(key = "empty_placeholder") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                            modifier = Modifier
                        )
                    }
                } else {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            LocalPlaylistHeader(
                                playlist = playlist,
                                songs = songs,
                                onShowEditDialog = { showEditDialog = true },
                                onShowRemoveDownloadDialog = {
                                    showRemoveDownloadDialog = true
                                },
                                onshowDeletePlaylistDialog = {
                                    showDeletePlaylistDialog = true
                                },
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            )
                        }
                    }

                    item(key = "controls_row") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { type ->
                                        when (type) {
                                            PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                            PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            PlaylistSongSortType.NAME -> R.string.sort_by_name
                                            PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                            PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    }
                                )

                                if (editable) {
                                    FilledIconToggleButton(
                                        checked = locked,
                                        onCheckedChange = { locked = it },
                                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        if (locked) {
                                            Icon(
                                                painter = painterResource(R.drawable.lock),
                                                contentDescription = "Locked"
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.lock_open),
                                                contentDescription = "Unlocked"
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (!selection) {
                        itemsIndexed(
                            items = if (isSearching) filteredSongs else mutableSongs,
                            key = { _, song -> song.map.id },
                        ) { index, song ->
                            ReorderableItem(
                                state = reorderableState,
                                key = song.map.id,
                            ) {
                                val currentItem by rememberUpdatedState(song)
                                val isFirst = index == 0
                                val isLast = index == (if (isSearching) filteredSongs.size else mutableSongs.size) - 1
                                val isActive = song.song.id == mediaMetadata?.id

                                var isVisible by remember { mutableStateOf(true) }

                                @SuppressLint("StringFormatInvalid")
                                fun deleteFromPlaylist() {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        database.transaction {
                                            launch {
                                                playlist.playlist.browseId?.let { browseId ->
                                                    var setVideoId = getSetVideoId(currentItem.map.songId)
                                                    if (setVideoId?.setVideoId != null) {
                                                        YouTube.removeFromPlaylist(
                                                            browseId,
                                                            currentItem.map.songId,
                                                            setVideoId.setVideoId!!
                                                        )
                                                    }
                                                }
                                            }
                                            move(
                                                currentItem.map.playlistId,
                                                currentItem.map.position,
                                                Int.MAX_VALUE
                                            )
                                            delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                        }
                                        dismissJob?.cancel()
                                        dismissJob = coroutineScope.launch {
                                            val snackbarResult = snackbarHostState.showSnackbar(
                                                message = context.getString(
                                                    R.string.removed_song_from_playlist,
                                                    currentItem.song.song.title
                                                ),
                                                actionLabel = context.getString(R.string.undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                                database.transaction {
                                                    insert(currentItem.map.copy(position = playlistLength))
                                                    move(
                                                        currentItem.map.playlistId,
                                                        playlistLength,
                                                        currentItem.map.position
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isVisible,
                                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        val songContent: @Composable () -> Unit = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(ListItemHeight)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isActive) MaterialTheme.colorScheme.secondaryContainer
                                                        else MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                            ) {
                                                SongListItem(
                                                    song = song.song,
                                                    isActive = isActive,
                                                    isPlaying = isPlaying,
                                                    showInLibraryIcon = true,
                                                    trailingContent = {
                                                        IconButton(
                                                            onClick = {
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = song.song,
                                                                        playlistSong = song,
                                                                        playlistBrowseId = playlist.playlist.browseId,
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

                                                        if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching) {
                                                            IconButton(
                                                                onClick = { },
                                                                modifier = Modifier.draggableHandle(),
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.drag_handle),
                                                                    contentDescription = null,
                                                                )
                                                            }
                                                        }
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (song.song.id == mediaMetadata?.id) {
                                                                        playerConnection.player.togglePlayPause()
                                                                    } else {
                                                                        playerConnection.playQueue(
                                                                            ListQueue(
                                                                                title = playlist.playlist.name,
                                                                                items = songs.map { it.song.toMediaItem() },
                                                                                startIndex = songs.indexOfFirst { it.map.id == song.map.id },
                                                                            ),
                                                                        )
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    if (!selection) {
                                                                        selection = true
                                                                    }
                                                                    wrappedSongs.forEach { it.isSelected = false }
                                                                    wrappedSongs.find { it.item.map.id == song.map.id }?.isSelected = true
                                                                },
                                                            )
                                                )
                                            }
                                        }

                                        if (locked || !editable) {
                                            songContent()
                                        } else {
                                            songContent()
                                        }

                                        if (!isLast) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = wrappedSongs,
                            key = { _, song -> song.item.map.id },
                        ) { index, songWrapper ->
                            ReorderableItem(
                                state = reorderableState,
                                key = songWrapper.item.map.id,
                            ) {
                                val currentItem by rememberUpdatedState(songWrapper.item)
                                val isFirst = index == 0
                                val isLast = index == wrappedSongs.size - 1
                                val isActive = songWrapper.item.song.id == mediaMetadata?.id

                                var isVisible by remember { mutableStateOf(true) }

                                @SuppressLint("StringFormatInvalid")
                                fun deleteFromPlaylist() {
                                    isVisible = false
                                    coroutineScope.launch {
                                        delay(300)
                                        database.transaction {
                                            move(
                                                currentItem.map.playlistId,
                                                currentItem.map.position,
                                                Int.MAX_VALUE
                                            )
                                            delete(currentItem.map.copy(position = Int.MAX_VALUE))
                                        }
                                        dismissJob?.cancel()
                                        dismissJob = coroutineScope.launch {
                                            val snackbarResult = snackbarHostState.showSnackbar(
                                                message = context.getString(
                                                    R.string.removed_song_from_playlist,
                                                    currentItem.song.song.title
                                                ),
                                                actionLabel = context.getString(R.string.undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (snackbarResult == SnackbarResult.ActionPerformed) {
                                                database.transaction {
                                                    insert(currentItem.map.copy(position = playlistLength))
                                                    move(
                                                        currentItem.map.playlistId,
                                                        playlistLength,
                                                        currentItem.map.position
                                                    )
                                                }
                                            }
                                        }
                                        wrappedSongs.remove(songWrapper)
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isVisible,
                                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        val songContent: @Composable () -> Unit = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(ListItemHeight)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isActive) MaterialTheme.colorScheme.secondaryContainer
                                                        else MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                            ) {
                                                SongListItem(
                                                    song = songWrapper.item.song,
                                                    isActive = isActive,
                                                    isPlaying = isPlaying,
                                                    showInLibraryIcon = true,
                                                    trailingContent = {
                                                        IconButton(
                                                            onClick = {
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = songWrapper.item.song,
                                                                        playlistBrowseId = playlist?.playlist?.browseId,
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
                                                        if (sortType == PlaylistSongSortType.CUSTOM && !locked && !selection && !isSearching) {
                                                            IconButton(
                                                                onClick = { },
                                                                modifier = Modifier.draggableHandle(),
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.drag_handle),
                                                                    contentDescription = null,
                                                                )
                                                            }
                                                        }
                                                    },
                                                    isSelected = songWrapper.isSelected && selection,
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (!selection) {
                                                                        if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                                            playerConnection.player.togglePlayPause()
                                                                        } else {
                                                                            playerConnection.playQueue(
                                                                                ListQueue(
                                                                                    title = playlist!!.playlist.name,
                                                                                    items = songs.map { it.song.toMediaItem() },
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
                                                                    if (!selection) {
                                                                        selection = true
                                                                    }
                                                                    wrappedSongs.forEach { it.isSelected = false }
                                                                    songWrapper.isSelected = true
                                                                },
                                                            ),
                                                )
                                            }
                                        }

                                        if (locked || selection) {
                                            songContent()
                                        } else {
                                            songContent()
                                        }

                                        if (!isLast) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "songs_list_end") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = 2
        )

        TopAppBar(
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (isSearching) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            },
            navigationIcon = {
                com.arturo254.opentune.ui.component.IconButton(
                    onClick = {
                        when {
                            isSearching -> {
                                isSearching = false
                                searchQuery = TextFieldValue()
                                focusManager.clearFocus()
                            }

                            selection -> {
                                selection = false
                            }

                            else -> {
                                navController.navigateUp()
                            }
                        }
                    },
                    onLongClick = {
                        if (!isSearching && !selection) {
                            navController.backToMain()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.song },
                                    songPosition = wrappedSongs.filter { it.isSelected }
                                        .map { it.item.map },
                                    onDismiss = menuState::dismiss,
                                    clearAction = {
                                        selection = false
                                        wrappedSongs.clear()
                                    }
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }

                    playlist?.let { playlistData ->
                        IconButton(
                            onClick = {
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = playlistData,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    }
                }
            },
            colors = if (transparentAppBar && !selection) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
            scrollBehavior = scrollBehavior
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocalPlaylistHeader(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    onShowEditDialog: () -> Unit,
    onShowRemoveDownloadDialog: () -> Unit,
    onshowDeletePlaylistDialog: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    val playlistLength = remember(songs) {
        songs.fastSumBy { it.song.song.duration }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val liked = playlist.playlist.bookmarkedAt != null
    val editable: Boolean = playlist.playlist.isEditable == true

    // TU LÓGICA ORIGINAL PARA IMÁGENES PERSONALIZADAS
    var customThumbnailUri by rememberSaveable {
        mutableStateOf<Uri?>(getPlaylistImageUri(context, playlist.playlist.id))
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customThumbnailUri = it
            saveCustomPlaylistImage(context, playlist.playlist.id, it)
        }
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(50.dp))

        // THUMBNAIL CON TU LÓGICA ORIGINAL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = editable) {
                        if (editable) imagePickerLauncher.launch("image/*")
                    }
            ) {
                var showEditButtons by remember { mutableStateOf(false) }

                when {
                    customThumbnailUri != null -> {
                        AsyncImage(
                            model = customThumbnailUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    playlist.thumbnails.size == 1 -> {
                        AsyncImage(
                            model = playlist.thumbnails[0],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    playlist.thumbnails.size > 1 -> {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            listOf(
                                Alignment.TopStart,
                                Alignment.TopEnd,
                                Alignment.BottomStart,
                                Alignment.BottomEnd,
                            ).fastForEachIndexed { index, alignment ->
                                AsyncImage(
                                    model = playlist.thumbnails.getOrNull(index),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .align(alignment)
                                        .fillMaxSize(0.5f)
                                )
                            }
                        }
                    }

                    else -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                tint = LocalContentColor.current.copy(alpha = 0.8f),
                                modifier = Modifier.size(96.dp)
                            )
                        }
                    }
                }

                // BOTONES DE EDICIÓN (TU LÓGICA ORIGINAL)
                if (editable && showEditButtons) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(54.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = stringResource(R.string.edit_thumbnail),
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                        if (customThumbnailUri != null) {
                            IconButton(
                                onClick = {
                                    deletePlaylistImage(context, playlist.playlist.id)
                                    customThumbnailUri = null
                                    showEditButtons = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = stringResource(R.string.remove_thumbnail),
                                    tint = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = playlist.playlist.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    if (editable) {
                        onshowDeletePlaylistDialog()
                    } else {
                        database.transaction {
                            update(playlist.playlist.toggleLike())
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            if (editable) R.drawable.delete
                            else if (playlist.playlist.bookmarkedAt != null) R.drawable.favorite
                            else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (!editable && playlist.playlist.bookmarkedAt != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (editable) stringResource(R.string.delete)
                        else if (playlist.playlist.bookmarkedAt != null) stringResource(R.string.saved)
                        else stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = playlist.playlist.name,
                            items = songs.map { it.song.toMediaItem() },
                        ),
                    )
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.play),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Surface(
                onClick = {
                    playerConnection.addToQueue(
                        items = songs.map { it.song.toMediaItem() },
                    )
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = stringResource(R.string.add_to_queue_content_desc),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = buildString {
                val trackCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                    playlist.playlist.remoteSongCount ?: 0
                } else {
                    playlist.songCount
                }

                val hours = playlistLength / 3600
                val minutes = (playlistLength % 3600) / 60

                append(stringResource(R.string.playlist_description_start))
                append(" ")
                append(pluralStringResource(R.plurals.n_song, trackCount, trackCount))
                append(". ")

                if (hours > 0 && minutes > 0) {
                    append(stringResource(R.string.playlist_duration_description))
                    append(" $hours ")
                    append(if (hours > 1) stringResource(R.string.hours) else stringResource(R.string.hour))
                    append(" ")
                    append(stringResource(R.string.and))
                    append(" $minutes ")
                    append(if (minutes > 1) stringResource(R.string.minutes) else stringResource(R.string.minute))
                    append(" ")
                    append(stringResource(R.string.of_music))
                } else if (hours > 0) {
                    append(stringResource(R.string.playlist_duration_description))
                    append(" $hours ")
                    append(if (hours > 1) stringResource(R.string.hours) else stringResource(R.string.hour))
                    append(" ")
                    append(stringResource(R.string.of_music))
                } else if (minutes > 0) {
                    append(stringResource(R.string.playlist_duration_description))
                    append(" $minutes ")
                    append(if (minutes > 1) stringResource(R.string.minutes) else stringResource(R.string.minute))
                    append(" ")
                    append(stringResource(R.string.of_music))
                }

                append(". ")
                append(stringResource(R.string.playlist_enjoy_message))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // First row: Download, Shuffle, and Edit (if editable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                ToggleButton(
                    checked = downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING,
                    onCheckedChange = {
                        when (downloadState) {
                            Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                onShowRemoveDownloadDialog()
                            }
                            else -> {
                                songs.forEach { song ->
                                    val downloadRequest = DownloadRequest
                                        .Builder(song.song.id, song.song.id.toUri())
                                        .setCustomCacheKey(song.song.id)
                                        .setData(song.song.song.title.toByteArray())
                                        .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.Button },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                ) {
                    when (downloadState) {
                        Download.STATE_COMPLETED -> {
                            Icon(
                                painter = painterResource(R.drawable.offline),
                                contentDescription = stringResource(R.string.saved),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Download.STATE_DOWNLOADING -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        else -> {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = stringResource(R.string.save),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(
                        text = when (downloadState) {
                            Download.STATE_COMPLETED -> stringResource(R.string.saved)
                            Download.STATE_DOWNLOADING -> stringResource(R.string.save)
                            else -> stringResource(R.string.save)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = playlist.playlist.name,
                                items = songs.shuffled().map { it.song.toMediaItem() },
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f).semantics { role = Role.Button },
                    shapes = when {
                        editable -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        playlist.playlist.browseId != null -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.shuffle), style = MaterialTheme.typography.labelMedium)
                }

                // Show Sync button in first row only if NOT editable
                if (!editable && playlist.playlist.browseId != null) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            scope.launch(Dispatchers.IO) {
                                val playlistPage = YouTube.playlist(playlist.playlist.browseId)
                                    .getOrNull() ?: return@launch
                                database.transaction {
                                    clearPlaylist(playlist.id)
                                    playlistPage.songs
                                        .map(SongItem::toMediaMetadata)
                                        .onEach(::insert)
                                        .mapIndexed { position, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlist.id,
                                                position = position,
                                                setVideoId = song.setVideoId
                                            )
                                        }
                                        .forEach(::insert)
                                }
                            }
                            scope.launch(Dispatchers.Main) {
                                snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                            }
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.Button },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = stringResource(R.string.sync),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.sync), style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Show Edit button in first row only if editable
                if (editable) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            onShowEditDialog()
                        },
                        modifier = Modifier.weight(1f).semantics { role = Role.Button },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.edit), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            // Second row: Sync button (only if editable and has browseId)
            if (editable && playlist.playlist.browseId != null) {
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        scope.launch(Dispatchers.IO) {
                            val playlistPage = YouTube.playlist(playlist.playlist.browseId)
                                .getOrNull() ?: return@launch
                            database.transaction {
                                clearPlaylist(playlist.id)
                                playlistPage.songs
                                    .map(SongItem::toMediaMetadata)
                                    .onEach(::insert)
                                    .mapIndexed { position, song ->
                                        PlaylistSongMap(
                                            songId = song.id,
                                            playlistId = playlist.id,
                                            position = position,
                                            setVideoId = song.setVideoId
                                        )
                                    }
                                    .forEach(::insert)
                            }
                        }
                        scope.launch(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
                    shapes = ToggleButtonDefaults.shapes(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = stringResource(R.string.sync),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.sync), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}