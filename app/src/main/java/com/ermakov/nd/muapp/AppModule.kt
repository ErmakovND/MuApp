package com.ermakov.nd.muapp

import com.ermakov.nd.muapp.data.datasource.AudioDataSource
import com.ermakov.nd.muapp.data.datasource.LocalAudioDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideAudioDataSource(
        localAudioDataSource: LocalAudioDataSource
    ): AudioDataSource = localAudioDataSource
}