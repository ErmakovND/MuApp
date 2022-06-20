package com.ermakov.nd.muapp.ui.viewmodel

import android.support.v4.media.MediaBrowserCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ermakov.nd.muapp.model.Audio
import com.ermakov.nd.muapp.service.AudioServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AudioListViewModel @Inject constructor(
    private val audioServiceController: AudioServiceController,
) : ViewModel() {

    var state by mutableStateOf(AudioListState())
        private set

    fun connect() = audioServiceController.connect()

    fun getAudio() {
        val root = audioServiceController.getRoot()
        audioServiceController.subscribe(root, object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                state = AudioListState(children.map {
                    Audio(
                        0,
                        it.description.title.toString(),
                        it.description.mediaUri.toString(),
                        it.description.mediaUri!!
                    )
                })
            }
        })
    }

    fun play() {
        audioServiceController.transportControls.play()
    }

    fun pause() {
        audioServiceController.transportControls.pause()
    }
}

data class AudioListState(
    var audioList: List<Audio> = emptyList()
)