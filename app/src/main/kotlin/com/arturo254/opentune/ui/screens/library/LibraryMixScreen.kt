/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 * New UI by ArchiveTune (github.com/ArchiveTuneApp/ArchiveTune)
 */

package com.arturo254.opentune.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.LibraryFilter
import com.arturo254.opentune.constants.MixSortDescendingKey
import com.arturo254.opentune.constants.MixSortType
import com.arturo254.opentune.constants.MixSortTypeKey
import com.arturo254.opentune.constants.PlaylistSortType
import com.arturo254.opentune.constants.PlaylistSortTypeKey
import com.arturo254.opentune.constants.PlaylistTagsFilterKey
import com.arturo254.opentune.constants.ShowCachedPlaylistKey
import com.arturo254.opentune.constants.ShowDownloadedPlaylistKey
import com.arturo254.opentune.constants.ShowLikedPlaylistKey
import com.arturo254.opentune.constants.ShowSpotifyPlaylistsKey
import com.arturo254.opentune.constants.ShowTopPlaylistKey
import com.arturo254.opentune.constants.YtmSyncKey
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.extensions.move
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.LocalAlbumRadio
import com.arturo254.opentune.spotify.SpotifyLibraryViewModel
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.spotify.models.SpotifyPlaylist
import com.arturo254.opentune.ui.component.ExpressivePullToRefreshBox
import com.arturo254.opentune.ui.component.LibraryAlbumSpotlightCard
import com.arturo254.opentune.ui.component.LibraryArtistSpotlightCard
import com.arturo254.opentune.ui.component.LibraryPinnedCollectionTile
import com.arturo254.opentune.ui.component.LibraryPlaylistListItem
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.SortHeader
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.Collator
import java.util.Locale
import kotlin.collections.emptyList

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    onTabSelected: (LibraryFilter) -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    spotifyLibraryViewModel: SpotifyLibraryViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val database = LocalDatabase.current

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        MixSortTypeKey,
        MixSortType.CREATE_DATE,
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val (playlistSortType) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CUSTOM)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val (selectedTagsFilter) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }
    val filteredPlaylistIds by database.playlistIdsByTags(
        if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList(),
    ).collectAsState(initial = emptyList())

    val (showSpotifyPlaylists) = rememberPreference(ShowSpotifyPlaylistsKey, false)
    val spotifyPlaylists by spotifyLibraryViewModel.playlists.collectAsStateWithLifecycle()

    val likedSongsCount by database.likedSongsCount().collectAsState(initial = 0)

    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)

    val topSize by viewModel.topValue.collectAsState(initial = "50")
    val likedTitle = stringResource(R.string.liked)
    val downloadedTitle = stringResource(R.string.offline)
    val cachedTitle = stringResource(R.string.cached_playlist)
    val topTitle = stringResource(R.string.my_top) + " $topSize"

    val collator = remember {
        Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
    }

    val visiblePlaylists = remember(playlists, selectedTagIds, filteredPlaylistIds) {
        if (selectedTagIds.isEmpty()) {
            playlists
        } else {
            playlists.filter { it.id in filteredPlaylistIds }
        }
    }

    val sortedAlbums = remember(albums, sortType, sortDescending, collator) {
        val sorted = when (sortType) {
            MixSortType.CREATE_DATE -> albums.sortedBy { it.album.bookmarkedAt }
            MixSortType.NAME -> albums.sortedWith(compareBy(collator) { it.album.title })
            MixSortType.LAST_UPDATED -> albums.sortedBy { it.album.lastUpdateTime }
        }
        if (sortDescending) sorted.asReversed() else sorted
    }

    val sortedArtists = remember(artists, sortType, sortDescending, collator) {
        val sorted = when (sortType) {
            MixSortType.CREATE_DATE -> artists.sortedBy { it.artist.bookmarkedAt }
            MixSortType.NAME -> artists.sortedWith(compareBy(collator) { it.artist.name })
            MixSortType.LAST_UPDATED -> artists.sortedBy { it.artist.lastUpdateTime }
        }
        if (sortDescending) sorted.asReversed() else sorted
    }

    val shortcuts = buildList {
        if (showLiked) {
            add(
                LibraryShortcutEntry(
                    title = likedTitle,
                    iconRes = R.drawable.favorite,
                    route = "auto_playlist/liked",
                    accentColor = MaterialTheme.colorScheme.error,
                )
            )
        }
        if (showDownloaded) {
            add(
                LibraryShortcutEntry(
                    title = downloadedTitle,
                    iconRes = R.drawable.offline,
                    route = "auto_playlist/downloaded",
                    accentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
        if (showCached) {
            add(
                LibraryShortcutEntry(
                    title = cachedTitle,
                    iconRes = R.drawable.cached,
                    route = "cache_playlist/cached",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                )
            )
        }
        if (showTop) {
            add(
                LibraryShortcutEntry(
                    title = topTitle,
                    iconRes = R.drawable.trending_up,
                    route = "top_playlist/$topSize",
                    accentColor = MaterialTheme.colorScheme.secondary,
                )
            )
        }
    }

    val lazyListState = rememberLazyListState()
    val customPlaylistMode = playlistSortType == PlaylistSortType.CUSTOM
    val canEnterReorderMode = customPlaylistMode && selectedTagIds.isEmpty()
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val canReorderPlaylists = canEnterReorderMode && reorderEnabled
    val playlistSectionLeadingItems = if (shortcuts.isNotEmpty()) 3 else 2
    val mutableVisiblePlaylists = remember { mutableStateListOf<Playlist>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) { from, to ->
        if (!canReorderPlaylists) return@rememberReorderableLazyListState
        if (from.index < playlistSectionLeadingItems || to.index < playlistSectionLeadingItems) {
            return@rememberReorderableLazyListState
        }

        val fromIndex = from.index - playlistSectionLeadingItems
        val toIndex = to.index - playlistSectionLeadingItems
        if (fromIndex !in mutableVisiblePlaylists.indices || toIndex !in mutableVisiblePlaylists.indices) {
            return@rememberReorderableLazyListState
        }

        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) fromIndex to toIndex else currentDragInfo.first to toIndex
        mutableVisiblePlaylists.move(fromIndex, toIndex)
    }

    LaunchedEffect(visiblePlaylists, canReorderPlaylists, reorderableState.isAnyItemDragging, dragInfo) {
        if (!canReorderPlaylists) {
            mutableVisiblePlaylists.clear()
            mutableVisiblePlaylists.addAll(visiblePlaylists)
            return@LaunchedEffect
        }

        if (!reorderableState.isAnyItemDragging && dragInfo == null) {
            mutableVisiblePlaylists.clear()
            mutableVisiblePlaylists.addAll(visiblePlaylists)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging, canReorderPlaylists) {
        if (!canReorderPlaylists || reorderableState.isAnyItemDragging) return@LaunchedEffect

        dragInfo ?: return@LaunchedEffect
        val playlistsToReorder = mutableVisiblePlaylists.toList()
        database.transaction {
            playlistsToReorder.forEachIndexed { index, playlist ->
                setPlaylistCustomOrder(playlist.id, index)
            }
        }
        dragInfo = null
    }

    LaunchedEffect(canEnterReorderMode) {
        if (!canEnterReorderMode) reorderEnabled = false
    }

    ExpressivePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.syncAllLibrary() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "filter") {
                filterContent()
            }

            item(key = "controls") {
                LibraryControlCard(
                    canEnterReorderMode = canEnterReorderMode,
                    reorderEnabled = reorderEnabled,
                    onToggleReorder = { reorderEnabled = !reorderEnabled },
                ) {
                    SortHeader(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { type ->
                            when (type) {
                                MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                                MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                                MixSortType.NAME -> R.string.sort_by_name
                            }
                        },
                    )
                }
            }

            if (shortcuts.isNotEmpty()) {
                item(key = "shortcuts") {
                    LibraryShortcutGrid(
                        entries = shortcuts,
                        onClick = navController::navigate,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }

            if (canReorderPlaylists) {
                itemsIndexed(
                    items = mutableVisiblePlaylists,
                    key = { _, item -> item.id },
                ) { _, item ->
                    ReorderableItem(
                        state = reorderableState,
                        key = item.id,
                    ) {
                        LibraryPlaylistListItem(
                            navController = navController,
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            playlist = item,
                            showDragHandle = true,
                            dragHandleModifier = Modifier.draggableHandle(),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem(),
                        )
                    }
                }
            } else {
                items(
                    items = visiblePlaylists,
                    key = { it.id },
                ) { item ->
                    LibraryPlaylistListItem(
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        playlist = item,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }

            if (showSpotifyPlaylists && spotifyPlaylists.isNotEmpty()) {
                item(key = "spotify_playlists_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.spotify_playlists),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Icon(
                            painter = painterResource(R.drawable.chevron_right),
                            contentDescription = stringResource(R.string.see_all),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onTabSelected(LibraryFilter.SPOTIFY) }
                                .padding(4.dp),
                        )
                    }
                }

                item(key = "spotify_playlists_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(
                            items = spotifyPlaylists.take(8),
                            key = { playlist -> "spotify_playlist_${playlist.id}" },
                            contentType = { "spotify_playlist" },
                        ) { playlist ->
                            SpotifyPlaylistCompactCard(
                                playlist = playlist,
                                onClick = {
                                    navController.navigate("spotify_playlist/${playlist.id}")
                                },
                            )
                        }
                    }
                }
            }

            if (sortedAlbums.isNotEmpty()) {
                item(key = "albums_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.your_albums),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Icon(
                            painter = painterResource(R.drawable.chevron_right),
                            contentDescription = stringResource(R.string.see_all),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onTabSelected(LibraryFilter.ALBUMS) }
                                .padding(4.dp),
                        )
                    }
                }

                items(
                    items = sortedAlbums,
                    key = { it.id },
                ) { album ->
                    LibraryAlbumSpotlightCard(
                        album = album,
                        isActive = album.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        onPlay = {
                            coroutineScope.launch {
                                database.albumWithSongs(album.id).firstOrNull()
                                    ?.let { albumWithSongs ->
                                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                                    }
                            }
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = album,
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
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .combinedClickable(
                                onClick = { navController.navigate("album/${album.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = album,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            )
                            .animateItem(),
                    )
                }
            }

            if (sortedArtists.isNotEmpty()) {
                item(key = "artists_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.your_artists),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Icon(
                            painter = painterResource(R.drawable.chevron_right),
                            contentDescription = stringResource(R.string.see_all),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onTabSelected(LibraryFilter.ARTISTS) }
                                .padding(4.dp),
                        )
                    }
                }

                item(key = "artists_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(
                            items = sortedArtists.take(10),
                            key = { it.id },
                        ) { artist ->
                            Column(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable {
                                        navController.navigate("artist/${artist.id}")
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AsyncImage(
                                    model = artist.artist.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = artist.artist.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { onTabSelected(LibraryFilter.ARTISTS) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.add),
                                        contentDescription = stringResource(R.string.more_label),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.more_label),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyPlaylistCompactCard(
    playlist: SpotifyPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnailUrl = remember(playlist) { SpotifyMapper.getPlaylistThumbnail(playlist) }
    val cardBgColor = MaterialTheme.colorScheme.surfaceContainer

    Column(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(cardBgColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(106.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.spotify_icon),
                    contentDescription = stringResource(R.string.spotify_account),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${playlist.tracks?.total ?: 0} ${stringResource(R.string.tracks_label)}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun LibraryControlCard(
    canEnterReorderMode: Boolean,
    reorderEnabled: Boolean,
    onToggleReorder: () -> Unit,
    controls: @Composable RowScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                content = controls,
            )
            if (canEnterReorderMode) {
                IconButton(
                    onClick = onToggleReorder,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        painter = painterResource(if (reorderEnabled) R.drawable.lock_open else R.drawable.lock),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryShortcutGrid(
    entries: List<LibraryShortcutEntry>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        entries.chunked(2).forEach { rowEntries ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowEntries.forEach { entry ->
                    LibraryPinnedCollectionTile(
                        title = entry.title,
                        iconRes = entry.iconRes,
                        accentColor = entry.accentColor,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = { onClick(entry.route) }),
                    )
                }
                if (rowEntries.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class LibraryShortcutEntry(
    val title: String,
    @DrawableRes val iconRes: Int,
    val route: String,
    val accentColor: Color,
)