/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.component.EmptyPlaceholder
import com.arturo254.opentune.viewmodels.DownloadItem
import com.arturo254.opentune.viewmodels.DownloadQueueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(
    navController: NavController,
    viewModel: DownloadQueueViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsState()
    val isPaused by viewModel.downloadsPaused.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.download_queue)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (downloads.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (isPaused) viewModel.resumeAll() else viewModel.pauseAll()
                            }
                        ) {
                            Icon(
                                painter = painterResource(if (isPaused) R.drawable.play else R.drawable.pause),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { viewModel.removeAll() }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyPlaceholder(
                    icon = R.drawable.downloading,
                    text = "No active downloads"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(downloads, key = { it.download.request.id }) { item ->
                    DownloadQueueItem(
                        item = item,
                        onRemove = { viewModel.removeDownload(item.download.request.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadQueueItem(
    item: DownloadItem,
    onRemove: () -> Unit,
) {
    val progress = item.download.percentDownloaded / 100f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "DownloadProgress")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.song?.song?.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            placeholder = painterResource(R.drawable.music_note)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val statusText = when (item.download.state) {
                Download.STATE_QUEUED -> "Queued"
                Download.STATE_DOWNLOADING -> "Downloading"
                Download.STATE_RESTARTING -> "Restarting"
                Download.STATE_FAILED -> "Failed"
                Download.STATE_STOPPED -> "Paused"
                else -> ""
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (item.download.state == Download.STATE_DOWNLOADING || item.download.state == Download.STATE_STOPPED) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Remove",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
