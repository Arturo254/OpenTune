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
import com.arturo254.opentune.MainActivity
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

    private val notificationUpdateListener = object : DownloadManager.Listener {
        override fun onDownloadsPausedChanged(downloadManager: DownloadManager, downloadsPaused: Boolean) {
            updateNotification()
        }

        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
            updateNotification()
        }

        override fun onIdle(downloadManager: DownloadManager) {
            updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        downloadManager.addListener(notificationUpdateListener)
    }

    override fun onDestroy() {
        downloadManager.removeListener(notificationUpdateListener)
        super.onDestroy()
    }

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
                updateNotification()
            }
            RESUME_DOWNLOADS -> {
                downloadManager.resumeDownloads()
                updateNotification()
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

        val activeDownload = downloads.find { it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_RESTARTING }
            ?: downloads.firstOrNull()

        val title = activeDownload?.let {
            val songName = Util.fromUtf8Bytes(it.request.data)
            if (downloads.size > 1) {
                "$songName (+${downloads.size - 1})"
            } else {
                songName
            }
        } ?: resources.getString(R.string.downloading)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_DOWNLOAD_QUEUE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder.recoverBuilder(
            this, notificationHelper.buildProgressNotification(
                this,
                R.drawable.downloading,
                contentIntent,
                title,
                downloads,
                notMetRequirements
            )
        )

        // 1. Pause/Resume Button
        val isPaused = downloadManager.downloadsPaused || downloads.all { it.state == Download.STATE_QUEUED || it.state == Download.STATE_RESTARTING }
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
        val cancelAction = activeDownload?.let { download ->
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

    private fun updateNotification() {
        val downloads = downloadManager.currentDownloads
        if (downloads.isNotEmpty()) {
            val notification = getForegroundNotification(downloads.toMutableList(), 0)
            startForeground(NOTIFICATION_ID, notification)
        }
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
        const val ACTION_DOWNLOAD_QUEUE = "com.arturo254.opentune.action.DOWNLOAD_QUEUE"
    }
}
