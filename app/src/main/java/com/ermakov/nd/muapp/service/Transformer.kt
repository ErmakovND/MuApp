package com.ermakov.nd.muapp.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import playground.SignalAnalyzer
import java.nio.FloatBuffer
import javax.inject.Inject
import kotlin.math.min

class Transformer @Inject constructor(
    private val analyzer: SignalAnalyzer
) {

    companion object {
        const val CAPACITY = 10
    }

    private val winlen = 1024
    private val synhop = 512
    private val corlen = winlen - synhop

    private val window = analyzer.window(winlen)

    fun transform(
        domainFrames: ReceiveChannel<Analyzer.Frame>,
        targetTempos: ReceiveChannel<Float>,
        scope: CoroutineScope
    ): ReceiveChannel<FloatArray> {
        return scope.analyze(domainFrames, targetTempos)
            .let { scope.synthesize(it) }
    }

    private fun CoroutineScope.analyze(
        domainFrames: ReceiveChannel<Analyzer.Frame>,
        targetTempos: ReceiveChannel<Float>
    ): Channel<FloatArray> {
        val chunks = Channel<FloatArray>(CAPACITY)
        launch {
            var targetTempo = targetTempos.receive()
            val tempos = arrayListOf<Float>()
            val buffer = FloatBuffer.allocate(winlen * 10)
            var offset = 0
            for (frame in domainFrames) {
                targetTempo = targetTempos.tryReceive().getOrNull() ?: targetTempo
                if (buffer.remaining() < frame.chunk.size) {
                    val domhop = (computeFactor(tempos, targetTempo) * synhop).toInt()
                    val tolerance = min(domhop, synhop)
                    buffer.flip()
                    while (buffer.remaining() > domhop + winlen + tolerance) {
                        val iref = buffer.position() + offset + synhop
                        val itrg = buffer.position() + domhop
                        offset = analyzer.corr(
                            buffer.array(), iref,
                            buffer.array(), itrg,
                            -tolerance, tolerance, corlen
                        )
                        buffer.position(itrg)
                        chunks.send(extractChunk(buffer, offset))
                    }
                    buffer.compact()
                    tempos.clear()
                }
                buffer.put(frame.chunk)
                tempos.add(frame.tempo)
            }
            chunks.close()
        }
        return chunks
    }

    private fun CoroutineScope.synthesize(chunks: ReceiveChannel<FloatArray>): Channel<FloatArray> {
        val synths = Channel<FloatArray>(CAPACITY)
        launch {
            var rest = FloatArray(synhop)
            for (chunk in chunks) {
                for (i in 0 until winlen - synhop) {
                    rest[2 * synhop - winlen + i] += chunk[i]
                }
                synths.send(rest)
                rest = chunk.copyOfRange(winlen - synhop, winlen)
            }
            synths.close()
        }
        return synths
    }

    private fun computeFactor(domainTempos: List<Float>, targetTempo: Float): Float {
        val domainTempo = domainTempos.average().toFloat()
        val factor = FloatArray(5) { i ->
            val t = domainTempo * (i + 1)
            if (t < targetTempo) targetTempo / t else t / targetTempo
        }.withIndex().minByOrNull { it.value }!!
        return if (targetTempo > domainTempo * (factor.index + 1)) factor.value else 1 / factor.value
    }

    private fun extractChunk(buffer: FloatBuffer, offset: Int): FloatArray {
        return FloatArray(winlen) { i ->
            buffer[buffer.position() + offset + i] * window[i]
        }
    }
}