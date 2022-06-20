package com.ermakov.nd.muapp.common.util

import kotlinx.coroutines.channels.Channel

class Monitor {
    private val channels = mutableListOf<Channel<Unit>>()

    suspend fun lock() {
        val channel = Channel<Unit>()
        channels.add(channel)
        channel.receive()
    }

    suspend fun release() {
        channels.forEach { it.send(Unit) }
        channels.clear()
    }
}