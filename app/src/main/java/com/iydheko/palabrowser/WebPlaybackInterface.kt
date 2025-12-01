package com.iydheko.palabrowser

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import com.iydheko.palabrowser.service.PlaybackService

class WebPlaybackInterface(private val context: Context) {

    @JavascriptInterface
    fun onMediaPlay() {
        // Send a broadcast to the PlaybackService to update its state
        val intent = Intent(PlaybackService.ACTION_UPDATE_STATE).apply {
            putExtra(PlaybackService.EXTRA_PLAYBACK_STATE, PlaybackService.STATE_PLAYING)
        }
        context.sendBroadcast(intent)
    }

    @JavascriptInterface
    fun onMediaPause() {
        // Send a broadcast to the PlaybackService to update its state
        val intent = Intent(PlaybackService.ACTION_UPDATE_STATE).apply {
            putExtra(PlaybackService.EXTRA_PLAYBACK_STATE, PlaybackService.STATE_PAUSED)
        }
        context.sendBroadcast(intent)
    }
}
