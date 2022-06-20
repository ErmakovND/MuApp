package com.ermakov.nd.muapp.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.ermakov.nd.muapp.data.repository.AudioRepository
import com.ermakov.nd.muapp.model.Audio
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOG_TAG = "AudioService"
private const val MEDIA_ROOT_ID = "MediaRoot"

@AndroidEntryPoint
class AudioService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var audioRepository: AudioRepository

    @Inject
    lateinit var player: Player

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    private var audioList = listOf<Audio>()

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(audioList.map { it.asMediaItem() }.toMutableList())
    }

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(applicationContext, LOG_TAG).apply {
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
            setPlaybackState(stateBuilder.build())

            setCallback(MediaSessionCallback(applicationContext))

            setSessionToken(sessionToken)
        }

        serviceScope.launch {
            audioList = audioRepository.getAudio()
            player.setup(audioList.map { it.path })
        }
    }

    private inner class MediaSessionCallback(context: Context) : MediaSessionCompat.Callback() {
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        override fun onPlay() {
            if (AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            ) {
                startService(Intent(applicationContext, MediaBrowserService::class.java))
                mediaSession.isActive = true
                player.play()
            }
        }

        override fun onPause() {
            player.pause()
        }

        override fun onStop() {
            AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
            stopService(Intent(applicationContext, MediaBrowserService::class.java))
            mediaSession.isActive = false
        }

        override fun onSetPlaybackSpeed(speed: Float) {
            Log.d(LOG_TAG, "onSetPlaybackSpeed")
        }

        private val audioFocusRequest =
            AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributesCompat.Builder()
                        .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener {}
                .build()
    }
}