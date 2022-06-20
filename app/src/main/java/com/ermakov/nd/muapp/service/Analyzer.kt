package com.ermakov.nd.muapp.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import playground.SignalAnalyzer
import java.nio.FloatBuffer
import javax.inject.Inject

class Analyzer @Inject constructor(
    private val analyzer: SignalAnalyzer
) {

    companion object {
        const val CAPACITY = 10
    }

    private val spcWin = 2048
    private val spcHop = 1024
    private val lagMax = 80
    private val corWin = 280

    private val rate = 44100

    fun detectFrames(
        samples: ReceiveChannel<FloatArray>,
        scope: CoroutineScope
    ): ReceiveChannel<Frame> {
        val (chunks, spects) = scope.computeSpects(samples)
        val onsets = scope.computeOnsets(spects)
        val tempos = scope.computeTempos(onsets)
        return scope.combineFrames(chunks, tempos)
    }

    private fun CoroutineScope.computeSpects(
        samples: ReceiveChannel<FloatArray>
    ): Pair<ReceiveChannel<FloatArray>, ReceiveChannel<FloatArray>> {
        val spects = Channel<FloatArray>(CAPACITY)
        val chunks = Channel<FloatArray>(corWin * 10)
        launch {
            val buffer = FloatBuffer.allocate(spcWin * 10)
            for (sample in samples) {
                if (buffer.remaining() < sample.size) {
                    buffer.flip()
                    for (i in 0 until buffer.limit() - spcWin step spcHop) {
                        spects.send(computeSpect(buffer.array(), i))
                        chunks.send(extractChunk(buffer.array(), i))
                        buffer.position(i + spcHop)
                    }
                    buffer.compact()
                }
                buffer.put(sample)
            }
            spects.close()
            chunks.close()
        }
        return chunks to spects
    }

    private fun CoroutineScope.computeOnsets(
        spects: ReceiveChannel<FloatArray>
    ): ReceiveChannel<Float> {
        val onsets = Channel<Float>(CAPACITY)
        launch {
            var prev = FloatArray(1 + spcWin / 2)
            for (curr in spects) {
                onsets.send(computeOnset(prev, curr))
                prev = curr
            }
            onsets.close()
        }
        return onsets
    }

    private fun CoroutineScope.computeTempos(
        onsets: ReceiveChannel<Float>
    ): ReceiveChannel<Float> {
        val tempos = Channel<Float>(CAPACITY)
        launch {
            val buffer = FloatBuffer.allocate(corWin + 10)
            for (onset in onsets) {
                if (!buffer.hasRemaining()) {
                    buffer.flip()
                    for (i in 0 until buffer.limit() - corWin) {
                        tempos.send(computeTempo(buffer.array(), i))
                        buffer.position(i + 1)
                    }
                    buffer.compact()
                }
                buffer.put(onset)
            }
            tempos.close()
        }
        return tempos
    }

    private fun CoroutineScope.combineFrames(
        chunks: ReceiveChannel<FloatArray>,
        tempos: ReceiveChannel<Float>
    ): ReceiveChannel<Frame> {
        val frames = Channel<Frame>(CAPACITY)
        launch {
            for (chunk in chunks) {
                frames.send(
                    Frame(
                        tempos.receive(),
                        chunk
                    )
                )
            }
            frames.close()
        }
        return frames
    }

    class Frame(
        val tempo: Float,
        val chunk: FloatArray
    )

    private fun extractChunk(wave: FloatArray, idx: Int): FloatArray {
        return wave.copyOfRange(idx, idx + spcHop)
    }

    private fun computeSpect(wave: FloatArray, idx: Int): FloatArray {
        return analyzer.spectrum(wave, spcWin, idx)
    }

    private fun computeOnset(prev: FloatArray, curr: FloatArray): Float {
        return analyzer.diff(prev, curr)
    }

    private fun computeTempo(onsets: FloatArray, idx: Int): Float {
        val corr = analyzer.correlate(
            onsets, idx,
            onsets, idx,
            0, lagMax, corWin - lagMax
        )
        val lag = FloatArray(lagMax / 2) { i ->
            val l = lagMax / 2 + i
            corr[l] + corr[l / 2] + corr[l / 4] + corr[l / 8]
        }.withIndex().maxByOrNull { it.value }!!.index + lagMax / 2
        return 60f * rate / spcHop / lag
    }
}
