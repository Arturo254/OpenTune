package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.innertube.utils.parseCookieString
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.CONTENT_TYPE_HEADER
import com.arturo254.opentune.constants.CONTENT_TYPE_PLAYLIST
import com.arturo254.opentune.constants.GridItemSize
import com.arturo254.opentune.constants.GridItemsSizeKey
import com.arturo254.opentune.constants.GridThumbnailHeight
import com.arturo254.opentune.constants.InnerTubeCookieKey
import com.arturo254.opentune.constants.LibraryViewType
import com.arturo254.opentune.constants.PlaylistSortDescendingKey
import com.arturo254.opentune.constants.PlaylistSortType
import com.arturo254.opentune.constants.PlaylistSortTypeKey
import com.arturo254.opentune.constants.PlaylistViewTypeKey
import com.arturo254.opentune.constants.YtmSyncKey
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.PlaylistEntity
import com.arturo254.opentune.ui.component.CreatePlaylistDialog
import com.arturo254.opentune.ui.component.HideOnScrollFAB
import com.arturo254.opentune.ui.component.LibraryPlaylistGridItem
import com.arturo254.opentune.ui.component.LibraryPlaylistListItem
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.PlaylistGridItem
import com.arturo254.opentune.ui.component.PlaylistListItem
import com.arturo254.opentune.ui.component.SortHeader
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,

    ) {
    val menuState = LocalMenuState.current
    LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSortDescendingKey,
        true
    )
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val playlists by viewModel.allPlaylists.collectAsState()

    val topSize by viewModel.topValue.collectAsState(initial = 50)

    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                        PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = pluralStringResource(
                    R.plurals.n_playlist,
                    playlists.size,
                    playlists.size
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            when (viewType) {
                                LibraryViewType.LIST -> R.drawable.list
                                LibraryViewType.GRID -> R.drawable.grid_view
                            },
                        ),
                    contentDescription = null,
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    item(
                        key = "likedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistListItem(
                            playlist = likedPlaylist,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/liked")
                                    }
                                    .animateItem(),
                        )
                    }

                    item(
                        key = "downloadedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistListItem(
                            playlist = downloadPlaylist,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/downloaded")
                                    }
                                    .animateItem(),
                        )
                    }

                    item(
                        key = "TopPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistListItem(
                            playlist = topPlaylist,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("top_playlist/$topSize")
                                    }
                                    .animateItem(),
                        )
                    }

                    item(
                        key = "cachePlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = cachePlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("cache_playlist/cached")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    playlists.let { playlists ->
                        if (playlists.isEmpty()) {
                            item {
                            }
                        }

                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { playlist ->
                            LibraryPlaylistListItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.add,
                    onClick = {
                        showCreatePlaylistDialog = true
                    },
                )
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                        GridCells.Adaptive(
                            minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                        ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    item(
                        key = "likedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = likedPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/liked")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    item(
                        key = "downloadedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = downloadPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/downloaded")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    item(
                        key = "TopPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = topPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("top_playlist/$topSize")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    item(
                        key = "cachePlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = cachePlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("cache_playlist/cached")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    playlists.let { playlists ->
                        if (playlists.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                            }
                        }

                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { playlist ->
                            LibraryPlaylistGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier.animateItem(),
                                context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = {
                        showCreatePlaylistDialog = true
                    },
                )
            }
        }
    }
}