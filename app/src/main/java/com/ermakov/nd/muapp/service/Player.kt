package com.ermakov.nd.muapp.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class Player @Inject constructor(
    private val analyzer: Analyzer,
    private val extractor: Extractor,
    private val transformer: Transformer
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val track = createAudioTrack()

    private val tempos = Channel<Float>()
    private val states = Channel<State>()

    private val writes = Channel<FloatArray>()

    private lateinit var writing: Job
    private var getting: Job? = null

    fun play(path: String) {
        launch {
            getting?.cancelAndJoin()
        }
        val samples = extractor.extractSamples(path, this)
        val frames = analyzer.detectFrames(samples, this)
        val synths = transformer.transform(frames, tempos, this)
    }

    fun setup(paths: List<String>) {
        writing = launch {
            tempos.send(120f)
            var state = State.PAUSED
            select {
                states.onReceive {
                    state = it
                }
                if (state == State.PLAYING) {
                    writes.onReceive {
                        track.write(it, 0, it.size, AudioTrack.WRITE_BLOCKING)
                    }
                }
            }
        }
    }

    fun play() {
        launch {
            states.send(State.PLAYING)
        }
    }

    fun pause() {
        launch {
            states.send(State.PAUSED)
        }
    }

    private fun createAudioTrack() = AudioTrack(
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build(),
        AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        ),
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    )

    private enum class State {
        PAUSED,
        PLAYING
    }
}