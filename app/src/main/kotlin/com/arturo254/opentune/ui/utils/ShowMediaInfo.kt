/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.utils

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.MediaInfo
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard

@Composable
fun ShowMediaInfo(videoId: String) {

    if (videoId.isBlank()) return

    val windowInsets = WindowInsets.systemBars
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current

    var info by remember { mutableStateOf<MediaInfo?>(null) }
    var song by remember { mutableStateOf<Song?>(null) }
    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    LaunchedEffect(videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }

    LaunchedEffect(videoId) {
        database.song(videoId).collect { song = it }
    }

    LaunchedEffect(videoId) {
        database.format(videoId).collect { currentFormat = it }
    }

    fun copy(text: String) {
        val cm =
            context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("text", text))
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(windowInsets.asPaddingValues())
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        /* ============================================================
         * DETAILS SECTION
         * ============================================================ */

        if (song != null) {

            item {
                SectionTitle(
                    icon = R.drawable.info,
                    title = stringResource(R.string.details)
                )
            }

            item {

                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(Modifier.padding(20.dp)) {

                        val baseList = listOf(
                            Triple(R.drawable.music_note, stringResource(R.string.song_title), song?.title),
                            Triple(R.drawable.person, stringResource(R.string.song_artists),
                                song?.artists?.joinToString { it.name }),
                            Triple(R.drawable.tag, stringResource(R.string.media_id), song?.id)
                        )

                        val extendedList = baseList + if (currentFormat != null) {
                            listOf(
                                Triple(R.drawable.code, "Itag", currentFormat?.itag?.toString()),
                                Triple(R.drawable.memory, stringResource(R.string.mime_type), currentFormat?.mimeType),
                                Triple(R.drawable.tune, stringResource(R.string.codecs), currentFormat?.codecs),
                                Triple(R.drawable.graphic_eq, stringResource(R.string.bitrate),
                                    currentFormat?.bitrate?.let { "${it / 1000} Kbps" }),
                                Triple(R.drawable.equalizer, stringResource(R.string.sample_rate),
                                    currentFormat?.sampleRate?.let { "$it Hz" }),
                                Triple(R.drawable.volume_up, stringResource(R.string.volume),
                                    "${(playerConnection?.player?.volume?.times(100))?.toInt()}%"),
                                Triple(
                                    R.drawable.folder,
                                    stringResource(R.string.file_size),
                                    currentFormat?.contentLength?.let {
                                        Formatter.formatShortFileSize(context, it)
                                    }
                                )
                            )
                        } else emptyList()

                        extendedList.forEach { (icon, label, value) ->

                            val text = value ?: stringResource(R.string.unknown)

                            MediaRow(
                                icon = icon,
                                label = label,
                                value = text,
                                onClick = { copy(text) }
                            )
                        }
                    }
                }
            }
        }

        /* ============================================================
         * MEDIA INFO
         * ============================================================ */

        item {
            Spacer(Modifier.height(24.dp))
            SectionTitle(
                icon = R.drawable.description,
                title = stringResource(R.string.information)
            )
        }

        if (info != null) {

            if (song == null) {
                item {
                    InfoBlock(
                        icon = R.drawable.music_note,
                        label = "",
                        text = info?.title ?: ""
                    )
                }
            }

            item {
                InfoBlock(
                    icon = R.drawable.person,
                    label = stringResource(R.string.artists),
                    text = info?.author ?: ""
                )
            }

            item {
                InfoBlock(
                    icon = R.drawable.description,
                    label = stringResource(R.string.description),
                    text = info?.description ?: ""
                )
            }

            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        StatColumn(
                            R.drawable.group,
                            stringResource(R.string.subscribers),
                            info?.subscribers ?: ""
                        )

                        StatColumn(
                            R.drawable.visibility,
                            stringResource(R.string.views),
                            info?.viewCount?.toInt()?.let { numberFormatter(it) } ?: ""
                        )

                        StatColumn(
                            R.drawable.thumbup,
                            stringResource(R.string.likes),
                            info?.like?.toInt()?.let { numberFormatter(it) } ?: ""
                        )

                        StatColumn(
                            R.drawable.thumbdown,
                            stringResource(R.string.dislikes),
                            info?.dislike?.toInt()?.let { numberFormatter(it) } ?: ""
                        )
                    }
                }
            }

        } else {

            item {
                ShimmerHost {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextPlaceholder()
                    }
                }
            }
        }
    }
}

/* ============================================================
 * COMPONENTS (EXPRESSIVE STYLE)
 * ============================================================ */

@Composable
private fun SectionTitle(icon: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MediaRow(
    icon: Int,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(Modifier.padding(vertical = 6.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(start = 24.dp, top = 4.dp)
        )
    }
}

@Composable
private fun InfoBlock(icon: Int, label: String, text: String) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(8.dp))

            if (label.isNotEmpty()) {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }

        BasicText(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(start = 28.dp, top = 6.dp)
        )
    }
}

@Composable
private fun StatColumn(icon: Int, label: String, value: String) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(6.dp))

        Text(label, style = MaterialTheme.typography.labelMedium)

        Spacer(Modifier.height(4.dp))

        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}