package com.arturo254.opentune.ui.screens.artist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.arturo254.innertube.models.AlbumItem
import com.arturo254.innertube.models.ArtistItem
import com.arturo254.innertube.models.PlaylistItem
import com.arturo254.innertube.models.SongItem
import com.arturo254.innertube.models.WatchEndpoint
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.NavigationTitle
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.component.YouTubeGridItem
import com.arturo254.opentune.ui.component.YouTubeListItem
import com.arturo254.opentune.ui.component.shimmer.ButtonPlaceholder
import com.arturo254.opentune.ui.component.shimmer.ListItemPlaceHolder
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.menu.YouTubeArtistMenu
import com.arturo254.opentune.ui.menu.YouTubePlaylistMenu
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.ui.utils.fadingEdge
import com.arturo254.opentune.ui.utils.resize
import com.arturo254.opentune.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset

@SuppressLint("ServiceCast")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    // Calculate the offset value outside of the offset lambda
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current
            .add(
                WindowInsets(
                    top = -WindowInsets.systemBars.asPaddingValues()
                        .calculateTopPadding() - AppBarHeight
                )
            )
            .asPaddingValues(),
    ) {
        if (artistPage == null) {
            item(key = "shimmer") {
                ShimmerHost {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3),
                    ) {
                        Spacer(
                            modifier = Modifier
                                .shimmer()
                                .background(MaterialTheme.colorScheme.onSurface)
                                .fadingEdge(
                                    top = WindowInsets.systemBars
                                        .asPaddingValues()
                                        .calculateTopPadding() + AppBarHeight,
                                    bottom = 108.dp,
                                ),
                        )
                        TextPlaceholder(
                            height = 56.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 48.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        ButtonPlaceholder(Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        ButtonPlaceholder(Modifier.weight(1f))
                    }

                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        } else {

            item(key = "header") {
                val thumbnail = artistPage.artist.thumbnail
                val artistName = artistPage.artist.title

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            AsyncImage(
                                model = thumbnail.resize(1200, 1200),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .fadingEdge(
                                        top = WindowInsets.systemBars
                                            .asPaddingValues()
                                            .calculateTopPadding() + AppBarHeight,
                                        bottom = 80.dp,
                                    ),
                            )
                        }
                    }

                    // Artist Name and Controls Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        // Artist Name
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Subscriber count badge
                        artistPage.artist.subscriberCountText?.let { subscribers ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = subscribers,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Description
                        val description = artistPage.description ?: buildString {
                            append(artistName)
                            append(" is a music artist.")
                        }

                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(bottom = 16.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Buttons Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            // Subscribe Button
                            ToggleButton(
                                checked = libraryArtist?.artist?.bookmarkedAt != null,
                                onCheckedChange = {
                                    database.transaction {
                                        val artist = libraryArtist?.artist
                                        if (artist != null) {
                                            update(artist.toggleLike())
                                        } else {
                                            artistPage.artist.let {
                                                insert(
                                                    ArtistEntity(
                                                        id = it.id,
                                                        name = it.title,
                                                        channelId = it.channelId,
                                                        thumbnailUrl = it.thumbnail,
                                                    ).toggleLike()
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (libraryArtist?.artist?.bookmarkedAt != null)
                                            R.drawable.subscribed
                                        else
                                            R.drawable.subscribe
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        LocalContentColor.current
                                )
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(
                                    text = stringResource(
                                        if (libraryArtist?.artist?.bookmarkedAt != null)
                                            R.string.subscribed
                                        else
                                            R.string.subscribe
                                    ),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            // Radio Button
                            artistPage.artist.radioEndpoint?.let { radioEndpoint ->
                                ToggleButton(
                                    checked = false,
                                    onCheckedChange = {
                                        playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                    },
                                    modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.radio),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Text(
                                        text = stringResource(R.string.radio),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            // Shuffle Button
                            artistPage.artist.shuffleEndpoint?.let { shuffleEndpoint ->
                                ToggleButton(
                                    checked = false,
                                    onCheckedChange = {
                                        playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                    },
                                    modifier = Modifier.weight(1f).semantics { role = Role.Button },
                                    shapes = if (artistPage.artist.radioEndpoint != null) {
                                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    } else {
                                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    },
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = stringResource(R.string.shuffle),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Text(
                                        text = stringResource(R.string.shuffle),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Canciones de la biblioteca - Manteniendo tu lógica original
            if (librarySongs.isNotEmpty()) {
                item {
                    NavigationTitle(
                        title = stringResource(R.string.from_your_library),
                        onClick = {
                            navController.navigate("artist/${viewModel.artistId}/songs")
                        },
                    )
                }

                items(
                    items = librarySongs,
                    key = { "local_${it.id}" },
                ) { song ->
                    SongListItem(
                        song = song,
                        showInLibraryIcon = true,
                        isActive = song.id == mediaMetadata?.id,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = song.id),
                                                song.toMediaMetadata(),
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
                            )
                            .animateItem(),
                    )
                }
            }

            // Secciones del artista - Manteniendo tu lógica original
            artistPage.sections.fastForEach { section ->
                if (section.items.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = section.title,
                            onClick = section.moreEndpoint?.let {
                                {
                                    navController.navigate(
                                        "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                    )
                                }
                            },
                        )
                    }
                }

                if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                    items(
                        items = section.items,
                        key = { it.id },
                    ) { song ->
                        YouTubeListItem(
                            item = song as SongItem,
                            isActive = mediaMetadata?.id == song.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
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
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    WatchEndpoint(videoId = song.id),
                                                    song.toMediaMetadata()
                                                ),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }
                } else {
                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = section.items,
                                key = { it.id },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = when (item) {
                                        is SongItem -> mediaMetadata?.id == item.id
                                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                                        else -> false
                                    },
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                when (item) {
                                                    is SongItem -> playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = item.id),
                                                            item.toMediaMetadata()
                                                        ),
                                                    )
                                                    is AlbumItem -> {
                                                        try {
                                                            if (item.id.isNotEmpty()) {
                                                                navController.navigate("album/${item.id}")
                                                            } else {
                                                                Log.w("ArtistScreen", "Album ID is empty")
                                                                Toast.makeText(
                                                                    context,
                                                                    "Error: Invalid album ID",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("ArtistScreen", "Navigation error to album: ${item.id}", e)
                                                            Toast.makeText(
                                                                context,
                                                                "Navigation error",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                    is ArtistItem -> {
                                                        try {
                                                            if (item.id.isNotEmpty()) {
                                                                navController.navigate("artist/${item.id}")
                                                            } else {
                                                                Log.w("ArtistScreen", "Artist ID is empty")
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("ArtistScreen", "Navigation error to artist: ${item.id}", e)
                                                        }
                                                    }
                                                    is PlaylistItem -> {
                                                        try {
                                                            if (item.id.isNotEmpty()) {
                                                                navController.navigate("online_playlist/${item.id}")
                                                            } else {
                                                                Log.w("ArtistScreen", "Playlist ID is empty")
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("ArtistScreen", "Navigation error to playlist: ${item.id}", e)
                                                        }
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    when (item) {
                                                        is SongItem -> YouTubeSongMenu(
                                                            song = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                        is AlbumItem -> YouTubeAlbumMenu(
                                                            albumItem = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                        is ArtistItem -> YouTubeArtistMenu(
                                                            artist = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                        is PlaylistItem -> YouTubePlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                        .animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // TopAppBar con la UI exacta de Vivi
    TopAppBar(
        title = {
            if (!transparentAppBar)
                Text(artistPage?.artist?.title.orEmpty())
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(
                    painterResource(R.drawable.link),
                    contentDescription = null,
                )
            }
        },
        colors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}