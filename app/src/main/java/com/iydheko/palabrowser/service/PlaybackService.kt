package com.iydheko.palabrowser.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.iydheko.palabrowser.R

class PlaybackService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager

    private val stateReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        ACTION_UPDATE_STATE -> {
                            val newState =
                                    intent.getIntExtra(
                                            EXTRA_PLAYBACK_STATE,
                                            PlaybackStateCompat.STATE_NONE
                                    )
                            if (newState != PlaybackStateCompat.STATE_NONE) {
                                updatePlaybackState(newState)
                            }
                        }
                        ACTION_UPDATE_TITLE -> {
                            val newTitle = intent.getStringExtra(EXTRA_TITLE)
                            if (!newTitle.isNullOrEmpty()) {
                                val currentMetadata =
                                        mediaSession.controller.metadata
                                                ?: MediaMetadataCompat.Builder().build()
                                val updatedMetadata =
                                        MediaMetadataCompat.Builder(currentMetadata)
                                                .putString(
                                                        MediaMetadataCompat.METADATA_KEY_TITLE,
                                                        newTitle
                                                )
                                                .build()
                                mediaSession.setMetadata(updatedMetadata)
                                // Update notification to reflect new title
                                notificationManager.notify(
                                        NOTIFICATION_ID,
                                        buildMediaNotification(
                                                mediaSession.controller.playbackState?.state
                                                        ?: PlaybackStateCompat.STATE_PAUSED
                                        )
                                )
                            }
                        }
                    }
                }
            }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        initializeMediaSession()
        startForegroundService()

        val intentFilter =
                IntentFilter().apply {
                    addAction(ACTION_UPDATE_STATE)
                    addAction(ACTION_UPDATE_TITLE)
                }
        // Using ContextCompat.registerReceiver to handle RECEIVER_NOT_EXPORTED flags correctly
        // across different API levels, especially for targetSdk 34+.
        ContextCompat.registerReceiver(
                this,
                stateReceiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "PlaybackService")

        mediaSession.setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        super.onPlay()
                        Log.d(
                                "PlaybackService",
                                "onPlay called. Updating state and sending broadcast."
                        )
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        sendBroadcast(Intent(ACTION_REQUEST_PLAY))
                    }

                    override fun onPause() {
                        super.onPause()
                        Log.d(
                                "PlaybackService",
                                "onPause called. Updating state and sending broadcast."
                        )
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        sendBroadcast(Intent(ACTION_REQUEST_PAUSE))
                    }

                    override fun onStop() {
                        super.onStop()
                        Log.d("PlaybackService", "onStop called. Stopping service.")
                        stopSelf()
                    }
                }
        )
        mediaSession.isActive = true
        // Set initial playback state
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)

        // Set some dummy metadata for now
        mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Web Media")
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Pala Browser")
                        .build()
        )
    }

    private fun updatePlaybackState(newState: Int) {
        val stateBuilder =
                PlaybackStateCompat.Builder()
                        .setActions(
                                PlaybackStateCompat.ACTION_PLAY or
                                        PlaybackStateCompat.ACTION_PAUSE or
                                        PlaybackStateCompat.ACTION_STOP
                        )
                        .setState(newState, 0, 1.0f) // position, playback speed

        mediaSession.setPlaybackState(stateBuilder.build())
        notificationManager.notify(NOTIFICATION_ID, buildMediaNotification(newState))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notification = buildMediaNotification(PlaybackStateCompat.STATE_PAUSED) // Initial state

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildMediaNotification(state: Int): android.app.Notification {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata?.description

        val playPauseAction =
                if (state == PlaybackStateCompat.STATE_PLAYING) {
                    NotificationCompat.Action(
                            android.R.drawable.ic_media_pause,
                            "Pause",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this,
                                    PlaybackStateCompat.ACTION_PAUSE
                            )
                    )
                } else {
                    NotificationCompat.Action(
                            android.R.drawable.ic_media_play,
                            "Play",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this,
                                    PlaybackStateCompat.ACTION_PLAY
                            )
                    )
                }

        // Action for stopping the service
        val stopAction =
                NotificationCompat.Action(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackStateCompat.ACTION_STOP
                        )
                )

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .apply {
                    setContentTitle(description?.title ?: "Web Media")
                    setContentText(description?.subtitle ?: "Playing in Pala Browser")
                    setSubText(description?.description)
                    setLargeIcon(description?.iconBitmap)
                    setContentIntent(controller.sessionActivity)
                    setDeleteIntent(
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this@PlaybackService,
                                    PlaybackStateCompat.ACTION_STOP
                            )
                    )
                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    setSmallIcon(R.drawable.progress_activity_24px)
                    addAction(playPauseAction)
                    addAction(stopAction) // Add stop action to notification
                    setStyle(
                            androidx.media.app.NotificationCompat.MediaStyle()
                                    .setMediaSession(mediaSession.sessionToken)
                                    .setShowActionsInCompactView(0, 1)
                    ) // Show play/pause and stop in compact view
                }
                .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel =
                    NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_LOW
                    )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        Log.d("PlaybackService", "onDestroy called. Releasing resources.")
        unregisterReceiver(stateReceiver)
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "PlaybackServiceChannel"
        const val CHANNEL_NAME = "Playback Service"

        const val ACTION_START = "com.iydheko.palabrowser.service.action.START"
        const val ACTION_STOP = "com.iydheko.palabrowser.service.action.STOP"

        const val ACTION_REQUEST_PLAY = "com.iydheko.palabrowser.service.action.REQUEST_PLAY"
        const val ACTION_REQUEST_PAUSE = "com.iydheko.palabrowser.service.action.REQUEST_PAUSE"

        const val ACTION_UPDATE_STATE = "com.iydheko.palabrowser.service.action.UPDATE_STATE"
        const val EXTRA_PLAYBACK_STATE = "playback_state"
        const val STATE_PLAYING = 1
        const val STATE_PAUSED = 2

        const val ACTION_UPDATE_TITLE = "com.iydheko.palabrowser.service.action.UPDATE_TITLE"
        const val EXTRA_TITLE = "extra_title"

        fun start(context: Context) {
            val intent =
                    Intent(context, PlaybackService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
