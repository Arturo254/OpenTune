/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.screens.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.arturo254.opentune.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.*
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.utils.DiscordRPC
import com.arturo254.opentune.utils.DiscordImageResolver
import com.arturo254.opentune.utils.DiscordSocialSdkBridge
import com.arturo254.opentune.utils.DiscordSocialSdkTokenStore
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object DiscordPresenceManager {
    private val started = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var rpcInstance: DiscordRPC? = null
    private var rpcToken: String? = null
    private val logTag = "DiscordPresenceManager"

    // Stored start parameters so we can restart the updater later.
    // We intentionally store the application Context (or whatever the caller passed) — callers
    // should prefer passing an Application context to avoid leaking Activities.
    private var lastStartContext: Context? = null
    private var lastToken: String? = null
    private var lastSongProvider: (() -> Song?)? = null
    private var lastPositionProvider: (() -> Long)? = null
    private var lastIsPausedProvider: (() -> Boolean)? = null
    private var lastIntervalProvider: (() -> Long)? = null
    private var lastPresenceUpdateTime = 0L
    private const val MIN_PRESENCE_UPDATE_INTERVAL = 20_000L // 20 seconds debounce
    private var consecutiveFailures = 0
    private const val MAX_CONSECUTIVE_FAILURES = 3
    private var lastRestartTime = 0L
    private const val MIN_RESTART_INTERVAL = 30_000L 
    private var lastFailedRestartDueToParams = 0L
    private const val FAILED_RESTART_LOCKOUT = 60_000L


    // Last successful RPC timestamps (nullable). Exposed as StateFlow so Compose can observe changes.
    private val _lastRpcStartTime = MutableStateFlow<Long?>(null)
    val lastRpcStartTimeFlow = _lastRpcStartTime.asStateFlow()
    val lastRpcStartTime: Long? get() = _lastRpcStartTime.value

    private val _lastRpcEndTime = MutableStateFlow<Long?>(null)
    val lastRpcEndTimeFlow = _lastRpcEndTime.asStateFlow()
    val lastRpcEndTime: Long? get() = _lastRpcEndTime.value
    private val rpcMutex = Mutex()

    /** Public helper to update the last RPC timestamps from callers. */
    fun setLastRpcTimestamps(start: Long?, end: Long?) {
        _lastRpcStartTime.value = start
        _lastRpcEndTime.value = end
    }

    suspend fun getOrCreateRpc(context: Context, token: String): DiscordRPC {
        if (rpcInstance == null || rpcToken != token) {
            try {
                rpcInstance?.stopActivity()
            } catch (ex: Exception) {
                Timber.tag(logTag).v(ex, "failed to stopActivity on previous RPC instance")
            }

            try {
                rpcInstance?.closeRPC()
            } catch (ex: Exception) {
                Timber.tag(logTag).v(ex, "failed to close previous RPC instance")
            }

            rpcInstance = DiscordRPC(context, token)
            rpcToken = token
        }
        return rpcInstance!!
    }

    /**
     * Core updater: update or clear Discord presence.
     */
    suspend fun updatePresence(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        rpcMutex.withLock {
            try {
                val useSocialSdk = context.dataStore[DiscordSocialSdkEnabledKey] == true &&
                    context.dataStore[DiscordSocialSdkLinkedKey] == true
                if (useSocialSdk) {
                    return@withLock updatePresenceViaSocialSdk(context, song, positionMs, isPaused)
                }

                if (token.isBlank()) {
                    Timber.tag(logTag).w("updatePresence skipped (token missing)")
                    return@withLock false
                }

                if (song == null) {
                    val rpc = getOrCreateRpc(context, token)
                    rpc.stopActivity()
                    Timber.tag(logTag).d("cleared presence (no song)")
                    consecutiveFailures = 0
                    return@withLock true
                }

                try {
                    withTimeout(8_000L) {
                        DiscordImageResolver.resolveImagesForSong(context, song)
                    }
                } catch (e: Exception) {
                    Timber.tag(logTag).v(e, "image resolution for presence failed or timed out")
                }

                val rpc = getOrCreateRpc(context, token)
                val result = rpc.updateSong(song, positionMs, isPaused)
                if (result.isSuccess) {
                    consecutiveFailures = 0
                    Timber.tag(logTag).d(
                        "updatePresence success (song=%s, paused=%s)",
                        song.song.title,
                        isPaused
                    )

                    if (!isPaused) {
                        val now = System.currentTimeMillis()
                        val calculatedStartTime = now - positionMs
                        val calculatedEndTime = calculatedStartTime + song.song.duration * 1000L
                        setLastRpcTimestamps(calculatedStartTime, calculatedEndTime)
                    }
                    true
                } else {
                    consecutiveFailures++
                    Timber.tag(logTag).w("updatePresence failed silently — updateSong returned failure (consecutive=%d)", consecutiveFailures)
                    false
                }
            } catch (ex: Exception) {
                consecutiveFailures++
                Timber.tag(logTag).e(ex, "updatePresence failed (consecutive=%d)", consecutiveFailures)
                false
            }
        }
    }

    /**
     * The native discordpp::Client is a process-lifetime singleton created by
     * [DiscordSocialSdkBridge.createClient] — a fresh process (e.g. after the app was killed and
     * MusicService restarted) starts with no client at all, even if the user linked their
     * account in a previous run. Re-creates it from the stored access token when needed, without
     * making the user go through the OAuth screen again.
     */
    private var socialSdkStatusListenerRegistered = false

    /** discordpp::Client::Error::UnexpectedClose — see the SDK header: fired when, among other
     * network issues, the linked account's auth token has gone stale/invalid. */
    private const val CLIENT_ERROR_UNEXPECTED_CLOSE = 2

    private fun registerSocialSdkStatusListener(context: Context) {
        if (socialSdkStatusListenerRegistered) return
        socialSdkStatusListenerRegistered = true
        val appContext = context.applicationContext
        DiscordSocialSdkBridge.setStatusChangedListener { _, error, _ ->
            if (error == CLIENT_ERROR_UNEXPECTED_CLOSE) {
                CoroutineScope(Dispatchers.IO).launch { handleUnexpectedClose(appContext) }
            }
        }
    }

    /**
     * The linked session dropped with an auth-looking error. Try to silently renew it from the
     * stored refresh token; only fall back to asking the user to sign in again if that fails too.
     */
    private suspend fun handleUnexpectedClose(context: Context) {
        val appId = BuildConfig.DISCORD_SOCIAL_SDK_CLIENT_ID.toLongOrNull()
        val refreshToken = DiscordSocialSdkTokenStore.refreshToken(context)
        if (appId == null || refreshToken.isNullOrBlank()) {
            Timber.tag(logTag).w("social sdk: UnexpectedClose with no refresh token available, unlinking")
            context.dataStore.edit { it[DiscordSocialSdkLinkedKey] = false }
            return
        }

        val result = DiscordSocialSdkBridge.refreshToken(appId, refreshToken)
        if (result.success) {
            DiscordSocialSdkTokenStore.save(context, result.accessToken, result.refreshToken)
            DiscordSocialSdkBridge.updateToken(result.accessToken)
            DiscordSocialSdkBridge.connect()
            Timber.tag(logTag).d("social sdk: token refreshed after UnexpectedClose")
        } else {
            Timber.tag(logTag).w("social sdk: refresh failed after UnexpectedClose (%s), unlinking", result.error)
            context.dataStore.edit { it[DiscordSocialSdkLinkedKey] = false }
        }
    }

    private suspend fun ensureSocialSdkClientReady(context: Context): Boolean {
        if (DiscordSocialSdkBridge.isClientCreated) {
            registerSocialSdkStatusListener(context)
            return true
        }

        if (com.arturo254.opentune.utils.DiscordSocialSdkInitCompat.getApplicationContext() == null) {
            // MainActivity hasn't run yet in this process (e.g. MusicService was started in the
            // background, such as by a media button, before the UI ever opened) — the SDK's
            // native code needs an Activity-derived Context first (set in MainActivity.onCreate)
            // or it crashes. Skip this cycle; the next one after the UI opens will succeed.
            Timber.tag(logTag).v("social sdk: engine context not ready yet, skipping")
            return false
        }

        if (!DiscordSocialSdkBridge.createClient()) {
            Timber.tag(logTag).w("social sdk: createClient failed on lazy re-init")
            return false
        }
        registerSocialSdkStatusListener(context)
        val accessToken = DiscordSocialSdkTokenStore.accessToken(context)
        if (accessToken.isNullOrBlank()) {
            Timber.tag(logTag).w("social sdk: no stored access token for lazy re-init")
            return false
        }
        val updateResult = DiscordSocialSdkBridge.updateToken(accessToken)
        if (!updateResult.success) {
            Timber.tag(logTag).w("social sdk: token refresh failed on lazy re-init: %s", updateResult.error)
            return false
        }
        DiscordSocialSdkBridge.connect()
        return true
    }

    private fun pickSourceValue(pref: String, song: Song, default: String): String = when (pref.uppercase()) {
        "ARTIST" -> song.artists.firstOrNull()?.name ?: default
        "ALBUM" -> song.song.albumName ?: song.album?.title ?: default
        "SONG" -> song.song.title
        else -> default
    }

    private fun resolveButtonUrl(source: String, song: Song, custom: String): String? = when (source.lowercase()) {
        "songurl" -> "https://music.youtube.com/watch?v=${song.song.id}"
        "artisturl" -> song.artists.firstOrNull()?.id?.let { "https://music.youtube.com/channel/$it" }
        "albumurl" -> song.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
        "custom" -> custom.takeIf { it.isNotBlank() }
        else -> null
    }

    /**
     * Presence update via the official Discord Social SDK (OAuth2-linked), instead of kizzy.
     * Mirrors [DiscordRPC.updateSong]'s preference-driven name/details/state/image/button
     * resolution (same DataStore keys, so switching backends keeps the same configured look),
     * minus the translator integration — the SDK path is new and untranslated for now.
     */
    private suspend fun updatePresenceViaSocialSdk(
        context: Context,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean {
        if (!ensureSocialSdkClientReady(context)) return false

        val showWhenPaused = context.dataStore[DiscordShowWhenPausedKey] ?: false
        if (song == null || (isPaused && !showWhenPaused)) {
            DiscordSocialSdkBridge.clearRichPresence()
            Timber.tag(logTag).d("cleared presence via social sdk (song=%s, paused=%s)", song, isPaused)
            consecutiveFailures = 0
            return true
        }

        val detailsPref = context.dataStore[DiscordActivityDetailsKey] ?: "SONG"
        val statePref = context.dataStore[DiscordActivityStateKey] ?: "ARTIST"
        val activityTypePref = context.dataStore[DiscordActivityTypeKey] ?: "LISTENING"
        val largeImageTypePref = context.dataStore[DiscordLargeImageTypeKey] ?: "thumbnail"
        val largeImageCustomPref = context.dataStore[DiscordLargeImageCustomUrlKey] ?: ""
        val smallImageTypePref = context.dataStore[DiscordSmallImageTypeKey] ?: "artist"
        val smallImageCustomPref = context.dataStore[DiscordSmallImageCustomUrlKey] ?: ""
        val largeTextSourcePref = (context.dataStore[DiscordLargeTextSourceKey] ?: "album").lowercase()
        val largeTextCustomPref = context.dataStore[DiscordLargeTextCustomKey] ?: ""

        val artistName = song.artists.joinToString { it.name }
        val details = pickSourceValue(detailsPref, song, song.song.title).ifBlank { song.song.title }
        val state = pickSourceValue(statePref, song, artistName).ifBlank { artistName }

        val activityType = when (activityTypePref.uppercase()) {
            "PLAYING" -> DiscordSocialSdkBridge.ActivityType.PLAYING
            "STREAMING" -> DiscordSocialSdkBridge.ActivityType.STREAMING
            "WATCHING" -> DiscordSocialSdkBridge.ActivityType.WATCHING
            "COMPETING" -> DiscordSocialSdkBridge.ActivityType.COMPETING
            else -> DiscordSocialSdkBridge.ActivityType.LISTENING
        }

        fun resolveImageUrl(typePref: String, customUrl: String): String? = when (typePref.lowercase()) {
            "thumbnail", "song", "album" -> song.song.thumbnailUrl
            "artist" -> song.artists.firstOrNull()?.thumbnailUrl
            "appicon" -> "https://raw.githubusercontent.com/Arturo254/OpenTune/refs/heads/master/assets/icon.png"
            "custom" -> customUrl.takeIf { it.isNotBlank() } ?: song.song.thumbnailUrl
            "none", "dontshow" -> null
            else -> song.song.thumbnailUrl
        }

        val largeImageUrl = resolveImageUrl(largeImageTypePref, largeImageCustomPref)
        val smallImageUrl = resolveImageUrl(smallImageTypePref, smallImageCustomPref)
        val largeText = when (largeTextSourcePref) {
            "song" -> song.song.title
            "artist" -> song.artists.firstOrNull()?.name
            "album" -> song.song.albumName ?: song.album?.title ?: song.song.title
            "custom" -> largeTextCustomPref.ifBlank { null }
            "dontshow" -> null
            else -> song.song.albumName ?: song.album?.title
        }

        val button1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: "Listen on YouTube Music"
        val button1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true
        val button1UrlSource = context.dataStore[DiscordActivityButton1UrlSourceKey] ?: "songurl"
        val button1CustomUrl = context.dataStore[DiscordActivityButton1CustomUrlKey] ?: ""
        val button2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: "Go to OpenTune"
        val button2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true
        val button2UrlSource = context.dataStore[DiscordActivityButton2UrlSourceKey] ?: "custom"
        val button2CustomUrl = context.dataStore[DiscordActivityButton2CustomUrlKey]
            ?: "https://github.com/Arturo254/OpenTune"

        val button1Url = resolveButtonUrl(button1UrlSource, song, button1CustomUrl)
            .takeIf { button1Enabled && button1Label.isNotBlank() }
        val button2Url = resolveButtonUrl(button2UrlSource, song, button2CustomUrl)
            .takeIf { button2Enabled && button2Label.isNotBlank() }

        val hasValidDuration = song.song.duration > 0
        val now = System.currentTimeMillis()
        val startTimestampMs = if (!isPaused && hasValidDuration) now - positionMs else 0L
        val endTimestampMs = if (!isPaused && hasValidDuration) {
            (now - positionMs) + song.song.duration * 1000L
        } else {
            0L
        }

        val result = DiscordSocialSdkBridge.updateRichPresence(
            activityType = activityType,
            details = details,
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            state = state,
            largeImageUrl = largeImageUrl,
            largeImageText = largeText,
            smallImageUrl = smallImageUrl,
            smallImageText = if (isPaused) context.getString(R.string.discord_paused) else artistName,
            startTimestampMs = startTimestampMs,
            endTimestampMs = endTimestampMs,
            button1Label = button1Label.takeIf { button1Url != null },
            button1Url = button1Url,
            button2Label = button2Label.takeIf { button2Url != null },
            button2Url = button2Url,
        )
        return if (result.success) {
            consecutiveFailures = 0
            if (!isPaused) setLastRpcTimestamps(startTimestampMs, endTimestampMs)
            Timber.tag(logTag).d("updatePresence (social sdk) success (song=%s)", song.song.title)
            true
        } else {
            consecutiveFailures++
            Timber.tag(logTag).w(
                "updatePresence (social sdk) failed: %s (consecutive=%d)",
                result.error, consecutiveFailures,
            )
            false
        }
    }

    /**
     * Start background updater.
     */
    fun start(
        context: Context,
        token: String,
        songProvider: () -> Song?,
        positionProvider: () -> Long,
        isPausedProvider: () -> Boolean,
        intervalProvider: () -> Long
    ) {
        lastStartContext = context
        lastToken = token
        lastSongProvider = songProvider
        lastPositionProvider = positionProvider
        lastIsPausedProvider = isPausedProvider
        lastIntervalProvider = intervalProvider

        if (started.getAndSet(true)) return

        resetFailureCount()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        job = scope!!.launch {
            // Perform an immediate first update (or at the first second of the interval).
            try {
                // switch to Main for player access
                val (firstSong, firstPosition, firstIsPaused) = withContext(Dispatchers.Main) {
                    Triple(songProvider(), positionProvider(), isPausedProvider())
                }

                // Try resolving and persisting image URLs before update so DiscordRPC can use saved artwork immediately.
                try {
                    firstSong?.let { song ->
                        DiscordImageResolver.resolveImagesForSong(context, song)
                    }
                } catch (e: Exception) {
                    Timber.tag(logTag).v(e, "initial image resolution failed")
                }

                // Run the first update immediately
                try {
                    val firstResult = updatePresence(
                        context = context,
                        token = token,
                        song = firstSong,
                        positionMs = firstPosition,
                        isPaused = firstIsPaused,
                    )
                    Timber.tag(logTag).d("initial updatePresence result=%s songId=%s", firstResult, firstSong?.song?.id)
                } catch (e: Exception) {
                    Timber.tag(logTag).e(e, "initial updatePresence failed")
                }
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "initial first-run failed")
            }

            while (isActive) {
                try {
                    // switch to Main for player access
                    val (song, position, isPaused) = withContext(Dispatchers.Main) {
                        Triple(songProvider(), positionProvider(), isPausedProvider())
                    }

                    val success = updatePresence(
                        context = context,
                        token = token,
                        song = song,
                        positionMs = position,
                        isPaused = isPaused,
                    )

                    // optional: handle `success` if needed
                } catch (e: CancellationException) {
                    Timber.tag(logTag).d("updater cancelled")
                    break
                } catch (e: Exception) {
                    Timber.tag(logTag).e(e, "loop error → ${e.message}")
                }

                val delayMs = intervalProvider()
                if (delayMs <= 0L) break
                delay(delayMs)
            }
        }

        lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                stop()
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!)
    }

    /**
     * Restart the manager using the most recent parameters passed to `start()`.
     * Returns true if restart was scheduled, false if there were no stored parameters or too many recent failures.
     */
    fun restart(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRestartTime < MIN_RESTART_INTERVAL) {
            Timber.tag(logTag).w("restart skipped (too soon since last restart, wait %dms)", MIN_RESTART_INTERVAL - (now - lastRestartTime))
            return false
        }
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            Timber.tag(logTag).w("restart skipped (too many consecutive failures: %d)", consecutiveFailures)
            return false
        }

        val ctx = lastStartContext
        val token = lastToken
        val songProv = lastSongProvider
        val posProv = lastPositionProvider
        val pausedProv = lastIsPausedProvider
        val intervalProv = lastIntervalProvider

        if (ctx == null || token == null || songProv == null || posProv == null || pausedProv == null || intervalProv == null) {
            if (now - lastFailedRestartDueToParams < FAILED_RESTART_LOCKOUT) {
                Timber.tag(logTag).w("restart skipped (lockout after missing params, wait %dms)", FAILED_RESTART_LOCKOUT - (now - lastFailedRestartDueToParams))
                return false
            }
            lastFailedRestartDueToParams = now
            Timber.tag(logTag).w("restart skipped (missing previous start parameters)")
            return false
        }

        lastRestartTime = now
        lastFailedRestartDueToParams = 0L
        stop()
        start(ctx, token, songProv, posProv, pausedProv, intervalProv)
        Timber.tag(logTag).d("restarted")
        return true
    }
    
    fun resetFailureCount() {
        consecutiveFailures = 0
    }

    /** Run update immediately. */
    suspend fun updateNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean = updatePresence(
        context = context,
        token = token,
        song = song,
        positionMs = positionMs,
        isPaused = isPaused,
    )

    /** Stop the manager. */
    fun stop() {
        if (!started.getAndSet(false)) return
        
        val rpcToClose = rpcInstance
        rpcInstance = null
        rpcToken = null
        
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
        lifecycleObserver = null

        if (rpcToClose != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rpcToClose.stopActivity()
                } catch (ex: Exception) {
                    Timber.tag(logTag).v(ex, "stopActivity failed during stop()")
                }
                try {
                    rpcToClose.closeRPC()
                } catch (ex: Exception) {
                    Timber.tag(logTag).v(ex, "closeRPC failed during stop()")
                }
            }
        }

        Timber.tag(logTag).d("stopped")
    }

    fun isRunning(): Boolean = started.get()
}
