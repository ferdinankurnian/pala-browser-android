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
    // Kita simpen instance WebView di sini
    private val appContext = application.applicationContext
    @SuppressLint("StaticFieldLeak") var webView: WebView? = null

    //    private var hasLoadedInitialUrl = false

    private val _currentUrl = mutableStateOf("https://google.com")
    val currentUrl: State<String> = _currentUrl

    private val _pageTitle = mutableStateOf("Loading...")
    val pageTitle: State<String> = _pageTitle

    private val _canGoBack = mutableStateOf(false)
    val canGoBack: State<Boolean> = _canGoBack

    private val _canGoForward = mutableStateOf(false)
    val canGoForward: State<Boolean> = _canGoForward

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _favicon = mutableStateOf<Bitmap?>(null)
    val favicon: State<Bitmap?> = _favicon

    fun updateTitle(title: String?) {
        _pageTitle.value = title ?: "Untitled"
    }

    fun updateIcon(icon: Bitmap?) {
        _favicon.value = icon
    }

    // Fungsi buat inisialisasi WebView sekali aja
    fun getOrCreateWebView(initialUrl: String): WebView {
        if (webView == null) {
            webView =
                    BackgroundWebView(appContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.allowFileAccess = true
                        settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

                        addJavascriptInterface(WebPlaybackInterface(appContext), "Android")
                        webViewClient = customWebViewClient
                        webChromeClient = customWebChromeClient

                        // LOAD URL CUMAN SEKALI PAS BIKIN BARU
                        loadUrl(initialUrl)
                    }
        }

        // webView?.loadUrl(initialUrl) // HAPUS INI BIAR GAK RELOAD TERUS

        return webView!!
    }

    // Di dalam class WebViewModel:

    private val customWebViewClient =
            object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    _isLoading.value = true
                    _currentUrl.value = url ?: ""
                    // Di sini lo bisa panggil start PlaybackService kalo perlu
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    _isLoading.value = false
                    _currentUrl.value = url ?: ""
                    // Update status navigasi
                    _canGoBack.value = view?.canGoBack() ?: false
                    _canGoForward.value = view?.canGoForward() ?: false
                    view?.evaluateJavascript(INJECT_JS, null)
                }

                // Dipanggil saat navigasi terjadi, penting untuk meng-update status back/forward
                override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    _canGoBack.value = view?.canGoBack() ?: false
                    _canGoForward.value = view?.canGoForward() ?: false
                }
            }

    private val customWebChromeClient =
            object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    _pageTitle.value = title ?: "Untitled"
                }

                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    _favicon.value = icon
                }
            }

    // Opsional: Clear pas ViewModel mati total
    override fun onCleared() {
        super.onCleared()
        webView?.destroy()
        webView = null
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
