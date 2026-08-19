/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.DiscordSocialSdkLinkedKey
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.DiscordSocialSdkBridge
import com.arturo254.opentune.utils.DiscordSocialSdkInitCompat
import com.arturo254.opentune.utils.DiscordSocialSdkTokenStore
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.launch
import timber.log.Timber

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val REDIRECT_URI_PATH = ":/authorize/callback"

/**
 * Account-linking screen for the official Discord Social SDK (OAuth2 + PKCE, public client) —
 * replaces the WebView token-scraping flow in [DiscordLoginScreen] for users who opt into the
 * "official SDK" backend in [DiscordSettings].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSocialLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLinked by rememberPreference(DiscordSocialSdkLinkedKey, false)

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<DiscordSocialSdkBridge.CurrentUser?>(null) }

    /** The client's websocket only reaches `Ready` (needed for [DiscordSocialSdkBridge.currentUser])
     * a little after `connect()` returns, so poll briefly instead of assuming it's immediate. */
    suspend fun refreshCurrentUser() {
        repeat(15) {
            val user = DiscordSocialSdkBridge.currentUser()
            if (user != null) {
                currentUser = user
                return
            }
            kotlinx.coroutines.delay(300L)
        }
    }

    androidx.compose.runtime.LaunchedEffect(isLinked) {
        if (isLinked) refreshCurrentUser()
    }

    fun signIn() {
        val activity = context.findActivity() ?: return
        val clientIdStr = BuildConfig.DISCORD_SOCIAL_SDK_CLIENT_ID
        val clientId = clientIdStr.toLongOrNull()
        if (clientId == null) {
            isError = true
            statusMessage = context.getString(R.string.discord_social_sdk_missing_client_id)
            return
        }

        isWorking = true
        isError = false
        statusMessage = null
        DiscordSocialSdkInitCompat.setEngineActivity(activity)

        scope.launch {
            try {
                if (!DiscordSocialSdkBridge.createClient()) {
                    isError = true
                    statusMessage = context.getString(R.string.discord_social_sdk_init_failed)
                    return@launch
                }

                val scopes = DiscordSocialSdkBridge.defaultPresenceScopes()
                statusMessage = context.getString(R.string.discord_social_sdk_waiting_for_browser)
                val authResult = DiscordSocialSdkBridge.authorize(clientId, scopes)
                if (!authResult.success) {
                    isError = true
                    statusMessage = authResult.error.ifBlank {
                        context.getString(R.string.discord_social_sdk_authorization_failed)
                    }
                    return@launch
                }

                statusMessage = context.getString(R.string.discord_social_sdk_exchanging_token)
                val redirectUri = "discord-$clientIdStr$REDIRECT_URI_PATH"
                val tokenResult = DiscordSocialSdkBridge.getToken(
                    clientId, authResult.code, redirectUri,
                )
                if (!tokenResult.success) {
                    isError = true
                    statusMessage = tokenResult.error.ifBlank {
                        context.getString(R.string.discord_social_sdk_token_exchange_failed)
                    }
                    return@launch
                }

                DiscordSocialSdkTokenStore.save(
                    context, tokenResult.accessToken, tokenResult.refreshToken,
                )

                val updateResult = DiscordSocialSdkBridge.updateToken(tokenResult.accessToken)
                if (!updateResult.success) {
                    isError = true
                    statusMessage = updateResult.error.ifBlank {
                        context.getString(R.string.discord_social_sdk_token_exchange_failed)
                    }
                    return@launch
                }

                DiscordSocialSdkBridge.connect()
                isLinked = true
                statusMessage = null
                refreshCurrentUser()
            } catch (e: Exception) {
                Timber.tag("DiscordSocialLogin").e(e, "sign-in flow failed")
                isError = true
                statusMessage = e.message ?: context.getString(R.string.discord_social_sdk_authorization_failed)
            } finally {
                isWorking = false
            }
        }
    }

    fun signOut() {
        DiscordSocialSdkBridge.clearRichPresence()
        DiscordSocialSdkBridge.disconnect()
        DiscordSocialSdkTokenStore.clear(context)
        isLinked = false
        isError = false
        statusMessage = null
        currentUser = null
    }

    val discordBlurple = androidx.compose.ui.graphics.Color(0xFF5865F2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    discordBlurple.copy(alpha = if (isLinked) 0.9f else 0.75f),
                                    discordBlurple.copy(alpha = if (isLinked) 0.6f else 0.4f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUrl = currentUser?.avatarUrl
                    if (avatarUrl != null) {
                        coil3.compose.AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.discord),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    if (isLinked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                val linkedUser = currentUser
                Text(
                    text = when {
                        isLinked && linkedUser != null -> linkedUser.displayName
                        isLinked -> stringResource(R.string.discord_social_sdk_linked)
                        else -> stringResource(R.string.discord_social_sdk_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isLinked && linkedUser != null) {
                        "@${linkedUser.username}"
                    } else {
                        stringResource(R.string.discord_social_sdk_ready)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                // Status chip
                AnimatedContent(targetState = Triple(isWorking, isError, statusMessage), label = "status") {
                        (working, error, message) ->
                    val chipColor = when {
                        error -> MaterialTheme.colorScheme.errorContainer
                        working -> MaterialTheme.colorScheme.secondaryContainer
                        isLinked -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    val chipContentColor = when {
                        error -> MaterialTheme.colorScheme.onErrorContainer
                        working -> MaterialTheme.colorScheme.onSecondaryContainer
                        isLinked -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val chipText = when {
                        message != null -> message
                        working -> stringResource(R.string.discord_social_sdk_waiting_for_browser)
                        isLinked -> stringResource(R.string.discord_social_sdk_linked)
                        else -> stringResource(R.string.discord_social_sdk_status_idle)
                    }

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = chipColor,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (working) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = chipContentColor,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(chipContentColor),
                                )
                            }
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = chipContentColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.97f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "scale",
                )

                if (isLinked) {
                    OutlinedButton(
                        onClick = ::signOut,
                        enabled = !isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale),
                        shape = RoundedCornerShape(18.dp),
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(R.string.action_logout), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = ::signIn,
                        enabled = !isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale),
                        shape = RoundedCornerShape(18.dp),
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(containerColor = discordBlurple),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.discord),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.discord_social_sdk_sign_in),
                            fontWeight = FontWeight.SemiBold,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.discord_social_sdk_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
