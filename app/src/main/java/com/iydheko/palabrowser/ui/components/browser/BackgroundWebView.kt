package com.iydheko.palabrowser.ui.components.browser

import android.content.Context
import android.webkit.WebView

class BackgroundWebView(context: Context) : WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        // Always report VISIBLE to prevent media from pausing when app is backgrounded
        super.onWindowVisibilityChanged(VISIBLE)
    }
}
