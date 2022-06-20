package com.ermakov.nd.muapp.service

import android.media.MediaExtractor
import android.media.MediaFormat

val MediaExtractor.outputFormat
    get() = getTrackFormat(0)

val MediaFormat.maxInputSize
    get() = getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)

var MediaFormat.pcmEncoding
    get() = getInteger(MediaFormat.KEY_PCM_ENCODING)
    set(value) = setInteger(MediaFormat.KEY_PCM_ENCODING, value)

var MediaFormat.sampleRate
    get() = getInteger(MediaFormat.KEY_SAMPLE_RATE)
    set(value) = setInteger(MediaFormat.KEY_SAMPLE_RATE, value)

var MediaFormat.channelCount
    get() = getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    set(value) = setInteger(MediaFormat.KEY_CHANNEL_COUNT, value)

var MediaFormat.mimeType
    get() = getString(MediaFormat.KEY_MIME)
    set(value) = setString(MediaFormat.KEY_MIME, value)