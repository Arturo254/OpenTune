package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.DisableScreenshotKey
import com.arturo254.opentune.constants.PauseListenHistoryKey
import com.arturo254.opentune.constants.PauseSearchHistoryKey
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.PreferenceEntry
import com.arturo254.opentune.ui.component.SettingsGeneralCategory
import com.arturo254.opentune.ui.component.SettingsPage
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val database = LocalDatabase.current
    val (pauseListenHistory, onPauseListenHistoryChange) = rememberPreference(
        key = PauseListenHistoryKey,
        defaultValue = false
    )
    val (pauseSearchHistory, onPauseSearchHistoryChange) = rememberPreference(
        key = PauseSearchHistoryKey,
        defaultValue = false
    )
    val (disableScreenshot, onDisableScreenshotChange) = rememberPreference(
        key = DisableScreenshotKey,
        defaultValue = false
    )

    var showClearListenHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(R.string.privacy),
        navController = navController,
        scrollBehavior = scrollBehavior,
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.listen_history),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.pause_listen_history)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    checked = pauseListenHistory,
                    onCheckedChange = onPauseListenHistoryChange
                )},

                {PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_listen_history)) },
                    icon = { Icon(painterResource(R.drawable.delete_history), null) },
                    onClick = { showClearListenHistoryDialog = true }
                )},
            )
        )
        SettingsGeneralCategory(
            title = stringResource(R.string.search_history),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.pause_search_history)) },
                    icon = { Icon(painterResource(R.drawable.search_off), null) },
                    checked = pauseSearchHistory,
                    onCheckedChange = onPauseSearchHistoryChange
                )},

                {PreferenceEntry(
                    title = { Text(stringResource(R.string.clear_search_history)) },
                    icon = { Icon(painterResource(R.drawable.clear_all), null) },
                    onClick = { showClearSearchHistoryDialog = true }
                )},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.disable_screenshot)) },
                    description = stringResource(R.string.disable_screenshot_desc),
                    icon = { Icon(painterResource(R.drawable.screenshot), null) },
                    checked = disableScreenshot,
                    onCheckedChange = onDisableScreenshotChange
                )},
            )
        )
    }


    if (showClearListenHistoryDialog) {
        ConfirmationDialog2(
            title = stringResource(R.string.clear_listen_history),
            message = stringResource(R.string.clear_listen_history_confirm),
            onDismiss = { showClearListenHistoryDialog = false },
            onConfirm = {
                showClearListenHistoryDialog = false
                database.query { clearListenHistory() }
            }
        )
    }

    if (showClearSearchHistoryDialog) {
        ConfirmationDialog2(
            title = stringResource(R.string.clear_search_history),
            message = stringResource(R.string.clear_search_history_confirm),
            onDismiss = { showClearSearchHistoryDialog = false },
            onConfirm = {
                showClearSearchHistoryDialog = false
                database.query { clearSearchHistory() }
            }
        )
    }
}

@Composable
private fun ConfirmationDialog2(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(android.R.string.ok),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}