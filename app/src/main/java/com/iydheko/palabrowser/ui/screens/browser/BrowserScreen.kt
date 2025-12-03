package com.iydheko.palabrowser.ui.screens.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.iydheko.palabrowser.ui.components.tabs.TabSwitcher
import com.iydheko.palabrowser.utils.extractDomain
import com.iydheko.palabrowser.utils.getBaseUrl
import com.iydheko.palabrowser.utils.isDeepLink
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
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

    // Notification Permission Request (Keep existing logic)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                ) { _: Boolean -> }

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

    // ViewModel State
    val tabs = viewModel.tabs
    val activeTabId by viewModel.activeTabId
    val currentUrl = viewModel.currentUrl
    val pageTitle = viewModel.pageTitle
    val favicon = viewModel.favicon
    val canGoBack by viewModel.canGoBack
    val canGoForward by viewModel.canGoForward
    val isLoading by viewModel.isLoading

    // UI State
    var showMenu by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }

    // Animation State
    // We use a simple boolean to trigger AnimatedContent or custom layout transition

    var rememberLastPage by remember {
        mutableStateOf(sharedPrefs.getBoolean("rememberLastPage", true))
    }

    // Initial Load Logic
    LaunchedEffect(Unit) {
        if ((tabs as List<*>).isEmpty()) {
            val initialUrl =
                    if (rememberLastPage) {
                        sharedPrefs.getString("lastUrl", "https://google.com")
                                ?: "https://google.com"
                    } else {
                        "https://google.com"
                    }
            viewModel.createNewTab(initialUrl)
        }
    }

    LaunchedEffect(currentUrl, rememberLastPage) {
        if (rememberLastPage && currentUrl.isNotEmpty()) {
            sharedPrefs.edit { putString("lastUrl", currentUrl) }
        }
    }

    val displayUrl = remember(currentUrl) { extractDomain(currentUrl) }
    val backButtonEnabled = remember(canGoBack, currentUrl) { canGoBack || isDeepLink(currentUrl) }

    val onBackClickHandler: () -> Unit = {
        if (viewModel.activeWebView?.canGoBack() == true) {
            viewModel.activeWebView?.goBack()
        } else if (isDeepLink(currentUrl)) {
            val baseUrl = getBaseUrl(currentUrl)
            viewModel.activeWebView?.loadUrl(baseUrl)
        }
    }

    // Broadcast Receiver (Keep existing logic)
    DisposableEffect(context) {
        val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            PlaybackService.ACTION_REQUEST_PLAY -> {
                                viewModel.activeWebView?.evaluateJavascript(
                                        "document.querySelector('video')?.play();",
                                        null
                                )
                            }
                            PlaybackService.ACTION_REQUEST_PAUSE -> {
                                viewModel.activeWebView?.evaluateJavascript(
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
        ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Main Layout with Shared Transition
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8B86D))) {
        SharedTransitionLayout {
            // Browser View - Always visible (no animation)
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                BrowserTopBar(title = pageTitle, favicon = favicon)

                // WebView Container
                Box(modifier = Modifier.weight(1f)) {
                    if (activeTabId != null && !showTabSwitcher) {
                        val webView = viewModel.getOrCreateWebViewForTab(activeTabId!!, context)
                        BrowserWebView(
                                webViewInstance = webView,
                                modifier = Modifier.fillMaxSize(),
                                onTitleChange = { viewModel.updateCurrentTabTitle(it) },
                                onIconChange = { viewModel.updateCurrentTabIcon(it) }
                        )
                    }
                }

                BrowserBottomBar(
                        currentUrl = displayUrl,
                        canGoBack = backButtonEnabled,
                        canGoForward = canGoForward,
                        isLoading = isLoading,
                        hasNoTabs = tabs.isEmpty(),
                        onBackClick = onBackClickHandler,
                        onForwardClick = { viewModel.activeWebView?.goForward() },
                        onReloadClick = { viewModel.activeWebView?.reload() },
                        onMenuClick = { showMenu = !showMenu },
                        onTabSwitcherClick = {
                            // Capture thumbnail before switching to tab switcher view
                            viewModel.captureThumbnailForCurrentTab()
                            showTabSwitcher = true
                        }
                )
            }

            // Tab Switcher - Overlay on top when visible
            AnimatedVisibility(
                    visible = showTabSwitcher,
                    label = "TabSwitcherView",
                    enter = fadeIn(),
                    exit = fadeOut()
            ) {
                TabSwitcher(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelected = { id ->
                            viewModel.switchToTab(id)
                            showTabSwitcher = false
                        },
                        onTabClosed = { id -> viewModel.closeTab(id) },
                        onNewTab = {
                            viewModel.createNewTab()
                            showTabSwitcher = false
                        },
                        paddingValues = paddingValues,
                        onMenuClick = { showMenu = !showMenu },
                        //                        sharedTransitionScope =
                        // this@SharedTransitionLayout,
                        //                        animatedVisibilityScope = this@AnimatedVisibility
                        )
            }
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
