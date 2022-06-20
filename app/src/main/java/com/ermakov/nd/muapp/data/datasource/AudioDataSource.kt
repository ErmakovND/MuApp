package com.ermakov.nd.muapp.data.datasource

import com.ermakov.nd.muapp.model.Audio

interface AudioDataSource {
    suspend fun getAudio(): List<Audio>
}