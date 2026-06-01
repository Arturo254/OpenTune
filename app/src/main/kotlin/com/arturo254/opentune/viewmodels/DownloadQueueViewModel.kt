/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.playback.DownloadUtil
import com.arturo254.opentune.playback.ExoDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import androidx.media3.exoplayer.offline.DownloadManager
import javax.inject.Inject

data class DownloadItem(
    val download: Download,
    val song: Song?,
    val title: String,
)

@HiltViewModel
class DownloadQueueViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadUtil: DownloadUtil,
    database: MusicDatabase,
) : ViewModel() {

    val downloads = combine(
        downloadUtil.downloads,
        database.allSongs()
    ) { downloadMap, allSongs ->
        val songMap = allSongs.associateBy { it.id }
        downloadMap.values
            .filter { it.state != Download.STATE_COMPLETED }
            .map { download ->
                DownloadItem(
                    download = download,
                    song = songMap[download.request.id],
                    title = Util.fromUtf8Bytes(download.request.data)
                )
            }
            .sortedBy { it.download.startTimeMs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadsPaused = callbackFlow {
        val listener = object : DownloadManager.Listener {
            override fun onDownloadsPausedChanged(downloadManager: DownloadManager, downloadsPaused: Boolean) {
                trySend(downloadsPaused)
            }
        }
        downloadUtil.downloadManager.addListener(listener)
        trySend(downloadUtil.downloadManager.downloadsPaused)
        awaitClose { downloadUtil.downloadManager.removeListener(listener) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), downloadUtil.downloadManager.downloadsPaused)

    fun pauseAll() {
        val intent = Intent(context, ExoDownloadService::class.java).setAction(ExoDownloadService.PAUSE_DOWNLOADS)
        context.startService(intent)
    }

    fun resumeAll() {
        val intent = Intent(context, ExoDownloadService::class.java).setAction(ExoDownloadService.RESUME_DOWNLOADS)
        context.startService(intent)
    }

    fun removeDownload(id: String) {
        DownloadService.sendRemoveDownload(
            context,
            ExoDownloadService::class.java,
            id,
            false
        )
    }

    fun removeAll() {
        val intent = Intent(context, ExoDownloadService::class.java).setAction(ExoDownloadService.REMOVE_ALL_PENDING_DOWNLOADS)
        context.startService(intent)
    }
}
