package com.malopieds.innertune.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.malopieds.innertube.YouTube
import com.malopieds.innertune.LocalPlayerAwareWindowInsets
import com.malopieds.innertune.R
import com.malopieds.innertune.constants.*
import com.malopieds.innertune.ui.component.IconButton
import com.malopieds.innertune.ui.utils.backToMain
import com.malopieds.innertune.utils.rememberPreference
import com.malopieds.innertune.utils.reportException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun clearLoginData(
    setVisitorData: (String) -> Unit,
    setInnerTubeCookie: (String) -> Unit,
    setAccountName: (String) -> Unit,
    setAccountEmail: (String) -> Unit,
    setAccountChannelHandle: (String) -> Unit,
) {
    // Limpiar cookies del WebView
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }

    // Limpiar todas las preferencias relacionadas con la cuenta
    setVisitorData("")
    setInnerTubeCookie("")
    setAccountName("")
    setAccountEmail("")
    setAccountChannelHandle("")
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(navController: NavController) {
    var visitorData by rememberPreference(VisitorDataKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    var showMenu by remember { mutableStateOf(false) }

    // Add isLoggedIn state
    val isLoggedIn = remember(accountName, innerTubeCookie) {
        accountName.isNotEmpty() && innerTubeCookie.isNotEmpty()
    }

    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(
                        view: WebView,
                        url: String,
                        isReload: Boolean,
                    ) {
                        if (url.startsWith("https://music.youtube.com")) {
                            innerTubeCookie = CookieManager.getInstance().getCookie(url)
                            GlobalScope.launch {
                                YouTube.accountInfo()
                                    .onSuccess {
                                        accountName = it.name
                                        accountEmail = it.email.orEmpty()
                                        accountChannelHandle = it.channelHandle.orEmpty()
                                        navController.navigateUp()
                                    }
                                    .onFailure {
                                        reportException(it)
                                    }
                            }
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onRetrieveVisitorData(newVisitorData: String?) {
                            if (newVisitorData != null) {
                                visitorData = newVisitorData
                            }
                        }
                    },
                    "Android",
                )
                webView = this
                loadUrl(
                    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F",
                )
            }
        },
    )

    TopAppBar(
        title = { Text(stringResource(R.string.login)) },
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

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}

