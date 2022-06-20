package com.ermakov.nd.muapp.service

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AudioServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, AudioService::class.java),
        MediaBrowserConnectionCallback(),
        null
    )

    private lateinit var mediaController: MediaControllerCompat

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    fun connect() {
        Log.d("Audio Controller", "Connecting")
        mediaBrowser.connect()
    }

    fun getRoot() = mediaBrowser.root

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String) {
        mediaBrowser.unsubscribe(parentId)
    }

    private inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Log.d("Audio Controller", "Connection suspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Log.d("Audio Controller", "Connection Failed")
        }
    }
}