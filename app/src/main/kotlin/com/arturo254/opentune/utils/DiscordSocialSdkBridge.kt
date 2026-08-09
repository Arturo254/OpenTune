package com.arturo254.opentune.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * JNI bridge to the official Discord Social SDK (discordpp::Client), used for Rich Presence via
 * proper OAuth2 (PKCE, public client) account-linking — replaces the legacy kizzy token-scraping
 * and gateway-impersonation path.
 *
 * Usage is a single, sequential flow (create -> authorize -> exchange code for token -> connect
 * -> update presence), so each pending native call is tracked with a single [CompletableDeferred]
 * rather than a request-id map; callers must not overlap calls of the same kind.
 */
object DiscordSocialSdkBridge {
    private const val UNAVAILABLE_ERROR =
        "Discord Social SDK not available in this build (discord_partner_sdk.aar missing)"

    /** False when discord_partner_sdk.aar wasn't present at build time (see app/libs/README.md
     * and the `discordSdkAarAvailable` Gradle check) — every public function below becomes a
     * no-op/failure instead of throwing UnsatisfiedLinkError. */
    val isAvailable: Boolean = runCatching {
        // libdiscord_partner_sdk.so has its own JNI_OnLoad (registers its internal websocket
        // JNI class map) that only runs if the JVM loads it directly via System.loadLibrary —
        // loading only discord_bridge.so (which merely *links against* it) leaves that
        // JNI_OnLoad unrun and crashes the SDK's background socket thread with
        // "ClassMapEmbedded not initialized" the moment Connect() spins it up.
        System.loadLibrary("discord_partner_sdk")
        System.loadLibrary("discord_bridge")
    }.isSuccess

    data class AuthorizeResult(val success: Boolean, val code: String, val redirectUri: String, val error: String)
    data class TokenResult(
        val success: Boolean,
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Int,
        val scopes: String,
        val error: String,
    )
    data class SimpleResult(val success: Boolean, val error: String)

    private var pendingAuthorize: CompletableDeferred<AuthorizeResult>? = null
    private var pendingToken: CompletableDeferred<TokenResult>? = null
    private var pendingUpdateToken: CompletableDeferred<SimpleResult>? = null
    private var pendingUpdateRichPresence: CompletableDeferred<SimpleResult>? = null

    private var onStatusChangedListener: ((status: Int, error: Int, errorDetail: Int) -> Unit)? = null

    fun setStatusChangedListener(listener: ((status: Int, error: Int, errorDetail: Int) -> Unit)?) {
        onStatusChangedListener = listener
    }

    private var pumpJob: Job? = null

    /**
     * The SDK queues callback results internally and only delivers them when
     * [nativeRunCallbacks] is pumped — it does not dispatch them on its own. Runs for as long as
     * the client exists (started here, never explicitly stopped since this is a process-lifetime
     * singleton), matching the "call periodically, e.g. once per frame" guidance in discordpp.h.
     */
    private fun startCallbackPump() {
        pumpJob?.cancel()
        pumpJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                nativeRunCallbacks()
                delay(50L)
            }
        }
    }

    /** True once [createClient] has succeeded in this process — the native `g_client` is a
     * process-lifetime singleton, so a fresh process always starts back at false, even if the
     * user linked their account in a previous run. */
    var isClientCreated: Boolean = false
        private set

    fun createClient(): Boolean {
        if (!isAvailable) return false
        val created = nativeCreateClient()
        isClientCreated = created
        if (created) startCallbackPump()
        return created
    }

    fun defaultPresenceScopes(): String = if (isAvailable) nativeGetDefaultPresenceScopes() else ""

    suspend fun authorize(clientId: Long, scopes: String): AuthorizeResult {
        if (!isAvailable) {
            return AuthorizeResult(success = false, code = "", redirectUri = "", error = UNAVAILABLE_ERROR)
        }
        val deferred = CompletableDeferred<AuthorizeResult>()
        pendingAuthorize = deferred
        nativeAuthorize(clientId, scopes)
        return deferred.await()
    }

    suspend fun getToken(applicationId: Long, code: String, redirectUri: String): TokenResult {
        if (!isAvailable) {
            return TokenResult(
                success = false, accessToken = "", refreshToken = "",
                expiresIn = 0, scopes = "", error = UNAVAILABLE_ERROR,
            )
        }
        val deferred = CompletableDeferred<TokenResult>()
        pendingToken = deferred
        nativeGetToken(applicationId, code, redirectUri)
        return deferred.await()
    }

    suspend fun updateToken(accessToken: String): SimpleResult {
        if (!isAvailable) return SimpleResult(success = false, error = UNAVAILABLE_ERROR)
        val deferred = CompletableDeferred<SimpleResult>()
        pendingUpdateToken = deferred
        nativeUpdateToken(accessToken)
        return deferred.await()
    }

    /** Exchanges a stored refresh token for a new access token, without the user re-authorizing. */
    suspend fun refreshToken(applicationId: Long, refreshToken: String): TokenResult {
        if (!isAvailable) {
            return TokenResult(
                success = false, accessToken = "", refreshToken = "",
                expiresIn = 0, scopes = "", error = UNAVAILABLE_ERROR,
            )
        }
        val deferred = CompletableDeferred<TokenResult>()
        pendingToken = deferred
        nativeRefreshToken(applicationId, refreshToken)
        return deferred.await()
    }

    fun connect() { if (isAvailable) nativeConnect() }

    fun disconnect() { if (isAvailable) nativeDisconnect() }

    fun isAuthenticated(): Boolean = isAvailable && nativeIsAuthenticated()

    /** Matches discordpp::ActivityTypes ordinals — pass the raw value through, no mapping needed. */
    object ActivityType {
        const val PLAYING = 0
        const val STREAMING = 1
        const val LISTENING = 2
        const val WATCHING = 3
        const val COMPETING = 5
    }

    suspend fun updateRichPresence(
        activityType: Int = ActivityType.LISTENING,
        details: String,
        detailsUrl: String? = null,
        state: String,
        largeImageUrl: String? = null,
        largeImageText: String? = null,
        smallImageUrl: String? = null,
        smallImageText: String? = null,
        startTimestampMs: Long = 0L,
        endTimestampMs: Long = 0L,
        button1Label: String? = null,
        button1Url: String? = null,
        button2Label: String? = null,
        button2Url: String? = null,
    ): SimpleResult {
        if (!isAvailable) return SimpleResult(success = false, error = UNAVAILABLE_ERROR)
        val deferred = CompletableDeferred<SimpleResult>()
        pendingUpdateRichPresence = deferred
        nativeUpdateRichPresence(
            activityType, details, detailsUrl, state,
            largeImageUrl, largeImageText, smallImageUrl, smallImageText,
            startTimestampMs, endTimestampMs,
            button1Label, button1Url, button2Label, button2Url,
        )
        return deferred.await()
    }

    fun clearRichPresence() { if (isAvailable) nativeClearRichPresence() }

    data class CurrentUser(val displayName: String, val username: String, val avatarUrl: String?)

    /** Only populated once the client's websocket status reaches `Ready` (see discordpp.h's
     * `Client::Status`) — returns null before that, or if there's no client in this process. */
    fun currentUser(): CurrentUser? {
        if (!isAvailable) return null
        val displayName = nativeGetCurrentUserDisplayName() ?: return null
        val username = nativeGetCurrentUserUsername() ?: displayName
        val avatarUrl = nativeGetCurrentUserAvatarUrl()
        return CurrentUser(displayName, username, avatarUrl)
    }

    // Called from native code (JNI static method lookups) — do not rename without updating
    // discord_bridge.cpp's GetStaticMethodID calls.
    @JvmStatic
    private fun onStatusChanged(status: Int, error: Int, errorDetail: Int) {
        onStatusChangedListener?.invoke(status, error, errorDetail)
    }

    @JvmStatic
    private fun onAuthorizeResult(success: Boolean, code: String, redirectUri: String, error: String) {
        pendingAuthorize?.complete(AuthorizeResult(success, code, redirectUri, error))
        pendingAuthorize = null
    }

    @JvmStatic
    private fun onTokenResult(
        success: Boolean,
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        scopes: String,
        error: String,
    ) {
        pendingToken?.complete(TokenResult(success, accessToken, refreshToken, expiresIn, scopes, error))
        pendingToken = null
    }

    @JvmStatic
    private fun onUpdateTokenResult(success: Boolean, error: String) {
        pendingUpdateToken?.complete(SimpleResult(success, error))
        pendingUpdateToken = null
    }

    @JvmStatic
    private fun onUpdateRichPresenceResult(success: Boolean, error: String) {
        pendingUpdateRichPresence?.complete(SimpleResult(success, error))
        pendingUpdateRichPresence = null
    }

    private external fun nativeCreateClient(): Boolean
    private external fun nativeRunCallbacks()
    private external fun nativeGetDefaultPresenceScopes(): String
    private external fun nativeAuthorize(clientId: Long, scopes: String)
    private external fun nativeGetToken(applicationId: Long, code: String, redirectUri: String)
    private external fun nativeUpdateToken(accessToken: String)
    private external fun nativeRefreshToken(applicationId: Long, refreshToken: String)
    private external fun nativeConnect()
    private external fun nativeDisconnect()
    private external fun nativeIsAuthenticated(): Boolean
    private external fun nativeUpdateRichPresence(
        activityType: Int,
        details: String,
        detailsUrl: String?,
        state: String,
        largeImageUrl: String?,
        largeImageText: String?,
        smallImageUrl: String?,
        smallImageText: String?,
        startTimestampMs: Long,
        endTimestampMs: Long,
        button1Label: String?,
        button1Url: String?,
        button2Label: String?,
        button2Url: String?,
    )
    private external fun nativeClearRichPresence()
    private external fun nativeGetCurrentUserDisplayName(): String?
    private external fun nativeGetCurrentUserUsername(): String?
    private external fun nativeGetCurrentUserAvatarUrl(): String?
}
