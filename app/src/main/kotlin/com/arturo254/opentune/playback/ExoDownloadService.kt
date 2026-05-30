/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.arturo254.opentune.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExoDownloadService : DownloadService(
    NOTIFICATION_ID,
    1000L,
    CHANNEL_ID,
    R.string.downloading,
    0
) {
    @Inject
    lateinit var downloadUtil: DownloadUtil

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            REMOVE_ALL_PENDING_DOWNLOADS -> {
                downloadManager.currentDownloads.forEach { download ->
                    downloadManager.removeDownload(download.request.id)
                }
            }
            REMOVE_DOWNLOAD -> {
                intent.getStringExtra(EXTRA_DOWNLOAD_ID)?.let { id ->
                    downloadManager.removeDownload(id)
                }
            }
            PAUSE_DOWNLOADS -> {
                downloadManager.pauseDownloads()
            }
            RESUME_DOWNLOADS -> {
                downloadManager.resumeDownloads()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun getDownloadManager() = downloadUtil.downloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        val notificationHelper = downloadUtil.downloadNotificationHelper
        val firstDownload = downloads.firstOrNull()

        val title = if (downloads.size == 1 && firstDownload != null) {
            Util.fromUtf8Bytes(firstDownload.request.data)
        } else {
            resources.getQuantityString(R.plurals.n_song, downloads.size, downloads.size)
        }

        val builder = Notification.Builder.recoverBuilder(
            this, notificationHelper.buildProgressNotification(
                this,
                R.drawable.download,
                null,
                title,
                downloads,
                notMetRequirements
            )
        )

        // 1. Pause/Resume Button
        val isPaused = downloads.all { it.state == Download.STATE_QUEUED || it.state == Download.STATE_RESTARTING } || downloadManager.downloadsPaused
        val pauseResumeAction = if (isPaused) {
            Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.play),
                getString(R.string.exo_download_resume),
                PendingIntent.getService(
                    this, 1,
                    Intent(this, ExoDownloadService::class.java).setAction(RESUME_DOWNLOADS),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ).build()
        } else {
            Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.pause),
                getString(R.string.exo_download_pause),
                PendingIntent.getService(
                    this, 2,
                    Intent(this, ExoDownloadService::class.java).setAction(PAUSE_DOWNLOADS),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ).build()
        }

        // 2. Cancel Button (Current/First)
        val cancelAction = firstDownload?.let { download ->
            Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.close),
                getString(R.string.action_cancel),
                PendingIntent.getService(
                    this, 3,
                    Intent(this, ExoDownloadService::class.java)
                        .setAction(REMOVE_DOWNLOAD)
                        .putExtra(EXTRA_DOWNLOAD_ID, download.request.id),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).build()
        }

        // 3. Cancel All Button
        val cancelAllAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.close),
            getString(R.string.action_cancel_all),
            PendingIntent.getService(
                this, 4,
                Intent(this, ExoDownloadService::class.java).setAction(REMOVE_ALL_PENDING_DOWNLOADS),
                PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        builder.setActions(pauseResumeAction)
        cancelAction?.let { builder.addAction(it) }
        builder.addAction(cancelAllAction)

        return builder.build()
    }

    /**
     * This helper will outlive the lifespan of a single instance of [ExoDownloadService]
     */
    class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        private var nextNotificationId: Int,
    ) : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) {
            if (download.state == Download.STATE_FAILED) {
                val notification = notificationHelper.buildDownloadFailedNotification(
                    context,
                    R.drawable.error,
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "download"
        const val NOTIFICATION_ID = 1
        const val JOB_ID = 1
        const val REMOVE_ALL_PENDING_DOWNLOADS = "REMOVE_ALL_PENDING_DOWNLOADS"
        const val REMOVE_DOWNLOAD = "REMOVE_DOWNLOAD"
        const val PAUSE_DOWNLOADS = "PAUSE_DOWNLOADS"
        const val RESUME_DOWNLOADS = "RESUME_DOWNLOADS"
        const val EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID"
    }
}
