package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.CONTENT_TYPE_HEADER
import com.arturo254.opentune.constants.CONTENT_TYPE_SONG
import com.arturo254.opentune.constants.SongFilter
import com.arturo254.opentune.constants.SongFilterKey
import com.arturo254.opentune.constants.SongSortDescendingKey
import com.arturo254.opentune.constants.SongSortType
import com.arturo254.opentune.constants.SongSortTypeKey
import com.arturo254.opentune.constants.YtmSyncKey
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.move
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.ChipsRow
import com.arturo254.opentune.ui.component.HideOnScrollFAB
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.component.SortHeader
import com.arturo254.opentune.ui.component.VerticalFastScroller
import com.arturo254.opentune.ui.menu.SelectionSongMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.utils.ItemWrapper
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.LibrarySongsViewModel
import org.burnoutcrew.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val mutableSongs =
        remember {
            mutableStateListOf<Song>()
        }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val songs by viewModel.allSongs.collectAsState()

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            when (filter) {
                SongFilter.LIKED -> viewModel.syncLikedSongs()
                SongFilter.LIBRARY -> viewModel.syncLibrarySongs()
                else -> return@LaunchedEffect
            }
        }
    }

    val wrappedSongs = songs.map { item -> ItemWrapper(item) }.toMutableList()
    var selection by remember {
        mutableStateOf(false)
    }

    val lazyListState = rememberLazyListState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }





    val contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
        .asPaddingValues()


    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = contentPadding.calculateTopPadding(),
            endContentPadding = 0.dp
        ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(
                key = "filter",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        label = { Text(stringResource(R.string.songs)) },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                        onClick = onDeselect,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = ""
                            )
                        },
                    )
                    ChipsRow(
                        chips =
                            listOf(
                                SongFilter.LIKED to stringResource(R.string.filter_liked),
                                SongFilter.LIBRARY to stringResource(R.string.filter_library),
                                SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded),
                            ),
                        currentValue = filter,
                        onValueUpdate = {
                            filter = it
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        IconButton(
                            onClick = { selection = false },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            modifier = Modifier.weight(1f)
                        )
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
                                painter = painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all),
                                contentDescription = null,
                            )
                        }

                        IconButton(
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }
                                            .map { it.item },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false },
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        SongSortType.NAME -> R.string.sort_by_name
                                        SongSortType.ARTIST -> R.string.sort_by_artist
                                        SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                },
                            )

                            Spacer(Modifier.weight(1f))

                            Text(
                                text = pluralStringResource(
                                    R.plurals.n_song,
                                    songs.size,
                                    songs.size
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
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
                    showInLibraryIcon = true,
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.queue_all_songs),
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
                                    if (!selection) {
                                        selection = true
                                    }
                                    wrappedSongs.forEach {
                                        it.isSelected = false
                                    } // Clear previous selections
                                    songWrapper.isSelected = true // Select current item
                                },
                            )
                            .animateItem(),
                )
            }
        }
        }


        HideOnScrollFAB(
            visible = songs.isNotEmpty() == true,
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.queue_all_songs),
                        items = songs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
    }
}