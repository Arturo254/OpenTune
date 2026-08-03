/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.DiscordSocialSdkLinkedKey
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.DiscordSocialSdkBridge
import com.arturo254.opentune.utils.DiscordSocialSdkTokenStore
import com.arturo254.opentune.utils.rememberPreference
import com.discord.socialsdk.DiscordSocialSdkInit
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

    var status by remember { mutableStateOf(context.getString(R.string.discord_social_sdk_ready)) }
    var isWorking by remember { mutableStateOf(false) }

    fun signIn() {
        val activity = context.findActivity() ?: return
        val clientIdStr = BuildConfig.DISCORD_SOCIAL_SDK_CLIENT_ID
        val clientId = clientIdStr.toLongOrNull()
        if (clientId == null) {
            status = context.getString(R.string.discord_social_sdk_missing_client_id)
            return
        }

        isWorking = true
        DiscordSocialSdkInit.setEngineActivity(activity)

        scope.launch {
            try {
                if (!DiscordSocialSdkBridge.createClient()) {
                    status = context.getString(R.string.discord_social_sdk_init_failed)
                    return@launch
                }

                val scopes = DiscordSocialSdkBridge.defaultPresenceScopes()
                status = context.getString(R.string.discord_social_sdk_waiting_for_browser)
                val authResult = DiscordSocialSdkBridge.authorize(clientId, scopes)
                if (!authResult.success) {
                    status = authResult.error.ifBlank {
                        context.getString(R.string.discord_social_sdk_authorization_failed)
                    }
                    return@launch
                }

                status = context.getString(R.string.discord_social_sdk_exchanging_token)
                val redirectUri = "discord-$clientIdStr$REDIRECT_URI_PATH"
                val tokenResult = DiscordSocialSdkBridge.getToken(
                    clientId, authResult.code, redirectUri,
                )
                if (!tokenResult.success) {
                    status = tokenResult.error.ifBlank {
                        context.getString(R.string.discord_social_sdk_token_exchange_failed)
                    }
                    return@launch
                }

                DiscordSocialSdkTokenStore.save(
                    context, tokenResult.accessToken, tokenResult.refreshToken,
                )

                val updateResult = DiscordSocialSdkBridge.updateToken(tokenResult.accessToken)
                if (!updateResult.success) {
                    status = updateResult.error.ifBlank {
                        context.getString(R.string.discord_social_sdk_token_exchange_failed)
                    }
                    return@launch
                }

                DiscordSocialSdkBridge.connect()
                isLinked = true
                status = context.getString(R.string.discord_social_sdk_linked)
            } catch (e: Exception) {
                Timber.tag("DiscordSocialLogin").e(e, "sign-in flow failed")
                status = e.message ?: context.getString(R.string.discord_social_sdk_authorization_failed)
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
        status = context.getString(R.string.discord_social_sdk_ready)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.discord),
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(text = status, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.padding(top = 24.dp))

        if (isWorking) {
            CircularProgressIndicator()
        } else if (isLinked) {
            Button(onClick = ::signOut) {
                Text(stringResource(R.string.action_logout))
            }
        } else {
            Button(onClick = ::signIn) {
                Text(stringResource(R.string.discord_social_sdk_sign_in))
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
