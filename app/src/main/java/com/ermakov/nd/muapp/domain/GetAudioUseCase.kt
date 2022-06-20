package com.ermakov.nd.muapp.domain

import com.ermakov.nd.muapp.data.repository.AudioRepository
import javax.inject.Inject

class GetAudioUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    suspend operator fun invoke() = audioRepository.getAudio()
}