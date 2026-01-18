package com.arturo254.opentune.ui.menu

import android.annotation.SuppressLint
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ArtistSongSortType
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.ArtistListItem
import com.arturo254.opentune.ui.component.MenuItemData
import com.arturo254.opentune.ui.component.MenuGroup
import com.arturo254.opentune.ui.component.NewAction
import com.arturo254.opentune.ui.component.NewActionGrid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist

    ArtistListItem(
        artist = artist,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    database.transaction {
                        update(artist.artist.toggleLike())
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (artist.artist.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
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
        if (artist.songCount > 0) {
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
                                    coroutineScope.launch {
                                        val songs =
                                            withContext(Dispatchers.IO) {
                                                database
                                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                                    .first()
                                                    .map { it.toMediaItem() }
                                            }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = artist.artist.name,
                                                items = songs,
                                            ),
                                        )
                                    }
                                    onDismiss()
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
                                    coroutineScope.launch {
                                        val songs =
                                            withContext(Dispatchers.IO) {
                                                database
                                                    .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                                    .first()
                                                    .map { it.toMediaItem() }
                                                    .shuffled()
                                            }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = artist.artist.name,
                                                items = songs,
                                            ),
                                        )
                                    }
                                    onDismiss()
                                }
                            )
                        )

                        if (artist.artist.isYouTubeArtist) {
                            add(
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
                                        onDismiss()
                                        val intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://music.youtube.com/channel/${artist.id}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    }
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
                )
            }
        } else if (artist.artist.isYouTubeArtist) {
            // Solo mostrar share si no hay canciones
            item {
                MenuGroup(
                    items = listOf(
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
                                onDismiss()
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/channel/${artist.id}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        )
                    )
                )
            }
        }
    }
}