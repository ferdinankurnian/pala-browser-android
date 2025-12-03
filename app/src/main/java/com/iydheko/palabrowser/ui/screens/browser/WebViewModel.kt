package com.iydheko.palabrowser.ui.screens.browser

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.iydheko.palabrowser.WebPlaybackInterface
import com.iydheko.palabrowser.ui.components.browser.BackgroundWebView

class WebViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext

    @SuppressLint("StaticFieldLeak") private val webViewPool = mutableMapOf<String, WebView>()

    val activeWebView: WebView?
        get() = webViewPool[activeTabId.value]

    // Tab Data Class
    data class BrowserTab(
            val id: String = java.util.UUID.randomUUID().toString(),
            val url: String = "https://google.com",
            val title: String = "New Tab",
            val favicon: Bitmap? = null,
            val thumbnail: Bitmap? = null // Snapshot for the switcher
    )

    private val _tabs = androidx.compose.runtime.mutableStateListOf<BrowserTab>()
    val tabs: List<BrowserTab>
        get() = _tabs

    private val _activeTabId = mutableStateOf<String?>(null)
    val activeTabId: State<String?> = _activeTabId

    // Computed properties for the active tab
    val currentTab: BrowserTab?
        get() = _tabs.find { it.id == _activeTabId.value }

    val currentUrl: String
        get() = currentTab?.url ?: ""
    val pageTitle: String
        get() = currentTab?.title ?: ""
    val favicon: Bitmap?
        get() = currentTab?.favicon

    // Navigation state for the ACTIVE tab
    private val _canGoBack = mutableStateOf(false)
    val canGoBack: State<Boolean> = _canGoBack

    private val _canGoForward = mutableStateOf(false)
    val canGoForward: State<Boolean> = _canGoForward

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun createNewTab(url: String = "https://google.com") {
        val newTab = BrowserTab(url = url)
        _tabs.add(newTab)
        switchToTab(newTab.id)
    }

    fun closeTab(tabId: String) {
        val tabIndex = _tabs.indexOfFirst { it.id == tabId }
        if (tabIndex == -1) return

        // Destroy and remove the associated WebView
        webViewPool.remove(tabId)?.destroy()

        _tabs.removeAt(tabIndex)

        if (_activeTabId.value == tabId) {
            // If we closed the active tab, switch to the one before it, or the one after, or null
            if (_tabs.isNotEmpty()) {
                val newIndex = if (tabIndex > 0) tabIndex - 1 else 0
                switchToTab(_tabs[newIndex].id, captureThumbnail = false)
            } else {
                _activeTabId.value = null
            }
        }
    }

    fun switchToTab(tabId: String, captureThumbnail: Boolean = true) {
        // 1. Capture thumbnail of the CURRENT tab before switching away
        if (captureThumbnail) {
            val currentId = _activeTabId.value
            if (currentId != null && activeWebView != null) {
                captureThumbnail(currentId)
            }
        }

        // 2. Update active ID
        _activeTabId.value = tabId

        // 3. Update nav state for the new tab
        val newWebView = activeWebView
        _canGoBack.value = newWebView?.canGoBack() ?: false
        _canGoForward.value = newWebView?.canGoForward() ?: false
        // NOTE: isLoading is not persisted per-tab in this model, it's global.
    }

    fun updateCurrentTabUrl(url: String) {
        val currentId = _activeTabId.value ?: return
        val index = _tabs.indexOfFirst { it.id == currentId }
        if (index != -1) {
            _tabs[index] = _tabs[index].copy(url = url)
        }
    }

    fun updateCurrentTabTitle(title: String?) {
        val currentId = _activeTabId.value ?: return
        val index = _tabs.indexOfFirst { it.id == currentId }
        if (index != -1) {
            _tabs[index] = _tabs[index].copy(title = title ?: "Untitled")
        }
    }

    fun updateCurrentTabIcon(icon: Bitmap?) {
        val currentId = _activeTabId.value ?: return
        val index = _tabs.indexOfFirst { it.id == currentId }
        if (index != -1) {
            _tabs[index] = _tabs[index].copy(favicon = icon)
        }
    }

    fun captureThumbnailForCurrentTab() {
        val currentId = _activeTabId.value ?: return
        captureThumbnail(currentId)
    }

    fun captureThumbnail(tabId: String) {
        val webView = activeWebView ?: return
        try {
            // Create bitmap from WebView
            val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            // Get current scroll position
            val scrollX = webView.scrollX
            val scrollY = webView.scrollY

            // Translate the canvas to draw the visible part of the WebView
            canvas.translate((-scrollX).toFloat(), (-scrollY).toFloat())

            webView.draw(canvas)

            val index = _tabs.indexOfFirst { it.id == tabId }
            if (index != -1) {
                _tabs[index] = _tabs[index].copy(thumbnail = bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Initialize WebView
    @SuppressLint("StaticFieldLeak")
    fun getOrCreateWebViewForTab(tabId: String, context: android.content.Context): WebView {
        return webViewPool.getOrPut(tabId) {
            val tab = _tabs.find { it.id == tabId }
            BackgroundWebView(context)
                    .apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.allowFileAccess = true
                        settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

                        addJavascriptInterface(WebPlaybackInterface(context), "Android")
                        webViewClient = customWebViewClient
                        webChromeClient = customWebChromeClient
                    }
                    .also { webView ->
                        tab?.let {
                            // Only load the URL if it's not the default "about:blank" or if it
                            // hasn't been loaded before.
                            // This prevents re-loading on configuration changes if the webview is
                            // re-attached.
                            if (webView.url == null || webView.url == "about:blank") {
                                webView.loadUrl(it.url)
                            }
                        }
                    }
        }
    }

    private val customWebViewClient =
            object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Only update loading state if the view is the currently active one
                    if (view === activeWebView) {
                        _isLoading.value = true
                    }
                    if (url != null) updateCurrentTabUrl(url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (view === activeWebView) {
                        _isLoading.value = false
                        _canGoBack.value = view?.canGoBack() ?: false
                        _canGoForward.value = view?.canGoForward() ?: false
                    }
                    if (url != null) updateCurrentTabUrl(url)
                    view?.evaluateJavascript(INJECT_JS, null)
                }

                override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    if (view === activeWebView) {
                        _canGoBack.value = view?.canGoBack() ?: false
                        _canGoForward.value = view?.canGoForward() ?: false
                    }
                }
            }

    private val customWebChromeClient =
            object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    updateCurrentTabTitle(title)
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    updateCurrentTabIcon(icon)
                }
            }

    override fun onCleared() {
        super.onCleared()
        // Destroy all WebViews in the pool
        webViewPool.values.forEach { it.destroy() }
        webViewPool.clear()
    }
}

private const val INJECT_JS =
        """
    (function() {
        // Mock Page Visibility API to trick sites like YouTube
        Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
        Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
        
        const originalAddEventListener = document.addEventListener;
        document.addEventListener = function(type, listener, options) {
            if (type === 'visibilitychange') {
                return; // Ignore visibility change listeners
            }
            return originalAddEventListener.call(document, type, listener, options);
        };

        function setupMediaListeners(mediaElement) {
            mediaElement.addEventListener('play', function() {
                Android.onMediaPlay();
            });
            mediaElement.addEventListener('pause', function() {
                Android.onMediaPause();
            });
            // Initial state check
            if (mediaElement.paused) {
                Android.onMediaPause();
            } else {
                Android.onMediaPlay();
            }
        }

        document.querySelectorAll('video, audio').forEach(setupMediaListeners);

        // Listen for new media elements added dynamically
        const observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                mutation.addedNodes.forEach(function(node) {
                    if (node.nodeType === 1 && (node.tagName === 'VIDEO' || node.tagName === 'AUDIO')) {
                        setupMediaListeners(node);
                    }
                    if (node.nodeType === 1 && node.querySelector) {
                        node.querySelectorAll('video, audio').forEach(setupMediaListeners);
                    }
                });
            });
        });

        observer.observe(document.body, { childList: true, subtree: true });
    })();
"""
