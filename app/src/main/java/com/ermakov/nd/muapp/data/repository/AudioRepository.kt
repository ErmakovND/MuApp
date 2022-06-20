package com.ermakov.nd.muapp.data.repository

import com.ermakov.nd.muapp.data.datasource.AudioDataSource
import javax.inject.Inject

class AudioRepository @Inject constructor(
    private val audioDataSource: AudioDataSource
) {
    suspend fun getAudio() = audioDataSource.getAudio()
}