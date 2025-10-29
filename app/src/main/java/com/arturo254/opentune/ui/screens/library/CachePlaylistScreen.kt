package com.arturo254.opentune.ui.screens.library

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AlbumThumbnailSize
import com.arturo254.opentune.constants.ThumbnailCornerRadius
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.EmptyPlaceholder
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.menu.SelectionSongMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.utils.ItemWrapper
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.viewmodels.HistoryViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val events by viewModel.events.collectAsState()

    val playerCache = LocalPlayerConnection.current?.service?.playerCache

    val cachedSongIds = remember(playerCache) {
        playerCache?.keys?.mapNotNull { it.toString() }?.toSet() ?: emptySet()
    }

    val allSongs = remember(events, cachedSongIds) {
        events.values.flatten()
            .map { it.song }
            .distinctBy { it.id }
            .filter { it.id in cachedSongIds }
    }

    val wrappedSongs = remember(allSongs) {
        mutableStateListOf<ItemWrapper<Song>>().apply {
            addAll(allSongs.map { ItemWrapper(it) })
        }
    }

    var selection by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs
        else wrappedSongs.filter { wrapper ->
            val song = wrapper.item
            song.title.contains(query.text, true) ||
                    song.artists.any { it.name.contains(query.text, true) }
        }
    }

    val state = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = state,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (filteredSongs.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.music_note,
                        text = stringResource(R.string.playlist_is_empty)
                    )
                }
            } else {
                if (!isSearching) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(AlbumThumbnailSize)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                ) {
                                    AsyncImage(
                                        model = filteredSongs.first().item.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.cached_playlist),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = pluralStringResource(
                                            id = R.plurals.n_song,
                                            count = filteredSongs.size,
                                            filteredSongs.size
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Cache Songs",
                                                items = filteredSongs.map { it.item.toMediaItem() },
                                            )
                                        )
                                    },
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.play))
                                }

                                OutlinedButton(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Cache Songs",
                                                items = filteredSongs.shuffled()
                                                    .map { it.item.toMediaItem() },
                                            )
                                        )
                                    },
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.shuffle))
                                }
                            }
                        }
                    }
                }

                itemsIndexed(
                    filteredSongs,
                    key = { _, song -> song.item.id }) { index, songWrapper ->
                    SongListItem(
                        song = songWrapper.item,
                        isActive = songWrapper.item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSelected = songWrapper.isSelected && selection,
                        showInLibraryIcon = true,
                        trailingContent = {
                            IconButton(onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = songWrapper.item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null
                                )
                            }
                        },
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
                                                    title = "Cache Songs",
                                                    items = filteredSongs.map { it.item.toMediaItem() },
                                                    startIndex = index
                                                )
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
                                        wrappedSongs.forEach { it.isSelected = false }
                                        songWrapper.isSelected = true
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
            }
        }

        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    isSearching -> {
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

                    else -> {
                        Text(
                            stringResource(R.string.cached_playlist),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        isSearching -> {
                            isSearching = false
                            query = TextFieldValue()
                            focusManager.clearFocus()
                        }

                        selection -> {
                            selection = false
                        }

                        else -> {
                            navController.navigateUp()
                        }
                    }
                }, onLongClick = {
                    if (!isSearching && !selection) {
                        navController.backToMain()
                    }
                }) {
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
                    IconButton(onClick = {
                        if (count == wrappedSongs.size) {
                            wrappedSongs.forEach { it.isSelected = false }
                        } else {
                            wrappedSongs.forEach { it.isSelected = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }
                                    .map { it.item },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false }
                            )
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}