package com.arturo254.opentune.playback

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.Timeline
import com.arturo254.opentune.MusicWidget.Companion.ACTION_STATE_CHANGED
import com.arturo254.opentune.MusicWidget.Companion.ACTION_UPDATE_PROGRESS
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.getCurrentQueueIndex
import com.arturo254.opentune.extensions.getQueueWindows
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.playback.MusicService.MusicBinder
import com.arturo254.opentune.playback.queues.Queue
import com.arturo254.opentune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    private val context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    scope: CoroutineScope,
) : Player.Listener {

    companion object {
        private const val TAG = "PlayerConnection"
        private const val PROGRESS_UPDATE_INTERVAL = 1000L
        private const val WIDGET_UPDATE_DEBOUNCE = 100L

        @Volatile
        var instance: PlayerConnection? = null
            private set
    }

    val service = binder.service
    val player = service.player

    // Estados básicos del reproductor
    private val _playbackState = MutableStateFlow(player.playbackState)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _playWhenReady = MutableStateFlow(player.playWhenReady)
    val playWhenReady: StateFlow<Boolean> = _playWhenReady.asStateFlow()

    // Estado combinado de reproducción
    val isPlaying = combine(playbackState, playWhenReady) { playbackState, playWhenReady ->
        playWhenReady && (playbackState == STATE_READY || playbackState == Player.STATE_BUFFERING)
    }.stateIn(
        scope,
        SharingStarted.Lazily,
        player.playWhenReady && player.playbackState == STATE_READY
    )

    // Estados de conexión y salud del reproductor
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Metadatos y información de la canción actual
    private val _mediaMetadata = MutableStateFlow(player.currentMetadata)
    val mediaMetadata: StateFlow<com.arturo254.opentune.models.MediaMetadata?> =
        _mediaMetadata.asStateFlow()

    val currentSong = mediaMetadata.flatMapLatest { metadata ->
        database.song(metadata?.id)
    }

    val currentLyrics = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.lyrics(mediaMetadata?.id)
    }

    val currentFormat = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.format(mediaMetadata?.id)
    }

    // Estados de la cola de reproducción
    private val _queueTitle = MutableStateFlow<String?>(service.queueTitle)
    val queueTitle: StateFlow<String?> = _queueTitle.asStateFlow()

    private val _queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val queueWindows: StateFlow<List<Timeline.Window>> = _queueWindows.asStateFlow()

    private val _currentMediaItemIndex = MutableStateFlow(-1)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

    private val _currentWindowIndex = MutableStateFlow(-1)
    val currentWindowIndex: StateFlow<Int> = _currentWindowIndex.asStateFlow()

    // Estados de control
    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Estados de navegación
    private val _canSkipPrevious = MutableStateFlow(true)
    val canSkipPrevious: StateFlow<Boolean> = _canSkipPrevious.asStateFlow()

    private val _canSkipNext = MutableStateFlow(true)
    val canSkipNext: StateFlow<Boolean> = _canSkipNext.asStateFlow()

    // Estados de progreso y posición
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    // Estados de audio y volumen
    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // Estados de favoritos y valoraciones
    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    // Estados de error
    private val _error = MutableStateFlow<PlaybackException?>(null)
    val error: StateFlow<PlaybackException?> = _error.asStateFlow()

    // Control de actualizaciones
    private val updateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private var progressUpdateRunnable: Runnable? = null
    private val isUpdatingProgress = AtomicBoolean(false)
    private val widgetUpdateHandler = Handler(Looper.getMainLooper())
    private var pendingWidgetUpdate: Runnable? = null

    // Estados previos para detectar cambios
    private var lastPlaybackState: Int = player.playbackState
    private var lastPlayWhenReady: Boolean = player.playWhenReady
    private var lastMediaItemIndex: Int = player.currentMediaItemIndex
    private var lastPosition: Long = 0L

    init {
        Log.d(TAG, "Initializing PlayerConnection")

        player.addListener(this)
        initializeStates()
        startProgressUpdates()

        instance = this

        // Listener adicional para actualizaciones del widget
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                handlePlayerEvents(player, events)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "Playback state changed to: $playbackState")
                updateConnectionState(playbackState)
            }
        })

        Log.d(TAG, "PlayerConnection initialized successfully")
    }

    private fun initializeStates() {
        try {
            _playbackState.value = player.playbackState
            _playWhenReady.value = player.playWhenReady
            _mediaMetadata.value = player.currentMetadata
            _queueTitle.value = service.queueTitle
            _queueWindows.value = player.getQueueWindows()
            _currentWindowIndex.value = player.getCurrentQueueIndex()
            _currentMediaItemIndex.value = player.currentMediaItemIndex
            _shuffleModeEnabled.value = player.shuffleModeEnabled
            _repeatMode.value = player.repeatMode
            _currentPosition.value = player.currentPosition
            _duration.value = player.duration
            _bufferedPosition.value = player.bufferedPosition
            _volume.value = player.volume

            // Inicializar estado de like
            CoroutineScope(Dispatchers.IO).launch {
                updateLikeStatusForCurrentSong()
            }

            updateCanSkipPreviousAndNext()
            Log.d(TAG, "States initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing states", e)
            reportException(e)
        }
    }


    fun refreshLikeStatus() {
        updateScope.launch(Dispatchers.IO) {
            updateLikeStatusForCurrentSong()
        }

    }

    private fun handlePlayerEvents(player: Player, events: Player.Events) {
        var shouldUpdateWidget = false

        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            shouldUpdateWidget = true
        }

        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
            shouldUpdateWidget = true
        }

        if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
            events.contains(Player.EVENT_TIMELINE_CHANGED)
        ) {
            shouldUpdateWidget = true
        }

        if (shouldUpdateWidget) {
            scheduleWidgetUpdate()
        }
    }

    private fun updateConnectionState(playbackState: Int) {
        val newConnectionState = when (playbackState) {
            Player.STATE_IDLE -> ConnectionState.IDLE
            Player.STATE_BUFFERING -> ConnectionState.BUFFERING
            Player.STATE_READY -> ConnectionState.CONNECTED
            Player.STATE_ENDED -> ConnectionState.ENDED
            else -> ConnectionState.ERROR
        }

        _connectionState.value = newConnectionState
        _isConnected.value = newConnectionState == ConnectionState.CONNECTED ||
                newConnectionState == ConnectionState.BUFFERING
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()

        progressUpdateRunnable = object : Runnable {
            override fun run() {
                if (isUpdatingProgress.compareAndSet(false, true)) {
                    try {
                        updateProgressStates()

                        // Programar siguiente actualización
                        if (player.playWhenReady && player.playbackState == STATE_READY) {
                            progressUpdateHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
                        } else {
                            progressUpdateHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL * 2)
                        }
                    } finally {
                        isUpdatingProgress.set(false)
                    }
                }
            }
        }

        progressUpdateRunnable?.let {
            progressUpdateHandler.post(it)
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateRunnable?.let { runnable ->
            progressUpdateHandler.removeCallbacks(runnable)
            progressUpdateRunnable = null
        }
    }

    private fun updateProgressStates() {
        try {
            val currentPos = player.currentPosition
            val totalDuration = player.duration
            val buffered = player.bufferedPosition

            // Solo actualizar si hay cambios significativos
            if (kotlin.math.abs(currentPos - lastPosition) > 500L ||
                _duration.value != totalDuration
            ) {

                _currentPosition.value = currentPos
                _duration.value = totalDuration
                _bufferedPosition.value = buffered

                lastPosition = currentPos

                // Enviar broadcast para actualización de progreso
                sendProgressUpdateBroadcast()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating progress states", e)
        }
    }

    private fun scheduleWidgetUpdate() {
        // Cancelar actualización pendiente
        pendingWidgetUpdate?.let { widgetUpdateHandler.removeCallbacks(it) }

        // Programar nueva actualización con debounce
        pendingWidgetUpdate = Runnable {
            sendStateChangedBroadcast()
        }

        widgetUpdateHandler.postDelayed(pendingWidgetUpdate!!, WIDGET_UPDATE_DEBOUNCE)
    }


    fun playQueue(queue: Queue) {
        service.playQueue(queue)
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        try {
            Log.d(TAG, "Adding ${items.size} items to play next")
            service.playNext(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding items to play next", e)
            reportException(e)
        }
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        try {
            Log.d(TAG, "Adding ${items.size} items to queue")
            service.addToQueue(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding items to queue", e)
            reportException(e)
        }
    }

    fun toggleLike() {
        try {
            Timber.tag(TAG).d("Toggling like for current track. Current state: ${_isLiked.value}")

            // Llamar al servicio para cambiar el estado en la base de datos
            service.toggleLike()

            // Actualizar estado local
            _isLiked.value = !_isLiked.value

            // Notificar cambios al widget
            scheduleWidgetUpdate()

            Log.d(TAG, "Like toggled to: ${_isLiked.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling like", e)
            reportException(e)
        }
    }


    fun isCurrentSongLiked(): Boolean {
        return _isLiked.value
    }

    fun seekToNext() {
        try {
            Log.d(TAG, "Seeking to next track")
            if (player.hasNextMediaItem()) {
                player.seekToNext()
                player.prepare()
                player.playWhenReady = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to next", e)
            reportException(e)
        }
    }

    fun seekToPrevious() {
        try {
            Log.d(TAG, "Seeking to previous track")
            if (player.hasPreviousMediaItem() || player.currentPosition > 3000) {
                player.seekToPrevious()
                player.prepare()
                player.playWhenReady = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to previous", e)
            reportException(e)
        }
    }

    fun togglePlayPause() {
        try {
            val newPlayWhenReady = !player.playWhenReady
            Log.d(TAG, "Toggling play/pause to: $newPlayWhenReady")
            player.playWhenReady = newPlayWhenReady
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause", e)
            reportException(e)
        }
    }

    fun toggleShuffle() {
        try {
            val newShuffleMode = !player.shuffleModeEnabled
            Log.d(TAG, "Toggling shuffle to: $newShuffleMode")
            player.shuffleModeEnabled = newShuffleMode
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling shuffle", e)
            reportException(e)
        }
    }

    fun toggleReplayMode() {
        try {
            val newRepeatMode = if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                REPEAT_MODE_OFF
            } else {
                Player.REPEAT_MODE_ONE
            }
            Log.d(TAG, "Toggling repeat mode to: $newRepeatMode")
            player.repeatMode = newRepeatMode
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling repeat mode", e)
            reportException(e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            Log.d(TAG, "Seeking to position: ${positionMs}ms")
            player.seekTo(positionMs.coerceIn(0, player.duration))
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to position", e)
            reportException(e)
        }
    }

    fun setVolume(volume: Float) {
        try {
            val clampedVolume = volume.coerceIn(0.0f, 1.0f)
            Log.d(TAG, "Setting volume to: $clampedVolume")
            player.volume = clampedVolume
            _volume.value = clampedVolume
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
            reportException(e)
        }
    }

    fun toggleMute() {
        try {
            val newMuteState = !_isMuted.value
            Log.d(TAG, "Toggling mute to: $newMuteState")

            if (newMuteState) {
                player.volume = 0.0f
            } else {
                player.volume = _volume.value
            }

            _isMuted.value = newMuteState
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
            reportException(e)
        }
    }

    // Listeners del reproductor sobrescritos
    override fun onPlaybackStateChanged(state: Int) {
        Log.d(TAG, "Playback state changed: $lastPlaybackState -> $state")
        _playbackState.value = state
        _error.value = player.playerError

        if (lastPlaybackState != state) {
            lastPlaybackState = state
            scheduleWidgetUpdate()
        }
    }

    override fun onPlayWhenReadyChanged(newPlayWhenReady: Boolean, reason: Int) {
        Log.d(
            TAG,
            "PlayWhenReady changed: $lastPlayWhenReady -> $newPlayWhenReady (reason: $reason)"
        )
        _playWhenReady.value = newPlayWhenReady

        if (lastPlayWhenReady != newPlayWhenReady) {
            lastPlayWhenReady = newPlayWhenReady
            scheduleWidgetUpdate()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Timber.tag(TAG).d("Media item transition: ${mediaItem?.mediaId} (reason: $reason)")
        _mediaMetadata.value = mediaItem?.metadata
        _currentMediaItemIndex.value = player.currentMediaItemIndex
        _currentWindowIndex.value = player.getCurrentQueueIndex()

        // Actualizar estado de like cuando cambia la canción
        CoroutineScope(Dispatchers.IO).launch {
            updateLikeStatusForCurrentSong()
        }

        if (lastMediaItemIndex != player.currentMediaItemIndex) {
            lastMediaItemIndex = player.currentMediaItemIndex
            updateCanSkipPreviousAndNext()
            scheduleWidgetUpdate()
        }
    }

    private suspend fun updateLikeStatusForCurrentSong() {
        try {
            val currentSongId = player.currentMediaItem?.mediaId
            if (currentSongId != null) {
                // Consultar la base de datos para obtener el estado actual del like
                // Similar a como lo hace Player.kt con currentSong?.song?.liked
                val songWithInfo = database.song(currentSongId).first()
                _isLiked.value = songWithInfo?.song?.liked ?: false
                Timber.tag(TAG).d("Like status updated for song $currentSongId: ${_isLiked.value}")
            } else {
                _isLiked.value = false
                Timber.tag(TAG).d("No current song, setting like status to false")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error updating like status for current song")
            _isLiked.value = false
        }
    }
    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        Log.d(TAG, "Timeline changed (reason: $reason)")
        _queueWindows.value = player.getQueueWindows()
        _queueTitle.value = service.queueTitle
        _currentMediaItemIndex.value = player.currentMediaItemIndex
        _currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        Log.d(TAG, "Shuffle mode changed: $enabled")
        _shuffleModeEnabled.value = enabled
        _queueWindows.value = player.getQueueWindows()
        _currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
        scheduleWidgetUpdate()
    }

    override fun onRepeatModeChanged(mode: Int) {
        Log.d(TAG, "Repeat mode changed: $mode")
        _repeatMode.value = mode
        updateCanSkipPreviousAndNext()
        scheduleWidgetUpdate()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        Log.e(TAG, "Player error changed", playbackError)
        if (playbackError != null) {
            reportException(playbackError)
        }
        _error.value = playbackError
        updateConnectionState(Player.STATE_IDLE)
    }

    override fun onVolumeChanged(volume: Float) {
        Log.d(TAG, "Volume changed: $volume")
        _volume.value = volume
        _isMuted.value = volume == 0.0f
    }

    private fun updateCanSkipPreviousAndNext() {
        try {
            if (!player.currentTimeline.isEmpty) {
                val window = player.currentTimeline.getWindow(
                    player.currentMediaItemIndex,
                    Timeline.Window()
                )

                val canPrevious = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                        !window.isLive ||
                        player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) ||
                        player.currentPosition > 3000

                val canNext = (window.isLive && window.isDynamic) ||
                        player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)

                _canSkipPrevious.value = canPrevious
                _canSkipNext.value = canNext
            } else {
                _canSkipPrevious.value = false
                _canSkipNext.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating skip states", e)
            _canSkipPrevious.value = false
            _canSkipNext.value = false
        }
    }

    private fun sendStateChangedBroadcast() {
        try {
            val intent = Intent(ACTION_STATE_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending state changed broadcast", e)
        }
    }

    private fun sendProgressUpdateBroadcast() {
        try {
            val intent = Intent(ACTION_UPDATE_PROGRESS).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending progress update broadcast", e)
        }
    }

    fun dispose() {
        Log.d(TAG, "Disposing PlayerConnection")

        stopProgressUpdates()

        // Cancelar actualizaciones pendientes
        pendingWidgetUpdate?.let { widgetUpdateHandler.removeCallbacks(it) }

        try {
            player.removeListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing player listener", e)
        }

        instance = null

        Log.d(TAG, "PlayerConnection disposed")
    }

    // Estados de conexión
    enum class ConnectionState {
        IDLE,
        CONNECTING,
        CONNECTED,
        BUFFERING,
        ENDED,
        ERROR
    }
}