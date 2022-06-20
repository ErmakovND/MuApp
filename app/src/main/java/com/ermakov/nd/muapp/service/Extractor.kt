package com.ermakov.nd.muapp.service

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

class Extractor @Inject constructor() {

    companion object {
        const val TIMEOUT = 0L
        const val CAPACITY = 10
    }

    fun extractSamples(path: String, scope: CoroutineScope): ReceiveChannel<FloatArray> {
        val (extSamples, format) = scope.extract(path)
        val decSamples = scope.decode(extSamples, format)
        return scope.convert(decSamples)
    }

    private fun CoroutineScope.extract(
        path: String
    ): Pair<ReceiveChannel<Sample>, MediaFormat> {
        val extSamples = Channel<Sample>(CAPACITY)
        val extractor = createMediaExtractor(path)
        launch {
            val buffer = ByteBuffer.allocate(extractor.outputFormat.maxInputSize)
            while (extractor.readSampleData(buffer, 0) >= 0) {
                //Log.d("Extractor", "Extracting")
                extSamples.send(
                    Sample(
                        size = extractor.sampleSize.toInt(),
                        time = extractor.sampleTime,
                        data = buffer.array().clone()
                    )
                )
                buffer.clear()
                extractor.advance()
                //Log.d("Extractor", "Extracted")
            }
            extSamples.close()
            extractor.release()
        }
        return extSamples to extractor.outputFormat
    }

    private fun CoroutineScope.decode(
        extSamples: ReceiveChannel<Sample>,
        format: MediaFormat
    ): ReceiveChannel<Sample> {
        val decSamples = Channel<Sample>(CAPACITY)
        val codec = createDecoder(format).apply { start() }
        launch {
            var ipEOS = false
            var opEOS = false
            while (!opEOS) {
                if (!ipEOS) {
                    ipEOS = handleCodecIp(codec, extSamples)
                }
                if (!opEOS) {
                    opEOS = handleCodecOp(codec, decSamples)
                }
            }
            decSamples.close()
            codec.release()
        }
        return decSamples
    }

    private suspend fun handleCodecIp(
        codec: MediaCodec,
        channel: ReceiveChannel<Sample>
    ): Boolean {
        //Log.d("Decoder", "Processing input")
        val bufIdx = codec.dequeueInputBuffer(TIMEOUT)
        if (bufIdx < 0) { return false }
        val buffer = codec.getInputBuffer(bufIdx) ?: return false
        val inpRes = channel.receiveCatching()
        if (inpRes.isClosed) {
            codec.queueInputBuffer(bufIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
        val sample = inpRes.getOrThrow()
        buffer.put(sample.data)
        codec.queueInputBuffer(bufIdx, 0, sample.size, sample.time, 0)
        //Log.d("Decoder", "Processed input")
        return false
    }

    private suspend fun handleCodecOp(
        codec: MediaCodec,
        channel: SendChannel<Sample>
    ): Boolean {
        //Log.d("Decoder", "Processing output")
        val bufInf = MediaCodec.BufferInfo()
        val bufIdx = codec.dequeueOutputBuffer(bufInf, TIMEOUT)
        if (bufIdx < 0) { return false }
        val buffer = codec.getOutputBuffer(bufIdx) ?: return false
        if (bufInf.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            channel.close()
            return true
        }
        val sample = Sample.fromBuffer(buffer, bufInf)
        codec.releaseOutputBuffer(bufIdx, false)
        channel.send(sample)
        //Log.d("Decoder", "Processed output")
        return false
    }

    private fun CoroutineScope.convert(
        decSamples: ReceiveChannel<Sample>
    ): ReceiveChannel<FloatArray> {
        val resSamples = Channel<FloatArray>(CAPACITY)
        launch {
            for (samples in decSamples) {
                //Log.d("Converter", "Converting")
                val shorts = ByteBuffer
                    .wrap(samples.data)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                val floats = FloatArray(shorts.remaining() / 2) {
                    (shorts[it * 2].toFloat() + shorts[it * 2 + 1].toFloat()) / 0xF000
                }
                resSamples.send(floats)
                //Log.d("Converter", "Converted")
            }
            resSamples.close()
        }
        return resSamples
    }

    private fun createMediaExtractor(path: String) = MediaExtractor().apply {
        setDataSource(path)
        selectTrack(0)
    }

    private fun createDecoder(format: MediaFormat) = MediaCodecList(MediaCodecList.ALL_CODECS)
        .findDecoderForFormat(format)
        .let { MediaCodec.createByCodecName(it) }
        .apply {
            configure(format, null, null, 0)
        }

    private class Sample(
        val size: Int,
        val time: Long,
        val data: ByteArray
    ) {
        companion object {
            fun fromBuffer(
                buffer: ByteBuffer,
                bufInfo: MediaCodec.BufferInfo
            ) = ByteArray(buffer.remaining())
                .also { buffer.get(it) }
                .let {
                    Sample(
                        size = bufInfo.size,
                        time = bufInfo.presentationTimeUs,
                        data = it
                    )
                }
        }
    }
}
