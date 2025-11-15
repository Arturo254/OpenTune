package com.arturo254.opentune.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AlbumThumbnailSize
import com.arturo254.opentune.constants.ThumbnailCornerRadius
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.playback.queues.LocalAlbumRadio
import com.arturo254.opentune.ui.component.AutoResizeText
import com.arturo254.opentune.ui.component.FontSizeRange
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.NavigationTitle
import com.arturo254.opentune.ui.component.SongListItem
import com.arturo254.opentune.ui.component.YouTubeGridItem
import com.arturo254.opentune.ui.component.shimmer.ButtonPlaceholder
import com.arturo254.opentune.ui.component.shimmer.ListItemPlaceHolder
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.SelectionSongMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.utils.ItemWrapper
import com.arturo254.opentune.viewmodels.AlbumViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()

    val wrappedSongs = albumWithSongs?.songs?.map { item -> ItemWrapper(item) }?.toMutableList()
    var selection by remember {
        mutableStateOf(false)
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AlbumThumbnailSize)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                        ) {
                            // Mostrar la imagen del álbum
                            AsyncImage(
                                model = albumWithSongs.album.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Botón de descarga superpuesto
                            IconButton(
                                onClick = {
                                    // Lanza una coroutine para guardar la imagen
                                    CoroutineScope(Dispatchers.IO).launch {
                                        albumWithSongs.album.thumbnailUrl?.let {
                                            saveAlbumImageToGallery(
                                                context,
                                                it,
                                                albumWithSongs.album.title
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd) // Ubicar el botón en la esquina inferior derecha
                                    .padding(8.dp) // Margen desde el borde
                                    .size(40.dp) // Hacerlo más pequeño
                                    .clip(RoundedCornerShape(24.dp)) // Bordes redondeados
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) // Fondo semitransparente
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = stringResource(R.string.icon_save_img),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer // Color del ícono
                                )
                            }
                        }


                        Spacer(Modifier.width(16.dp))

                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            AutoResizeText(
                                text = albumWithSongs.album.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSizeRange = FontSizeRange(16.sp, 22.sp),
                            )

                            val annotatedString =
                                buildAnnotatedString {
                                    withStyle(
                                        style =
                                            MaterialTheme.typography.titleMedium
                                                .copy(
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                ).toSpanStyle(),
                                    ) {
                                        albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                            pushStringAnnotation(artist.id, artist.name)
                                            append(artist.name)
                                            pop()
                                            if (index != albumWithSongs.artists.lastIndex) {
                                                append(", ")
                                            }
                                        }
                                    }
                                }
                            ClickableText(annotatedString) { offset ->
                                annotatedString.getStringAnnotations(offset, offset).firstOrNull()
                                    ?.let { range ->
                                        navController.navigate("artist/${range.tag}")
                                    }
                            }

                            if (albumWithSongs.album.year != null) {
                                Text(
                                    text = albumWithSongs.album.year.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        database.query {
                                            update(albumWithSongs.album.toggleLike())
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (albumWithSongs.album.bookmarkedAt !=
                                                    null
                                                ) {
                                                    R.drawable.favorite
                                                } else {
                                                    R.drawable.favorite_border
                                                },
                                            ),
                                        contentDescription = null,
                                        tint =
                                            if (albumWithSongs.album.bookmarkedAt !=
                                                null
                                            ) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                LocalContentColor.current
                                            },
                                    )
                                }

                                when (downloadState) {
                                    Download.STATE_COMPLETED -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
                                                    DownloadService.sendRemoveDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        song.id,
                                                        false,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                            )
                                        }
                                    }

                                    Download.STATE_DOWNLOADING -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
                                                    DownloadService.sendRemoveDownload(
                                                        context,
                                                        ExoDownloadService::class.java,
                                                        song.id,
                                                        false,
                                                    )
                                                }
                                            },
                                        ) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }

                                    else -> {
                                        IconButton(
                                            onClick = {
                                                albumWithSongs.songs.forEach { song ->
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
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            AlbumMenu(
                                                originalAlbum = Album(
                                                    albumWithSongs.album,
                                                    albumWithSongs.artists
                                                ),
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs),
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
                            Text(
                                text = stringResource(R.string.play),
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                playerConnection.service.getAutomix(playlistId)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs.copy(songs = albumWithSongs.songs.shuffled())),
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

            if (wrappedSongs != null) {
                itemsIndexed(
                    items = wrappedSongs,
                    key = { _, song -> song.item.id },
                ) { index, songWrapper ->
                    SongListItem(
                        song = songWrapper.item,
                        albumIndex = index + 1,
                        isActive = songWrapper.item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
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
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(
                                                        albumWithSongs,
                                                        startIndex = index
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
                                        songWrapper.isSelected = true // Select the current item
                                    },
                                ),
                    )
                }
            }

            if (otherVersions.isNotEmpty()) {
                item {
                    NavigationTitle(
                        title = stringResource(R.string.other_versions),
                    )
                }
                item {
                    LazyRow {
                        items(
                            items = otherVersions,
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                    Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
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
                }
            }
        } else {
            item {
                ShimmerHost {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .size(AlbumThumbnailSize)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .background(MaterialTheme.colorScheme.onSurface),
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(
                                verticalArrangement = Arrangement.Center,
                            ) {
                                TextPlaceholder()
                                TextPlaceholder()
                                TextPlaceholder()
                            }
                        }

                        Spacer(Modifier.padding(8.dp))

                        Row {
                            ButtonPlaceholder(Modifier.weight(1f))

                            Spacer(Modifier.width(12.dp))

                            ButtonPlaceholder(Modifier.weight(1f))
                        }
                    }

                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    TopAppBar(
        title = {
            if (selection) {
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            } else {
                Text(
                    text = albumWithSongs?.album?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (selection) {
                        selection = false
                    } else {
                        navController.navigateUp()
                    }
                },
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
                val count = wrappedSongs?.count { it.isSelected } ?: 0
                IconButton(
                    onClick = {
                        if (count == wrappedSongs?.size) {
                            wrappedSongs.forEach { it.isSelected = false }
                        } else {
                            wrappedSongs?.forEach { it.isSelected = true }
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            if (count == wrappedSongs?.size) R.drawable.deselect else R.drawable.select_all
                        ),
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs?.filter { it.isSelected }!!
                                    .map { it.item },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false }
                            )
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            } else {
            }
        }
    )
}

suspend fun saveAlbumImageToGallery(context: Context, imageUrl: String, albumTitle: String) {
    try {
        // Cargar la imagen usando Coil
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        val drawable = context.imageLoader.execute(request).drawable

        if (drawable != null) {
            val bitmap = drawable.toBitmap()

            // Usar el título del álbum para el nombre del archivo
            val displayName = "${albumTitle.replace(" ", "_")}.png"
            val mimeType = "image/png" // Tipo MIME para PNG


            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // Mostrar un mensaje de éxito
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.image_save_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                context.getString(R.string.image_save_failure),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}