package com.iydheko.palabrowser.ui.screens.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.iydheko.palabrowser.service.PlaybackService
import com.iydheko.palabrowser.ui.components.browser.BrowserBottomBar
import com.iydheko.palabrowser.ui.components.browser.BrowserMenuModal
import com.iydheko.palabrowser.ui.components.browser.BrowserTopBar
import com.iydheko.palabrowser.ui.components.browser.BrowserWebView
import com.iydheko.palabrowser.utils.extractDomain
import com.iydheko.palabrowser.utils.getBaseUrl
import com.iydheko.palabrowser.utils.isDeepLink
import kotlinx.coroutines.launch

@Composable
fun BrowserScreen(
        paddingValues: PaddingValues = PaddingValues(0.dp),
        navController: NavController,
        viewModel: WebViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("PalaBrowserPrefs", Context.MODE_PRIVATE)
    }
    val coroutineScope = rememberCoroutineScope()

    // Notification Permission Request
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                ) { _: Boolean ->
                    // Handle permission result
                }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val pageTitle by viewModel.pageTitle
    val favicon by viewModel.favicon
    val currentUrl by viewModel.currentUrl
    val canGoBack by viewModel.canGoBack
    val canGoForward by viewModel.canGoForward
    val isLoading by viewModel.isLoading

    var showMenu by remember { mutableStateOf(false) }
    var rememberLastPage by remember {
        mutableStateOf(sharedPrefs.getBoolean("rememberLastPage", true))
    }

    val initialUrl =
            if (rememberLastPage) {
                sharedPrefs.getString("lastUrl", "https://google.com") ?: "https://google.com"
            } else {
                "https://google.com"
            }

    var url by remember { mutableStateOf(initialUrl) }

    val webViewInstance = viewModel.getOrCreateWebView(url)

    LaunchedEffect(currentUrl, rememberLastPage) {
        if (rememberLastPage) {
            sharedPrefs.edit { putString("lastUrl", currentUrl) }
        }
    }

    val displayUrl = remember(currentUrl) { extractDomain(currentUrl) }
    val backButtonEnabled = remember(canGoBack, currentUrl) { canGoBack || isDeepLink(currentUrl) }

    val onBackClickHandler: () -> Unit = {
        if (viewModel.webView?.canGoBack() == true) {
            viewModel.webView?.goBack()
        } else if (isDeepLink(currentUrl)) {
            val baseUrl = getBaseUrl(currentUrl)
            viewModel.webView?.loadUrl(baseUrl)
        }
    }

    // Broadcast Receiver for PlaybackService commands
    DisposableEffect(context) {
        val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            PlaybackService.ACTION_REQUEST_PLAY -> {
                                viewModel.webView?.evaluateJavascript(
                                        "document.querySelector('video')?.play();",
                                        null
                                )
                            }
                            PlaybackService.ACTION_REQUEST_PAUSE -> {
                                viewModel.webView?.evaluateJavascript(
                                        "document.querySelector('video')?.pause();",
                                        null
                                )
                            }
                        }
                    }
                }
        val intentFilter =
                IntentFilter().apply {
                    addAction(PlaybackService.ACTION_REQUEST_PLAY)
                    addAction(PlaybackService.ACTION_REQUEST_PAUSE)
                }

        // Using ContextCompat.registerReceiver to handle RECEIVER_NOT_EXPORTED flags correctly
        // across different API levels, especially for targetSdk 34+.
        ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose { context.unregisterReceiver(receiver) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
                modifier =
                        Modifier.fillMaxSize().background(Color(0xFFE8B86D)).padding(paddingValues)
        ) {
            BrowserTopBar(title = pageTitle, favicon = favicon)

            BrowserWebView(
                    webViewInstance = webViewInstance,
                    modifier = Modifier.weight(1f),
                    onTitleChange = { viewModel.updateTitle(it) },
                    onIconChange = { viewModel.updateIcon(it) }
            )

            BrowserBottomBar(
                    currentUrl = displayUrl,
                    canGoBack = backButtonEnabled,
                    canGoForward = canGoForward,
                    isLoading = isLoading,
                    onBackClick = onBackClickHandler,
                    onForwardClick = { viewModel.webView?.goForward() },
                    onReloadClick = { viewModel.webView?.reload() },
                    onMenuClick = { showMenu = !showMenu },
            )
        }
        BrowserMenuModal(
                showMenu = showMenu,
                rememberLastPage = rememberLastPage,
                onRememberLastPageChanged = {
                    rememberLastPage = it
                    coroutineScope.launch {
                        sharedPrefs.edit { putBoolean("rememberLastPage", it) }
                    }
                },
                onClose = { showMenu = false },
                navController = navController
        )
    }
}
