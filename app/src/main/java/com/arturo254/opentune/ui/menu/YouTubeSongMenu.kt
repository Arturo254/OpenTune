package com.arturo254.opentune.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.SongItem
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.ListThumbnailSize
import com.arturo254.opentune.constants.ThumbnailCornerRadius
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.ListDialog
import com.arturo254.opentune.ui.component.ListItem
import com.arturo254.opentune.ui.component.MenuItemData
import com.arturo254.opentune.ui.component.MenuGroup
import com.arturo254.opentune.ui.component.NewAction
import com.arturo254.opentune.ui.component.NewActionGrid
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val artists =
        remember {
            song.artists.mapNotNull {
                it.id?.let { artistId ->
                    MediaMetadata.Artist(id = artistId, name = it.name)
                }
            }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(song.toMediaMetadata())
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier
                                .fillParentMaxWidth()
                                .height(ListItemHeight)
                                .clickable {
                                    navController.navigate("artist/${artist.id}")
                                    showSelectArtistDialog = false
                                    onDismiss()
                                }
                                .padding(horizontal = 24.dp),
                    ) {
                        Text(
                            text = artist.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    ListItem(
        title = song.title,
        subtitle =
            joinByBullet(
                song.artists.joinToString { it.name },
                song.duration?.let { makeTimeString(it * 1000L) },
            ),
        thumbnailContent = {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    database.transaction {
                        librarySong.let { librarySong ->
                            if (librarySong == null) {
                                insert(song.toMediaMetadata(), SongEntity::toggleLike)
                            } else {
                                update(librarySong.song.toggleLike())
                            }
                        }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (librarySong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
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
                actions = listOf(
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
                            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            onDismiss()
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, song.shareLink)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                            onDismiss()
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        // Grupo: Ver artista y álbum
        item {
            MenuGroup(
                items = buildList {
                    if (artists.isNotEmpty()) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.view_artist)) },
                                description = {
                                    Text(
                                        text = artists.joinToString { it.name },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.artist),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    if (artists.size == 1) {
                                        navController.navigate("artist/${artists[0].id}")
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                }
                            )
                        )
                    }

                    song.album?.let { album ->
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.view_album)) },
                                description = {
                                    Text(
                                        text = album.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    navController.navigate("album/${album.id}")
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

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
                            playerConnection.playNext(song.toMediaItem())
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
                            playerConnection.addToQueue((song.toMediaItem()))
                            onDismiss()
                        }
                    )
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Grupo: Descarga
        item {
            MenuGroup(
                items = listOf(
                    when (download?.state) {
                        androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_download)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            )
                        }

                        androidx.media3.exoplayer.offline.Download.STATE_QUEUED,
                        androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.downloading)) },
                                icon = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
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
                                    database.transaction {
                                        insert(song.toMediaMetadata())
                                    }
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            )
                        }
                    }
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Grupo: Biblioteca
        item {
            MenuGroup(
                items = listOf(
                    if (librarySong?.song?.inLibrary != null) {
                        MenuItemData(
                            title = { Text(text = stringResource(R.string.remove_from_library)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.library_add_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                database.query {
                                    inLibrary(song.id, null)
                                }
                            }
                        )
                    } else {
                        MenuItemData(
                            title = { Text(text = stringResource(R.string.add_to_library)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.library_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                database.transaction {
                                    insert(song.toMediaMetadata())
                                    inLibrary(song.id, LocalDateTime.now())
                                }
                            }
                        )
                    }
                )
            )
        }
    }
}