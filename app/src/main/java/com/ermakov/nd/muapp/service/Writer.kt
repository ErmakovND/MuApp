package com.ermakov.nd.muapp.service

import android.media.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import javax.inject.Inject

class Writer @Inject constructor() {

    private lateinit var samples: ReceiveChannel<ShortArray>

    fun setup(samplesChannel: ReceiveChannel<ShortArray>) {
        samples = samplesChannel
    }

    fun launchIn(scope: CoroutineScope) {
        scope.launch {
            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .build(),
                AudioTrack.getMinBufferSize(
                    44100,
                    2,
                    AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            for (sample in samples) {
                track.write(sample, 0, sample.size)
            }
        }
    }

    private val MediaFormat.sampleRate: Int
        get() = getInteger(MediaFormat.KEY_SAMPLE_RATE)

    private val MediaFormat.channelCount: Int
        get() = getInteger(MediaFormat.KEY_CHANNEL_COUNT)

    private val MediaFormat.pcmEncoding: Int
        get() = getInteger(MediaFormat.KEY_PCM_ENCODING)
}