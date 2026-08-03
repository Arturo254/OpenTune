/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arturo254.opentune.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.constants.*
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.ui.component.EditTextPreference
import com.arturo254.opentune.ui.component.EnumListPreference
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.IconContainer
import com.arturo254.opentune.ui.component.ListPreference
import com.arturo254.opentune.ui.component.PreferenceEntry
import com.arturo254.opentune.ui.component.PreferenceGroupTitle
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.my.kizzy.rpc.KizzyRPC
import timber.log.Timber
import kotlinx.coroutines.*
import com.arturo254.opentune.utils.ArtworkStorage

enum class ActivitySource { ARTIST, ALBUM, SONG, APP }

/** Which backend actually sends presence updates — mirrors [DiscordSocialSdkEnabledKey]. */
private enum class DiscordAuthMethod { OFFICIAL, LEGACY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val song by playerConnection.currentSong.collectAsState(null)
    val playbackState by playerConnection.playbackState.collectAsState()
    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    val lastRpcStartTime = DiscordPresenceManager.lastRpcStartTime
    val lastRpcEndTime = DiscordPresenceManager.lastRpcEndTime
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")
    var infoDismissed by rememberPreference(DiscordInfoDismissedKey, false)
    val isLegacyLoggedIn = remember(discordToken) { discordToken.isNotEmpty() }

    var socialSdkEnabled by rememberPreference(DiscordSocialSdkEnabledKey, false)
    val socialSdkLinked by rememberPreference(DiscordSocialSdkLinkedKey, false)
    var authMethod by remember(socialSdkEnabled) {
        mutableStateOf(if (socialSdkEnabled) DiscordAuthMethod.OFFICIAL else DiscordAuthMethod.LEGACY)
    }

    val isLoggedIn = if (authMethod == DiscordAuthMethod.OFFICIAL) socialSdkLinked else isLegacyLoggedIn

    LaunchedEffect(discordToken) {
        val token = discordToken
        if (token.isNotEmpty()) {
            try {
                withContext(Dispatchers.IO) {
                    KizzyRPC.getUserInfo(token)
                }.onSuccess {
                    discordUsername = it.username
                    discordName = it.name
                }
            } catch (e: Exception) {
                Timber.tag("DiscordSettings").w(e, "getUserInfo failed")
            }
        }
    }

    val (discordRPC, onDiscordRPCChange) = rememberPreference(
        key = EnableDiscordRPCKey,
        defaultValue = true
    )

    LaunchedEffect(discordToken, discordRPC, socialSdkEnabled, socialSdkLinked) {
        if (discordRPC && isLoggedIn) {
            Timber.tag("DiscordSettings").d("RPC enabled, MusicService will handle start")
        } else {
            Timber.tag("DiscordSettings").d("RPC disabled or not logged in, stopping manager")
            DiscordPresenceManager.stop()
        }
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            Modifier
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
                )
            )

            AnimatedVisibility(visible = !infoDismissed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.discord_information),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextButton(
                        onClick = { infoDismissed = true },
                        modifier = Modifier.align(Alignment.End).padding(16.dp),
                    ) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            }

            // ── Sign-in ──────────────────────────────────────────────────
            PreferenceGroupTitle(title = stringResource(R.string.account))

            ListPreference(
                title = { Text(stringResource(R.string.discord_sign_in_method)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.discord), null) } },
                selectedValue = authMethod,
                values = listOf(DiscordAuthMethod.OFFICIAL, DiscordAuthMethod.LEGACY),
                valueText = {
                    when (it) {
                        DiscordAuthMethod.OFFICIAL -> stringResource(R.string.discord_sign_in_method_official)
                        DiscordAuthMethod.LEGACY -> stringResource(R.string.discord_sign_in_method_legacy)
                    }
                },
                onValueSelected = {
                    authMethod = it
                    socialSdkEnabled = it == DiscordAuthMethod.OFFICIAL
                },
            )

            if (authMethod == DiscordAuthMethod.LEGACY) {
                Text(
                    text = stringResource(R.string.discord_sign_in_method_legacy_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (authMethod == DiscordAuthMethod.OFFICIAL) {
                PreferenceEntry(
                    title = {
                        Text(
                            text = if (socialSdkLinked) {
                                stringResource(R.string.discord_social_sdk_linked)
                            } else {
                                stringResource(R.string.not_logged_in)
                            },
                            modifier = Modifier.alpha(if (socialSdkLinked) 1f else 0.5f),
                        )
                    },
                    icon = { IconContainer { Icon(painterResource(R.drawable.discord), null) } },
                    trailingContent = {
                        OutlinedButton(onClick = { navController.navigate("settings/discord/social_login") }) {
                            Text(
                                stringResource(
                                    if (socialSdkLinked) R.string.action_logout else R.string.action_login
                                )
                            )
                        }
                    },
                )
            } else {
                PreferenceEntry(
                    title = {
                        Text(
                            text = if (isLegacyLoggedIn) discordName else stringResource(R.string.not_logged_in),
                            modifier = Modifier.alpha(if (isLegacyLoggedIn) 1f else 0.5f),
                        )
                    },
                    description = if (discordUsername.isNotEmpty()) "@$discordUsername" else null,
                    icon = { IconContainer { Icon(painterResource(R.drawable.discord), null) } },
                    trailingContent = {
                        if (isLegacyLoggedIn) {
                            OutlinedButton(onClick = { showLogoutConfirm = true }) { Text(stringResource(R.string.action_logout)) }
                        } else {
                            OutlinedButton(onClick = { navController.navigate("settings/discord/login") }) {
                                Text(stringResource(R.string.action_login))
                            }
                        }
                    },
                )
            }

            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    title = { Text(stringResource(R.string.logout_confirm_title)) },
                    text = { Text(stringResource(R.string.logout_confirm_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            discordName = ""
                            discordToken = ""
                            discordUsername = ""
                            showLogoutConfirm = false
                        }) { Text(stringResource(R.string.logout_confirm_yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) { Text(stringResource(R.string.logout_confirm_no)) }
                    }
                )
            }

            // ── Rich Presence ────────────────────────────────────────────
            PreferenceGroupTitle(title = stringResource(R.string.options))

            SwitchPreference(
                title = { Text(stringResource(R.string.enable_discord_rpc)) },
                checked = discordRPC,
                onCheckedChange = onDiscordRPCChange,
                isEnabled = isLoggedIn,
            )

            val imageOptions = listOf("thumbnail", "artist", "appicon", "custom")
            val smallImageOptions = listOf("thumbnail", "artist", "appicon", "custom", "dontshow")

            val (largeImageType, onLargeImageTypeChange) = rememberPreference(DiscordLargeImageTypeKey, "thumbnail")
            val (largeImageCustomUrl, onLargeImageCustomUrlChange) = rememberPreference(DiscordLargeImageCustomUrlKey, "")
            val (smallImageType, onSmallImageTypeChange) = rememberPreference(DiscordSmallImageTypeKey, "artist")
            val (smallImageCustomUrl, onSmallImageCustomUrlChange) = rememberPreference(DiscordSmallImageCustomUrlKey, "")

            LaunchedEffect(largeImageType, smallImageType) {
                ArtworkStorage.removeBySongId(context, song?.song?.id ?: return@LaunchedEffect)
            }

            var isRefreshing by remember { mutableStateOf(false) }

            PreferenceEntry(
                title = { Text(stringResource(R.string.refresh)) },
                description = stringResource(R.string.description_refresh),
                icon = { IconContainer { Icon(painterResource(R.drawable.update), null) } },
                isEnabled = discordRPC,
                trailingContent = {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    } else {
                        OutlinedButton(
                            enabled = discordRPC,
                            onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    val success = DiscordPresenceManager.updatePresence(
                                        context = context,
                                        token = discordToken,
                                        song = song,
                                        positionMs = playerConnection.player.currentPosition,
                                        isPaused = !playerConnection.player.isPlaying,
                                    )
                                    isRefreshing = false
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(
                                            if (success) "Refreshed!" else "Refresh failed"
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.refresh))
                        }
                    }
                }
            )

            val activityOptions = listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "COMPETING")
            val (activityType, onActivityTypeChange) = rememberPreference(DiscordActivityTypeKey, "LISTENING")
            ListPreference(
                title = { Text(stringResource(R.string.discord_activity_type)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.discord), null) } },
                selectedValue = activityType,
                values = activityOptions,
                valueText = { it.lowercase().replaceFirstChar(Char::titlecase) },
                onValueSelected = onActivityTypeChange,
            )

            var showWhenPaused by rememberPreference(DiscordShowWhenPausedKey, false)
            SwitchPreference(
                title = { Text(stringResource(R.string.discord_show_when_paused)) },
                description = stringResource(R.string.discord_show_when_paused_desc),
                icon = { IconContainer { Icon(painterResource(R.drawable.ic_pause_white), null) } },
                checked = showWhenPaused,
                onCheckedChange = { showWhenPaused = it }
            )

            // Legacy-only knobs: the official SDK path doesn't spoof an online status or client
            // platform, it just reports Rich Presence — hide these instead of leaving them as
            // dead controls that silently do nothing when the official backend is active.
            AnimatedVisibility(visible = authMethod == DiscordAuthMethod.LEGACY) {
                Column {
                    val activityStatusOptions = listOf("online", "dnd", "idle", "streaming")
                    val (activityStatusSelection, onActivityStatusSelectionChange) = rememberPreference(
                        DiscordPresenceStatusKey, "online"
                    )
                    ListPreference(
                        title = { Text(stringResource(R.string.activity_status)) },
                        icon = { IconContainer { Icon(painterResource(R.drawable.status), null) } },
                        selectedValue = activityStatusSelection,
                        values = activityStatusOptions,
                        valueText = {
                            when (it) {
                                "dnd" -> "Do Not Disturb"
                                else -> it.replaceFirstChar(Char::titlecase)
                            }
                        },
                        onValueSelected = onActivityStatusSelectionChange,
                    )

                    val platformOptions = listOf("android", "desktop", "web")
                    val (platformSelection, onPlatformSelectionChange) = rememberPreference(
                        DiscordActivityPlatformKey, "desktop"
                    )
                    ListPreference(
                        title = { Text(stringResource(R.string.platform_status)) },
                        icon = { IconContainer { Icon(painterResource(R.drawable.desktop_windows), null) } },
                        selectedValue = platformSelection,
                        values = platformOptions,
                        valueText = { it.replaceFirstChar(Char::titlecase) },
                        onValueSelected = onPlatformSelectionChange,
                    )
                }
            }

            val intervalOptions = listOf("20s", "50s", "1m", "5m", "Custom", "Disabled")
            val (intervalSelection, onIntervalSelectionChange) = rememberPreference(
                key = androidx.datastore.preferences.core.stringPreferencesKey("discordPresenceIntervalPreset"),
                defaultValue = "20s",
            )
            ListPreference(
                title = { Text(stringResource(R.string.update_interval)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.timer), null) } },
                selectedValue = intervalSelection,
                values = intervalOptions,
                valueText = { it },
                onValueSelected = onIntervalSelectionChange,
            )

            if (intervalSelection == "Custom") {
                val (customValue, onCustomValueChange) = rememberPreference(DiscordPresenceIntervalValueKey, 30)
                val (customUnit, onCustomUnitChange) = rememberPreference(DiscordPresenceIntervalUnitKey, "S")

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customValue.toString(),
                        onValueChange = { text ->
                            val number = text.toIntOrNull()
                            if (number != null) {
                                if (customUnit == "S" && number < 30) onCustomValueChange(30)
                                else onCustomValueChange(number)
                            }
                        },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        singleLine = true
                    )

                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                        TextField(
                            value = when (customUnit) {
                                "S" -> "Seconds"; "M" -> "Minutes"; "H" -> "Hours"; else -> "Seconds"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor().weight(1f),
                        )
                        ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                            listOf("S" to "Seconds", "M" to "Minutes", "H" to "Hours").forEach { (code, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    if (code == "S" && customValue < 30) onCustomValueChange(30)
                                    onCustomUnitChange(code)
                                    unitExpanded = false
                                })
                            }
                        }
                    }
                }
            }

            // ── Content ──────────────────────────────────────────────────
            PreferenceGroupTitle(title = stringResource(R.string.discord_section_content))

            val (nameSource, onNameSourceChange) = rememberEnumPreference(DiscordActivityNameKey, ActivitySource.APP)
            val (detailsSource, onDetailsSourceChange) = rememberEnumPreference(DiscordActivityDetailsKey, ActivitySource.SONG)
            val (stateSource, onStateSourceChange) = rememberEnumPreference(DiscordActivityStateKey, ActivitySource.ARTIST)

            EnumListPreference(
                title = { Text(stringResource(R.string.discord_activity_name)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.text_fields), null) } },
                selectedValue = nameSource,
                valueText = { it.name.lowercase().replaceFirstChar(Char::titlecase) },
                onValueSelected = onNameSourceChange,
            )
            EnumListPreference(
                title = { Text(stringResource(R.string.discord_activity_details)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.text_fields), null) } },
                selectedValue = detailsSource,
                valueText = { it.name.lowercase().replaceFirstChar(Char::titlecase) },
                onValueSelected = onDetailsSourceChange,
            )
            EnumListPreference(
                title = { Text(stringResource(R.string.discord_activity_state)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.text_fields), null) } },
                selectedValue = stateSource,
                valueText = { it.name.lowercase().replaceFirstChar(Char::titlecase) },
                onValueSelected = onStateSourceChange,
            )

            PreferenceGroupTitle(title = stringResource(R.string.discord_image_options))

            ListPreference(
                title = { Text(stringResource(R.string.large_image)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.image), null) } },
                selectedValue = largeImageType,
                values = imageOptions,
                valueText = {
                    if (it == "appicon") "App Icon" else it.replaceFirstChar(Char::titlecase)
                },
                onValueSelected = onLargeImageTypeChange,
            )
            if (largeImageType == "custom") {
                EditTextPreference(
                    title = { Text(stringResource(R.string.large_image_custom_url)) },
                    icon = { IconContainer { Icon(painterResource(R.drawable.link), null) } },
                    value = largeImageCustomUrl,
                    onValueChange = onLargeImageCustomUrlChange,
                    isInputValid = { true },
                )
            }

            val largeTextOptions = listOf("song", "artist", "album", "app", "custom", "dontshow")
            val (largeTextSource, onLargeTextSourceChange) = rememberPreference(DiscordLargeTextSourceKey, "album")
            val (largeTextCustom, onLargeTextCustomChange) = rememberPreference(DiscordLargeTextCustomKey, "")

            ListPreference(
                title = { Text(stringResource(R.string.large_text)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.text_fields), null) } },
                selectedValue = largeTextSource,
                values = largeTextOptions,
                valueText = {
                    when (it) {
                        "song" -> "Song name"; "artist" -> "Artist name"; "album" -> "Album name"
                        "app" -> "App name"; "custom" -> "Custom text"; "dontshow" -> "Don't show"
                        else -> it
                    }
                },
                onValueSelected = onLargeTextSourceChange,
            )
            if (largeTextSource == "custom") {
                EditTextPreference(
                    title = { Text(stringResource(R.string.custom_large_text)) },
                    icon = { IconContainer { Icon(painterResource(R.drawable.text_fields), null) } },
                    value = largeTextCustom,
                    onValueChange = onLargeTextCustomChange,
                    isInputValid = { true },
                )
            }

            ListPreference(
                title = { Text(stringResource(R.string.small_image)) },
                icon = { IconContainer { Icon(painterResource(R.drawable.image), null) } },
                selectedValue = smallImageType,
                values = smallImageOptions,
                valueText = {
                    when (it) {
                        "appicon" -> "App Icon"; "dontshow" -> "Don't show"
                        else -> it.replaceFirstChar(Char::titlecase)
                    }
                },
                onValueSelected = onSmallImageTypeChange,
            )
            if (smallImageType == "custom") {
                EditTextPreference(
                    title = { Text(stringResource(R.string.small_image_custom_url)) },
                    icon = { IconContainer { Icon(painterResource(R.drawable.link), null) } },
                    value = smallImageCustomUrl,
                    onValueChange = onSmallImageCustomUrlChange,
                    isInputValid = { true },
                )
            }

            // ── Buttons ──────────────────────────────────────────────────
            PreferenceGroupTitle(title = stringResource(R.string.discord_section_buttons))

            val (button1Label, onButton1LabelChange) = rememberPreference(DiscordActivityButton1LabelKey, "Listen on YouTube Music")
            val (button1Enabled, onButton1EnabledChange) = rememberPreference(DiscordActivityButton1EnabledKey, true)
            val (button2Label, onButton2LabelChange) = rememberPreference(DiscordActivityButton2LabelKey, "Go to OpenTune")
            val (button2Enabled, onButton2EnabledChange) = rememberPreference(DiscordActivityButton2EnabledKey, true)

            SwitchPreference(
                title = { Text(button1Label.ifBlank { "Button 1" }) },
                checked = button1Enabled,
                onCheckedChange = onButton1EnabledChange,
            )
            if (button1Enabled) {
                EditTextPreference(
                    title = { Text(stringResource(R.string.discord_activity_button1_label) ) },
                    value = button1Label,
                    onValueChange = onButton1LabelChange,
                    isInputValid = { true },
                )
            }

            SwitchPreference(
                title = { Text(button2Label.ifBlank { "Button 2" }) },
                checked = button2Enabled,
                onCheckedChange = onButton2EnabledChange,
            )
            if (button2Enabled) {
                EditTextPreference(
                    title = { Text(stringResource(R.string.discord_activity_button2_label)) },
                    value = button2Label,
                    onValueChange = onButton2LabelChange,
                    isInputValid = { true },
                )
            }

            // ── Preview ──────────────────────────────────────────────────
            PreferenceGroupTitle(title = stringResource(R.string.preview))

            val button1UrlSource by rememberPreference(DiscordActivityButton1UrlSourceKey, "songurl")
            val button1CustomUrl by rememberPreference(DiscordActivityButton1CustomUrlKey, "")
            val button2UrlSource by rememberPreference(DiscordActivityButton2UrlSourceKey, "custom")
            val button2CustomUrl by rememberPreference(
                DiscordActivityButton2CustomUrlKey, "https://github.com/Arturo254/OpenTune"
            )

            val playerIsPlayingForPreview = playerConnection.player.playWhenReady && playbackState == STATE_READY

            RichPresence(
                song,
                currentPlaybackTimeMillis = playerConnection.player.currentPosition,
                nameSource = nameSource,
                detailsSource = detailsSource,
                stateSource = stateSource,
                activityType = activityType,
                largeImageType = largeImageType,
                largeImageCustomUrl = largeImageCustomUrl,
                smallImageType = smallImageType,
                smallImageCustomUrl = smallImageCustomUrl,
                button1Enabled = button1Enabled,
                button2Enabled = button2Enabled,
                isPlaying = playerConnection.player.isPlaying
            )
        }

        TopAppBar(
            title = { Text(stringResource(R.string.discord_integration)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            },
            actions = {
                var threeDotMenuExpanded by remember { mutableStateOf(false) }

                IconButton(onClick = { threeDotMenuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }

                DropdownMenu(
                    expanded = threeDotMenuExpanded,
                    onDismissRequest = { threeDotMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.experiment_settings)) },
                        onClick = {
                            threeDotMenuExpanded = false
                            navController.navigate("settings/discord/experimental")
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.experiment),
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        )
    }
}

/** Used by [DiscordExperimental]; kept as a thin wrapper so that screen's raw-dropdown UI still
 * gets the same look without duplicating the bottom-sheet dialog logic. */
@Composable
fun EditablePreference(
    title: String,
    iconRes: Int,
    value: String,
    defaultValue: String,
    onValueChange: (String) -> Unit,
    description: String? = null,
) {
    EditTextPreference(
        title = { Text(title) },
        icon = { IconContainer { Icon(painterResource(iconRes), null) } },
        value = value,
        onValueChange = onValueChange,
        isInputValid = { true },
    )
}

@Composable
fun RichPresence(
    song: Song?,
    currentPlaybackTimeMillis: Long = 0L,
    nameSource: ActivitySource = ActivitySource.APP,
    detailsSource: ActivitySource = ActivitySource.SONG,
    stateSource: ActivitySource = ActivitySource.ARTIST,
    activityType: String = "LISTENING",
    largeImageType: String = "thumbnail",
    largeImageCustomUrl: String = "",
    smallImageType: String = "artist",
    smallImageCustomUrl: String = "",
    button1Enabled: Boolean = true,
    button2Enabled: Boolean = true,
    isPlaying: Boolean = false,
) {
    val context = LocalContext.current

    fun resolveUrl(source: String, song: Song?, custom: String): String? {
    return when (source.lowercase()) {
        "songurl" -> song?.id?.let { "https://music.youtube.com/watch?v=$it" }
        "artisturl" -> song?.artists?.firstOrNull()?.id?.let { "https://music.youtube.com/channel/$it" }
        "albumurl" -> song?.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
        "custom" -> if (custom.isNotBlank()) custom else null
        else -> null
    }
   }

   val (button1Label) = rememberPreference(DiscordActivityButton1LabelKey, "Listen on YouTube Music")
   val (button1Enabled) = rememberPreference(DiscordActivityButton1EnabledKey, true)

   val (button2Label) = rememberPreference(DiscordActivityButton2LabelKey, "Go to OpenTune")
   val (button2Enabled) = rememberPreference(DiscordActivityButton2EnabledKey, true)

// Button URL sources + custom
   val (button1UrlSource) = rememberPreference(DiscordActivityButton1UrlSourceKey, "songurl")
   val (button1CustomUrl) = rememberPreference(DiscordActivityButton1CustomUrlKey, "")

   val (button2UrlSource) = rememberPreference(DiscordActivityButton2UrlSourceKey, "custom")
   val (button2CustomUrl) = rememberPreference(DiscordActivityButton2CustomUrlKey, "https://github.com/Arturo254/OpenTune")

// Large text source + custom
   val (largeTextSource) = rememberPreference(DiscordLargeTextSourceKey, "album")
   val (largeTextCustom) = rememberPreference(DiscordLargeTextCustomKey, "")

    val previewLargeText = when (largeTextSource) {
    "song" -> song?.song?.title ?: "Song name"
    "artist" -> song?.artists?.firstOrNull()?.name ?: "Artist"
    "album" -> song?.song?.albumName ?: song?.album?.title ?: "Album"
    "app" -> stringResource(R.string.app_name)
    "custom" -> largeTextCustom.ifBlank { "Custom text" }
    "dontshow" -> null
    else -> song?.song?.albumName ?: song?.album?.title
    }
    val resolvedButton1Url = resolveUrl(button1UrlSource, song, button1CustomUrl)
    val resolvedButton2Url = resolveUrl(button2UrlSource, song, button2CustomUrl)
    val activityVerb = when (activityType.uppercase()) {
    "PLAYING" -> "Playing"
    "LISTENING" -> "Listening to"
    "WATCHING" -> "Watching"
    "STREAMING" -> "Streaming"
    "COMPETING" -> "Competing in"
    else -> activityType.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
       }
    }

    val previewTitle = when (nameSource) {
    ActivitySource.ARTIST -> "$activityVerb ${song?.artists?.firstOrNull()?.name ?: "Artist"}"
    ActivitySource.ALBUM -> "$activityVerb ${song?.album?.title ?: song?.song?.albumName ?: "Album"}"
    ActivitySource.SONG -> "$activityVerb ${song?.song?.title ?: "Song"}"
    ActivitySource.APP -> "$activityVerb OpenTune"
   }


    PreferenceEntry(
        title = {
            Text(
            text = stringResource(R.string.preview),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
            )
        },
        content = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = previewTitle,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(108.dp)) {
                            AsyncImage(
                                model = when (largeImageType) {
                                    "thumbnail" -> song?.song?.thumbnailUrl
                                    "artist" -> song?.artists?.firstOrNull()?.thumbnailUrl
                                    "appicon" -> "https://raw.githubusercontent.com/Arturo254/OpenTune/refs/heads/master/assets/icon.png"
                                    "custom" -> largeImageCustomUrl.ifBlank { song?.song?.thumbnailUrl }
                                    else -> song?.song?.thumbnailUrl
                                },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .align(Alignment.TopStart)
                                    .run {
                                        if (song == null) border(
                                            2.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            RoundedCornerShape(12.dp)
                                        ) else this
                                    },
                            )
                            val songThumb = song?.song?.thumbnailUrl
                            val artistThumb = song?.artists?.firstOrNull()?.thumbnailUrl

                            val smallModel = when (smallImageType.lowercase()) {
                                "thumbnail" -> songThumb
                                "artist" -> artistThumb
                                "appicon" -> "https://raw.githubusercontent.com/Arturo254/OpenTune/refs/heads/master/assets/icon.png"
                                "custom" -> smallImageCustomUrl.takeIf { it.isNotBlank() } ?: songThumb
                                "dontshow", "none" -> null
                                else -> artistThumb
                            }
                            smallModel?.let {
                                Box(
                                    modifier = Modifier
                                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                        .padding(2.dp)
                                        .align(Alignment.BottomEnd),
                                ) {
                                    AsyncImage(
                                        model = it,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(CircleShape),
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        ) {
                            Text(
                                text = song?.song?.title ?: "Song Title",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            val previewState = when (stateSource) {
                                ActivitySource.ARTIST -> song?.artists?.joinToString { it.name } ?: "Artist"
                                ActivitySource.ALBUM -> song?.song?.albumName ?: song?.album?.title ?: song?.song?.title ?: "Unknown Album"
                                ActivitySource.SONG -> song?.song?.title ?: "Song"
                                ActivitySource.APP -> stringResource(R.string.app_name)
                            }

                            Text(
                                text = previewState,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            previewLargeText?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (song != null) {
                                SongProgressBar(
                                    currentTimeMillis = currentPlaybackTimeMillis,
                                    durationMillis = song.song.duration * 1000L,
                                    isPlaying = isPlaying,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    androidx.compose.animation.AnimatedVisibility(visible = button1Enabled && button1Label.isNotBlank()) {
                        Button(
                            enabled = !resolvedButton1Url.isNullOrBlank(),
                            onClick = {
                              resolvedButton1Url?.let {
                              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                         }
                     },
                        modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(button1Label.ifBlank { "Listen on YouTube Music" })
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = button2Enabled && button2Label.isNotBlank()) {
                        Button(
                            enabled = !resolvedButton2Url.isNullOrBlank(),
                            onClick = {
                              resolvedButton2Url?.let {
                              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                        }
                     },
                        modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(button2Label.ifBlank { "View Album" })
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SongProgressBar(
    currentTimeMillis: Long,
    durationMillis: Long,
    isPlaying: Boolean = false
) {
    var displayedTime by remember { mutableStateOf(currentTimeMillis) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(500)
                displayedTime += 500
                if (displayedTime >= durationMillis) {
                    displayedTime = durationMillis
                    break
                }
            }
        }
    }

    val progress = if (durationMillis > 0) {
        displayedTime.toFloat() / durationMillis
    } else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = makeTimeString(displayedTime),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontSize = 12.sp
            )
            Text(
                text = makeTimeString(durationMillis),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontSize = 12.sp
            )
        }
    }
}
