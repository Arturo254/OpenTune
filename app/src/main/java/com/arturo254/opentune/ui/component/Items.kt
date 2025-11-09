@file:OptIn(ExperimentalFoundationApi::class)

package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.arturo254.innertube.YouTube
import com.arturo254.innertube.models.AlbumItem
import com.arturo254.innertube.models.ArtistItem
import com.arturo254.innertube.models.PlaylistItem
import com.arturo254.innertube.models.SongItem
import com.arturo254.innertube.models.YTItem
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.GridThumbnailHeight
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.ListThumbnailSize
import com.arturo254.opentune.constants.SmallGridThumbnailHeight
import com.arturo254.opentune.constants.ThumbnailCornerRadius
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.queues.LocalAlbumRadio
import com.arturo254.opentune.ui.theme.extractThemeColor
import com.arturo254.opentune.utils.getPlaylistImageUri
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==================== CONSTANTES ====================
internal object ItemDimensions {
    val GridCornerRadius = 27.dp
    val PlayButtonSize = 36.dp
    val SmallPlayButtonSize = 24.dp
    val IconSize = 18.dp
    val SmallIconSize = 16.dp
    val ProgressStrokeWidth = 2.dp
    val ItemPadding = 12.dp
    val InnerPadding = 6.dp
    val SmallPadding = 8.dp
    val IconEndPadding = 2.dp
    val PlaylistCardCorner = 25.dp
}

internal object ItemAlpha {
    const val OverlayDark = 0.4f
    const val OverlaySemiDark = 0.5f
    const val OverlayMedium = 0.6f
    const val ContentAlpha = 0.8f
}

// ==================== COMPONENTES REUTILIZABLES ====================

@Composable
private fun ThumbnailImage(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(ThumbnailCornerRadius),
    contentScale: ContentScale = ContentScale.Fit,
    onState: (AsyncImagePainter.State) -> Unit = {}
) {
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = contentScale,
        onState = onState,
        modifier = modifier.clip(shape)
    )
}

@Composable
private fun PlayButton(
    onClick: () -> Unit = {},
    size: Dp = ItemDimensions.PlayButtonSize,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = ItemAlpha.OverlayMedium))
            .clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(R.drawable.play),
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun DownloadStateIcon(
    downloadState: Int?,
    modifier: Modifier = Modifier
) {
    when (downloadState) {
        STATE_COMPLETED -> Icon(
            painter = painterResource(R.drawable.offline),
            contentDescription = null,
            modifier = modifier
                .size(ItemDimensions.IconSize)
                .padding(end = ItemDimensions.IconEndPadding)
        )
        STATE_QUEUED, STATE_DOWNLOADING -> CircularProgressIndicator(
            strokeWidth = ItemDimensions.ProgressStrokeWidth,
            modifier = modifier
                .size(ItemDimensions.SmallIconSize)
                .padding(end = ItemDimensions.IconEndPadding)
        )
    }
}

@Composable
private fun BadgeIcon(
    icon: Int,
    tint: Color = LocalContentColor.current,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(ItemDimensions.IconSize)
            .padding(end = ItemDimensions.IconEndPadding)
    )
}

@Composable
private fun StandardBadges(
    isLiked: Boolean = false,
    inLibrary: Boolean = false,
    isExplicit: Boolean = false,
    downloadState: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true
) {
    if (showLikedIcon && isLiked) {
        BadgeIcon(R.drawable.favorite, MaterialTheme.colorScheme.error)
    }
    if (showInLibraryIcon && inLibrary) {
        BadgeIcon(R.drawable.library_add_check)
    }
    if (isExplicit) {
        BadgeIcon(R.drawable.explicit)
    }
    if (showDownloadIcon) {
        DownloadStateIcon(downloadState)
    }
}

@Composable
private fun rememberAlbumDownloadState(albumId: String, songs: List<Song>): Int {
    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState = when {
                songs.all { downloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                songs.all {
                    downloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING, STATE_COMPLETED)
                } -> STATE_DOWNLOADING
                else -> Download.STATE_STOPPED
            }
        }
    }

    return downloadState
}

@Composable
private fun SelectionOverlay(
    isSelected: Boolean,
    shape: Shape = RoundedCornerShape(ThumbnailCornerRadius),
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .zIndex(1000f)
                .clip(shape)
                .background(Color.Black.copy(alpha = ItemAlpha.OverlaySemiDark))
        ) {
            Icon(
                painter = painterResource(R.drawable.done),
                modifier = Modifier.align(Alignment.Center),
                contentDescription = null
            )
        }
    }
}

// ==================== COMPONENTES BASE ====================

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isActive) {
            modifier
                .height(ListItemHeight)
                .padding(horizontal = ItemDimensions.SmallPadding)
                .clip(RoundedCornerShape(ItemDimensions.SmallPadding))
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            modifier
                .height(ListItemHeight)
                .padding(horizontal = ItemDimensions.SmallPadding)
        }
    ) {
        Box(
            modifier = Modifier.padding(ItemDimensions.InnerPadding),
            contentAlignment = Alignment.Center
        ) {
            thumbnailContent()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ItemDimensions.InnerPadding)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .basicMarquee()
                    .fillMaxWidth()
            )

            if (subtitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    subtitle()
                }
            }
        }

        trailingContent()
    }
}


@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) = ListItem(
    title = title,
    subtitle = {
        badges()
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier = if (fillMaxWidth) {
            modifier
                .padding(ItemDimensions.ItemPadding)
                .fillMaxWidth()
        } else {
            modifier
                .padding(ItemDimensions.ItemPadding)
                .width(GridThumbnailHeight * thumbnailRatio)
        }
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = if (fillMaxWidth) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.height(GridThumbnailHeight)
            }
                .aspectRatio(thumbnailRatio)
                .clip(RoundedCornerShape(ItemDimensions.GridCornerRadius))
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(ItemDimensions.InnerPadding))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .basicMarquee()
                .fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SmallGridItem(
    modifier: Modifier = Modifier,
    title: String,
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    isArtist: Boolean? = false,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (isArtist == true) Alignment.CenterHorizontally else Alignment.Start,
        modifier = modifier
            .fillMaxHeight()
            .width(GridThumbnailHeight * thumbnailRatio)
            .padding(ItemDimensions.ItemPadding)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .height(SmallGridThumbnailHeight)
                .aspectRatio(thumbnailRatio)
                .clip(RoundedCornerShape(ItemDimensions.SmallPadding))
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(ItemDimensions.InnerPadding))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(SmallGridThumbnailHeight)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .basicMarquee()
                    .fillMaxWidth()
            )
        }
    }
}

// ==================== SONG ITEMS ====================

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    isSelected: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
        StandardBadges(
            isLiked = song.song.liked,
            inLibrary = song.song.inLibrary != null,
            downloadState = download?.state,
            showLikedIcon = showLikedIcon,
            showInLibraryIcon = showInLibraryIcon,
            showDownloadIcon = showDownloadIcon
        )
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val subtitle = remember(song.artists, song.song.duration) {
        joinByBullet(
            song.artists.joinToString { it.name },
            makeTimeString(song.song.duration * 1000L)
        )
    }

    ListItem(
        title = song.song.title,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ListThumbnailSize)
            ) {
                if (albumIndex != null) {
                    AnimatedVisibility(
                        visible = !isActive,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.done),
                                modifier = Modifier.align(Alignment.Center),
                                contentDescription = null
                            )
                        } else {
                            Text(
                                text = albumIndex.toString(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else {
                    SelectionOverlay(isSelected)
                    ThumbnailImage(
                        model = song.song.thumbnailUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PlayingIndicatorBox(
                    isActive = isActive,
                    playWhenReady = isPlaying,
                    color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (albumIndex != null) Color.Transparent else Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                        )
                )
            }
        },
        trailingContent = trailingContent,
        modifier = modifier.semantics {
            contentDescription = "Canción: ${song.song.title}, Artistas: ${song.artists.joinToString { it.name }}"
            role = Role.Button
        },
        isActive = isActive
    )
}

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
        StandardBadges(
            isLiked = song.song.liked,
            inLibrary = song.song.inLibrary != null,
            downloadState = download?.state,
            showLikedIcon = showLikedIcon,
            showInLibraryIcon = showInLibraryIcon,
            showDownloadIcon = showDownloadIcon
        )
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val subtitle = remember(song.artists, song.song.duration) {
        joinByBullet(
            song.artists.joinToString { it.name },
            makeTimeString(song.song.duration * 1000L)
        )
    }

    GridItem(
        title = song.song.title,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            ThumbnailImage(
                model = song.song.thumbnailUrl,
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) ItemAlpha.OverlayDark else 0f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                        )
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(ItemDimensions.SmallPadding)
            ) {
                PlayButton()
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        fillMaxWidth = fillMaxWidth,
        modifier = modifier
    )
}

@Composable
fun SongSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = SmallGridItem(
    title = song.song.title,
    thumbnailContent = {
        ThumbnailImage(
            model = song.song.thumbnailUrl,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.Black.copy(alpha = if (isPlaying) ItemAlpha.OverlayDark else 0f),
                        shape = RoundedCornerShape(ThumbnailCornerRadius)
                    )
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !(isActive && isPlaying),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(ItemDimensions.SmallPadding)
        ) {
            PlayButton()
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    modifier = modifier
)

// ==================== ARTIST ITEMS ====================

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            BadgeIcon(R.drawable.favorite, MaterialTheme.colorScheme.error)
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount)

    ListItem(
        title = artist.artist.name,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            ThumbnailImage(
                model = artist.artist.thumbnailUrl,
                shape = CircleShape,
                modifier = Modifier.size(ListThumbnailSize)
            )
        },
        trailingContent = trailingContent,
        modifier = modifier
    )
}

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            BadgeIcon(R.drawable.favorite, MaterialTheme.colorScheme.error)
        }
    },
    fillMaxWidth: Boolean = false,
) {
    val subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount)

    GridItem(
        title = artist.artist.name,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            ThumbnailImage(
                model = artist.artist.thumbnailUrl,
                modifier = Modifier.fillMaxSize()
            )
        },
        thumbnailShape = CircleShape,
        fillMaxWidth = fillMaxWidth,
        modifier = modifier
    )
}

@Composable
fun ArtistSmallGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
) = SmallGridItem(
    title = artist.artist.name,
    thumbnailContent = {
        ThumbnailImage(
            model = artist.artist.thumbnailUrl,
            shape = CircleShape,
            modifier = Modifier.fillMaxSize()
        )
    },
    thumbnailShape = CircleShape,
    modifier = modifier,
    isArtist = true
)

// ==================== ALBUM ITEMS ====================

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        var songs by remember { mutableStateOf(emptyList<Song>()) }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect { songs = it }
        }

        val downloadState = rememberAlbumDownloadState(album.id, songs)

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            BadgeIcon(R.drawable.favorite, MaterialTheme.colorScheme.error)
        }
        DownloadStateIcon(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val subtitle = joinByBullet(
        album.artists.joinToString { it.name },
        pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
        album.album.year?.toString()
    )

    ListItem(
        title = album.album.title,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            val database = LocalDatabase.current
            val coroutineScope = rememberCoroutineScope()

            ThumbnailImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.album.thumbnailUrl)
                    .allowHardware(false)
                    .build(),
                onState = { state ->
                    if (album.album.themeColor == null && state is AsyncImagePainter.State.Success) {
                        coroutineScope.launch(Dispatchers.IO) {
                            state.result.drawable.toBitmapOrNull()?.extractThemeColor()?.toArgb()?.let { color ->
                                database.query { update(album.album.copy(themeColor = color)) }
                            }
                        }
                    }
                },
                modifier = Modifier.size(ListThumbnailSize)
            )

            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .background(
                        color = Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                        shape = RoundedCornerShape(ThumbnailCornerRadius)
                    )
            )
        },
        trailingContent = trailingContent,
        modifier = modifier
    )
}

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        var songs by remember { mutableStateOf(emptyList<Song>()) }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect { songs = it }
        }

        val downloadState = rememberAlbumDownloadState(album.id, songs)

        if (album.album.bookmarkedAt != null) {
            BadgeIcon(R.drawable.favorite, MaterialTheme.colorScheme.error)
        }
        DownloadStateIcon(downloadState)
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val subtitle = album.artists.joinToString { it.name }

    GridItem(
        title = album.album.title,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            ThumbnailImage(
                model = album.album.thumbnailUrl,
                modifier = Modifier.fillMaxSize()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                        )
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !isActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ItemDimensions.SmallPadding)
            ) {
                val database = LocalDatabase.current
                val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

                PlayButton(
                    onClick = {
                        coroutineScope.launch {
                            database.albumWithSongs(album.id).first()?.let { albumWithSongs ->
                                playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                            }
                        }
                    }
                )
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        fillMaxWidth = fillMaxWidth,
        modifier = modifier
    )
}

@Composable
fun AlbumSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = song.song.albumName?.let {
    SmallGridItem(
        title = it,
        thumbnailContent = {
            ThumbnailImage(
                model = song.song.thumbnailUrl,
                modifier = Modifier.fillMaxSize()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                        )
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        modifier = modifier
    )
}

// ==================== PLAYLIST ITEMS ====================

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
    autoPlaylist: Boolean = false,
) {
    val subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
        }
    }

    val painter = when (playlist.playlist.name) {
        stringResource(R.string.liked) -> R.drawable.favorite_border
        stringResource(R.string.offline) -> R.drawable.offline
        stringResource(R.string.cached_playlist) -> R.drawable.cached
        else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
    }

    ListItem(
        title = playlist.playlist.name,
        subtitle = subtitle,
        thumbnailContent = {
            when (playlist.thumbnails.size) {
                0 -> Box(
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ListThumbnailSize / 2)
                            .align(Alignment.Center)
                    )
                }
                1 -> ThumbnailImage(
                    model = playlist.thumbnails[0],
                    modifier = Modifier.size(ListThumbnailSize)
                )
                else -> Box(
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd
                    ).fastForEachIndexed { index, alignment ->
                        ThumbnailImage(
                            model = playlist.thumbnails.getOrNull(index),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier
                                .align(alignment)
                                .size(ListThumbnailSize / 2)
                        )
                    }
                }
            }
        },
        trailingContent = trailingContent,
        modifier = modifier
    )
}

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {},
    fillMaxWidth: Boolean = false,
    autoPlaylist: Boolean = false,
    context: Context
) {
    val subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
        }
    }

    GridItem(
        title = playlist.playlist.name,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            val thumbnailUri = getPlaylistImageUri(context, playlist.playlist.id)
            val width = maxWidth

            if (thumbnailUri != null) {
                ThumbnailImage(
                    model = thumbnailUri,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val painter = when (playlist.playlist.name) {
                    stringResource(R.string.liked) -> R.drawable.favorite_border
                    stringResource(R.string.offline) -> R.drawable.offline
                    stringResource(R.string.cached_playlist) -> R.drawable.cached
                    else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
                }

                when (playlist.thumbnails.size) {
                    0 -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clip(RoundedCornerShape(ItemDimensions.PlaylistCardCorner))
                    ) {
                        Icon(
                            painter = painterResource(painter),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = ItemAlpha.ContentAlpha),
                            modifier = Modifier
                                .size(width / 2)
                                .align(Alignment.Center)
                        )
                    }
                    1 -> ThumbnailImage(
                        model = playlist.thumbnails[0],
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> Box(
                        modifier = Modifier
                            .size(width)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    ) {
                        listOf(
                            Alignment.TopStart,
                            Alignment.TopEnd,
                            Alignment.BottomStart,
                            Alignment.BottomEnd
                        ).fastForEachIndexed { index, alignment ->
                            ThumbnailImage(
                                model = playlist.thumbnails.getOrNull(index),
                                contentScale = ContentScale.Crop,
                                shape = RoundedCornerShape(0.dp),
                                modifier = Modifier
                                    .align(alignment)
                                    .size(width / 2)
                            )
                        }
                    }
                }
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        fillMaxWidth = fillMaxWidth,
        modifier = modifier
    )
}

// ==================== MEDIA METADATA ITEMS ====================

@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val subtitle = joinByBullet(
        mediaMetadata.artists.joinToString { it.name },
        makeTimeString(mediaMetadata.duration * 1000L)
    )

    ListItem(
        title = mediaMetadata.title,
        subtitle = subtitle,
        thumbnailContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ListThumbnailSize)
            ) {
                SelectionOverlay(isSelected)

                ThumbnailImage(
                    model = mediaMetadata.thumbnailUrl,
                    modifier = Modifier.fillMaxWidth()
                )

                PlayingIndicatorBox(
                    isActive = isActive,
                    playWhenReady = isPlaying,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                        )
                )
            }
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive
    )
}

// ==================== YOUTUBE ITEMS ====================

@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)
        val downloads by LocalDownloadUtil.current.downloads.collectAsState()

        StandardBadges(
            isLiked = (item is SongItem && song?.song?.liked == true) ||
                    (item is AlbumItem && album?.album?.bookmarkedAt != null),
            inLibrary = item is SongItem && song?.song?.inLibrary != null,
            isExplicit = item.explicit,
            downloadState = if (item is SongItem) downloads[item.id]?.state else null,
            showInLibraryIcon = item is SongItem
        )
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val subtitle = when (item) {
        is SongItem -> joinByBullet(
            item.artists.joinToString { it.name },
            makeTimeString(item.duration?.times(1000L))
        )
        is AlbumItem -> joinByBullet(
            item.artists?.joinToString { it.name },
            item.year?.toString()
        )
        is ArtistItem -> null
        is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
    }

    ListItem(
        title = item.title,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ListThumbnailSize)
            ) {
                val thumbnailShape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)

                if (albumIndex != null) {
                    AnimatedVisibility(
                        visible = !isActive,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.done),
                                modifier = Modifier.align(Alignment.Center),
                                contentDescription = null
                            )
                        } else {
                            Text(
                                text = albumIndex.toString(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else {
                    SelectionOverlay(isSelected)
                    ThumbnailImage(
                        model = item.thumbnail,
                        shape = thumbnailShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PlayingIndicatorBox(
                    isActive = isActive,
                    playWhenReady = isPlaying,
                    color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (albumIndex != null) Color.Transparent else Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                            shape = thumbnailShape
                        )
                )
            }
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive
    )
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)
        val downloads by LocalDownloadUtil.current.downloads.collectAsState()

        StandardBadges(
            isLiked = (item is SongItem && song?.song?.liked == true) ||
                    (item is AlbumItem && album?.album?.bookmarkedAt != null),
            inLibrary = item is SongItem && song?.song?.inLibrary != null,
            isExplicit = item.explicit,
            downloadState = if (item is SongItem) downloads[item.id]?.state else null,
            showInLibraryIcon = item is SongItem,
            showDownloadIcon = item is SongItem
        )
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val thumbnailShape = if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)

    val subtitle = when (item) {
        is SongItem -> joinByBullet(
            item.artists.joinToString { it.name },
            makeTimeString(item.duration?.times(1000L))
        )
        is AlbumItem -> joinByBullet(
            item.artists?.joinToString { it.name },
            item.year?.toString()
        )
        is ArtistItem -> null
        is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
    }

    Column(
        modifier = if (fillMaxWidth) {
            modifier
                .padding(ItemDimensions.ItemPadding)
                .fillMaxWidth()
        } else {
            modifier
                .padding(ItemDimensions.ItemPadding)
                .width(GridThumbnailHeight * thumbnailRatio)
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(thumbnailRatio)
                .clip(thumbnailShape)
        ) {
            ThumbnailImage(
                model = item.thumbnail,
                modifier = Modifier.fillMaxWidth()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = item is AlbumItem && !isActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ItemDimensions.SmallPadding)
            ) {
                val database = LocalDatabase.current
                val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

                PlayButton(
                    onClick = {
                        var playlistId = ""
                        coroutineScope?.launch(Dispatchers.IO) {
                            var albumWithSongs = database.albumWithSongs(item.id).first()
                            if (albumWithSongs?.songs.isNullOrEmpty()) {
                                YouTube.album(item.id).onSuccess { albumPage ->
                                    playlistId = albumPage.album.playlistId
                                    database.transaction { insert(albumPage) }
                                    albumWithSongs = database.albumWithSongs(item.id).first()
                                }.onFailure { reportException(it) }
                            }
                            albumWithSongs?.let {
                                withContext(Dispatchers.Main) {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(LocalAlbumRadio(it))
                                }
                            }
                        }
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) ItemAlpha.OverlayDark else 0f),
                            shape = thumbnailShape
                        )
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = item is SongItem && !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(ItemDimensions.SmallPadding)
            ) {
                PlayButton()
            }
        }

        Spacer(modifier = Modifier.height(ItemDimensions.InnerPadding))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .basicMarquee(iterations = 3)
                .fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun YouTubeSmallGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = SmallGridItem(
    title = item.title,
    thumbnailContent = {
        ThumbnailImage(
            model = item.thumbnail,
            modifier = Modifier.fillMaxWidth()
        )

        if (item is SongItem) {
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) ItemAlpha.OverlayDark else 0f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                        )
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(ItemDimensions.SmallPadding)
            ) {
                PlayButton()
            }
        }
    },
    thumbnailShape = when (item) {
        is ArtistItem -> CircleShape
        else -> RoundedCornerShape(ThumbnailCornerRadius)
    },
    modifier = modifier,
    isArtist = item is ArtistItem
)

// ==================== LOCAL ITEMS ====================

@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ThumbnailCornerRadius))
        ) {
            ThumbnailImage(
                model = thumbnailUrl,
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                            shape = RoundedCornerShape(ThumbnailCornerRadius)
                        )
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(ItemDimensions.SmallPadding)
            ) {
                PlayButton()
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            ThumbnailImage(
                model = thumbnailUrl,
                shape = CircleShape,
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                            shape = CircleShape
                        )
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        ThumbnailImage(
            model = thumbnailUrl,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.Black.copy(alpha = ItemAlpha.OverlayDark),
                        shape = RoundedCornerShape(ThumbnailCornerRadius)
                    )
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(ItemDimensions.SmallPlayButtonSize)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)