package com.ermakov.nd.muapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ermakov.nd.muapp.ui.viewmodel.AudioListViewModel


@Composable
fun AudioListScreen(
    viewModel: AudioListViewModel = hiltViewModel()
) {
    val state = viewModel.state
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.audioList.forEachIndexed { index, audio ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = audio.name)
            }
        }
        Button(onClick = { viewModel.connect() }) {
            Text("Connect")
        }
        Button(onClick = { viewModel.getAudio() }) {
            Text("Get Audio")
        }
        Button(onClick = { viewModel.play() }) {
            Text("Play")
        }
        Button(onClick = { viewModel.pause() }) {
            Text("Pause")
        }
    }
}