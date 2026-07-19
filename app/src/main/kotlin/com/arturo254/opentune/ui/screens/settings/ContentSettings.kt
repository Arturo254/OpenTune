/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.*
import com.arturo254.opentune.ui.component.*
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.utils.setAppLocale
import java.net.Proxy
import java.util.Locale
import androidx.core.net.toUri

private fun getLanguageDisplayName(languageCode: String): String {
    return when (languageCode) {
        SYSTEM_DEFAULT -> "System Default"
        else -> LanguageCodeToName[languageCode] ?: languageCode
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (appLanguage, onAppLanguageChange) = rememberPreference(
        key = AppLanguageKey,
        defaultValue = SYSTEM_DEFAULT
    )

    val (contentLanguage, onContentLanguageChange) = rememberPreference(
        key = ContentLanguageKey,
        defaultValue = "system"
    )
    val (contentCountry, onContentCountryChange) = rememberPreference(
        key = ContentCountryKey,
        defaultValue = "system"
    )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(
        key = HideExplicitKey,
        defaultValue = false
    )
    val (hideVideo, onHideVideoChange) = rememberPreference(
        key = HideVideoKey,
        defaultValue = false
    )
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(
        key = ProxyEnabledKey,
        defaultValue = false
    )
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(
        key = ProxyTypeKey,
        defaultValue = Proxy.Type.HTTP
    )
    val (proxyUrl, onProxyUrlChange) = rememberPreference(
        key = ProxyUrlKey,
        defaultValue = "host:port"
    )
    val (streamBypassProxy, onStreamBypassProxyChange) = rememberPreference(
        key = StreamBypassProxyKey,
        defaultValue = false
    )
    val (enableKugou, onEnableKugouChange) = rememberPreference(
        key = EnableKugouKey,
        defaultValue = true
    )
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(
        key = EnableLrcLibKey,
        defaultValue = true
    )
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(
        key = EnableBetterLyricsKey,
        defaultValue = true
    )
    val (enableSimpMusicLyrics, onEnableSimpMusicLyricsChange) =
        rememberPreference(
            key = EnableSimpMusicLyricsKey,
            defaultValue = true
        )
    val (preferredProvider, onPreferredProviderChange) =
        rememberEnumPreference(
            key = PreferredLyricsProviderKey,
            defaultValue = PreferredLyricsProvider.LRCLIB,
        )
    val (lyricsRomanizeJapanese, onLyricsRomanizeJapaneseChange) = rememberPreference(
        LyricsRomanizeJapaneseKey,
        defaultValue = true
    )
    val (lyricsRomanizeKorean, onLyricsRomanizeKoreanChange) = rememberPreference(
        LyricsRomanizeKoreanKey,
        defaultValue = true
    )
    val (preloadQueueLyricsEnabled, onPreloadQueueLyricsEnabledChange) = rememberPreference(
        PreloadQueueLyricsEnabledKey,
        defaultValue = true
    )
    val (queueLyricsPreloadCount, onQueueLyricsPreloadCountChange) = rememberPreference(
        QueueLyricsPreloadCountKey,
        defaultValue = 1
    )
    val (lengthTop, onLengthTopChange) = rememberPreference(
        key = TopSize,
        defaultValue = "50"
    )
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(
        key = QuickPicksKey,
        defaultValue = QuickPicks.QUICK_PICKS
    )
    val (jossRedEnabled, onJossRedEnabledChange) = rememberPreference(
        key = JossRedMultimediaKey,
        defaultValue = true
    )

    var showLanguageSelector by remember { mutableStateOf(false) }

    val languageOptions = remember {
        LanguageCodeToName.map { (code, name) ->
            LanguageOption(code = code, displayName = name)
        }
    }

    var showProviderOrderDialog by remember { mutableStateOf(false) }

    val (providerOrder, onProviderOrderChange) = rememberPreference(
        key = ProviderOrderKey,
        defaultValue = DefaultProviderOrder.joinToString(",") { it.name },
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(title = stringResource(R.string.general))

        ListPreference(
            title = { Text(stringResource(R.string.content_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = { newValue ->
                val locale = Locale.getDefault()
                val languageTag = locale.toLanguageTag().replace("-Hant", "")

                YouTube.locale = YouTube.locale.copy(
                    hl = newValue.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.language.takeIf { it in LanguageCodeToName }
                        ?: languageTag.takeIf { it in LanguageCodeToName }
                        ?: "en"
                )

                onContentLanguageChange(newValue)
            }
        )

        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(painterResource(R.drawable.location_on), null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
            },
            onValueSelected = { newValue ->
                val locale = Locale.getDefault()

                YouTube.locale = YouTube.locale.copy(
                    gl = newValue.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.country.takeIf { it in CountryCodeToName }
                        ?: "US"
                )

                onContentCountryChange(newValue)
            }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_explicit)) },
            icon = { Icon(painterResource(R.drawable.explicit), null) },
            checked = hideExplicit,
            onCheckedChange = onHideExplicitChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_video)) },
            icon = { Icon(painterResource(R.drawable.slow_motion_video), null) },
            checked = hideVideo,
            onCheckedChange = onHideVideoChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.app_language))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.app_language)) },
                subtitle = {
                    Text(
                        text = getLanguageDisplayName(appLanguage)
                    )
                },
                icon = { Icon(painterResource(R.drawable.translate), null) },
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APP_LOCALE_SETTINGS,
                            "package:${context.packageName}".toUri()
                        )
                    )
                }
            )
        } else {
            PreferenceEntry(
                title = { Text(stringResource(R.string.app_language)) },
                subtitle = {
                    Text(
                        text = getLanguageDisplayName(appLanguage)
                    )
                },
                icon = { Icon(painterResource(R.drawable.language), null) },
                onClick = { showLanguageSelector = true }
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.proxy))

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_proxy)) },
            icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
            checked = proxyEnabled,
            onCheckedChange = onProxyEnabledChange,
        )

        if (proxyEnabled) {
            Column {
                ListPreference(
                    title = { Text(stringResource(R.string.proxy_type)) },
                    selectedValue = proxyType,
                    values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                    valueText = { it.name },
                    onValueSelected = onProxyTypeChange,
                )
                EditTextPreference(
                    title = { Text(stringResource(R.string.proxy_url)) },
                    value = proxyUrl,
                    onValueChange = onProxyUrlChange,
                )
                SwitchPreference(
                    title = { Text(stringResource(R.string.stream_bypass_proxy)) },
                    description = stringResource(R.string.stream_bypass_proxy_desc),
                    icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                    checked = streamBypassProxy,
                    onCheckedChange = {
                        onStreamBypassProxyChange(it)
                        YouTube.streamBypassProxy = it
                    },
                )
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.lyrics))

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_lrclib)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableLrclib,
            onCheckedChange = onEnableLrclibChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_kugou)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableKugou,
            onCheckedChange = onEnableKugouChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_betterlyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableBetterLyrics,
            onCheckedChange = onEnableBetterLyricsChange,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_simpmusic_lyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableSimpMusicLyrics,
            onCheckedChange = onEnableSimpMusicLyricsChange,
        )

        val savedOrder = remember(providerOrder) {
            val parsed = providerOrder.split(",")
                .mapNotNull { name -> PreferredLyricsProvider.entries.find { it.name == name } }

            val missing = DefaultProviderOrder.filterNot { it in parsed }
            (parsed + missing).ifEmpty { DefaultProviderOrder }
        }

        PreferenceEntry(
            title = {
                Text(stringResource(R.string.lyrics_provider_order))
            },
            subtitle = {
                Text(
                    stringResource(
                        R.string.lyrics_provider_priority,
                        savedOrder.joinToString(" → ") { it.displayName() }
                    )
                )
            },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            onClick = { showProviderOrderDialog = true },
        )

        if (showProviderOrderDialog) {
            DragDropLyricsProviderDialog(
                providers = savedOrder,
                selectedProvider = preferredProvider,
                onDismiss = { showProviderOrderDialog = false },
                onOrderConfirmed = { newOrder ->
                    onProviderOrderChange(newOrder.joinToString(",") { it.name })

                    val newPreferred = newOrder.firstOrNull() ?: PreferredLyricsProvider.LRCLIB
                    if (newPreferred != preferredProvider) {
                        onPreferredProviderChange(newPreferred)
                    }
                },
                valueText = { it.displayName() },
            )
        }

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_japanese)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsRomanizeJapanese,
            onCheckedChange = onLyricsRomanizeJapaneseChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_romanize_korean)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsRomanizeKorean,
            onCheckedChange = onLyricsRomanizeKoreanChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.preload_queue_lyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = preloadQueueLyricsEnabled,
            onCheckedChange = onPreloadQueueLyricsEnabledChange,
        )

        if (preloadQueueLyricsEnabled) {
            NumberPickerPreference(
                title = { Text(stringResource(R.string.queue_lyrics_preload_count)) },
                icon = { Icon(painterResource(R.drawable.lyrics), null) },
                value = queueLyricsPreloadCount,
                onValueChange = onQueueLyricsPreloadCountChange,
                minValue = 0,
                maxValue = 10,
                valueText = { if (it == 0) "Off" else it.toString() },
            )
        }

        PreferenceGroupTitle(title = stringResource(R.string.playback))

        SwitchPreference(
            title = { Text(stringResource(R.string.jossred_fallback_label)) },
            description = stringResource(R.string.jossred_fallback_description),
            icon = { Icon(painterResource(R.drawable.cloud_off), null) },
            checked = jossRedEnabled,
            onCheckedChange = onJossRedEnabledChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.misc))

        EditTextPreference(
            title = { Text(stringResource(R.string.top_length)) },
            icon = { Icon(painterResource(R.drawable.trending_up), null) },
            value = lengthTop,
            isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
            onValueChange = onLengthTopChange,
        )

        ListPreference(
            title = { Text(stringResource(R.string.set_quick_picks)) },
            icon = { Icon(painterResource(R.drawable.home_outlined), null) },
            selectedValue = quickPicks,
            values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
            valueText = {
                when (it) {
                    QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                    QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                }
            },
            onValueSelected = onQuickPicksChange,
        )
    }

    LanguageSelectorBottomSheet(
        show = showLanguageSelector,
        title = "Select App Language",
        languages = languageOptions,
        selectedCode = appLanguage,
        systemDefaultCode = SYSTEM_DEFAULT,
        systemDefaultLabel = "System Default",
        searchPlaceholder = "Search language...",
        onDismiss = { showLanguageSelector = false },
        onLanguageSelected = { selectedCode ->
            onAppLanguageChange(selectedCode)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val newLocale = if (selectedCode == SYSTEM_DEFAULT) {
                    Locale.getDefault()
                } else {
                    Locale.forLanguageTag(selectedCode)
                }
                setAppLocale(context, newLocale)
            }

            showLanguageSelector = false
        }
    )

    TopAppBar(
        title = { Text(stringResource(R.string.content)) },
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
        }
    )
}