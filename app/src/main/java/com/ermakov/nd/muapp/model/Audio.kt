package com.ermakov.nd.muapp.model

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat

data class Audio(
    val id: Long,
    val name: String,
    val path: String,
    val uri: Uri
) {
    fun asMediaItem(): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(id.toString())
            .setMediaUri(uri)
            .setTitle(name)
            .build()
        return MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE)
    }
}
