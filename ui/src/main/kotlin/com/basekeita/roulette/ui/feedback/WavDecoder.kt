package com.basekeita.roulette.ui.feedback

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WAV ファイルの波形を -1..1 の Float に読む。回転音の素材（`res/raw/click.wav`）を `ClickTrack` に渡すために使う。
 * 対応するのは整数 PCM の 16 bit と 32 bit 浮動小数点。複数チャンネルは平均してモノラルにする。読めなければ null。
 */
internal object WavDecoder {
    /** 読み込んだ波形。`samples` はモノラル。 */
    class Decoded(
        val samples: FloatArray,
        val sampleRate: Int,
    )

    private const val FORMAT_PCM = 1
    private const val FORMAT_IEEE_FLOAT = 3
    private const val RIFF_HEADER_SIZE = 12
    private const val CHUNK_HEADER_SIZE = 8
    private const val FMT_CHUNK_MIN_SIZE = 16
    private const val PCM_16_MAX = 32768f

    fun decode(bytes: ByteArray): Decoded? {
        if (bytes.size < RIFF_HEADER_SIZE) return null

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (bytes.ascii(0) != "RIFF") return null

        if (bytes.ascii(8) != "WAVE") return null

        var format: Format? = null
        var position = RIFF_HEADER_SIZE
        while (position + CHUNK_HEADER_SIZE <= bytes.size) {
            val chunkId = bytes.ascii(position)
            val chunkSize = buffer.getInt(position + 4)
            val bodyStart = position + CHUNK_HEADER_SIZE
            if (chunkSize < 0 || bodyStart + chunkSize > bytes.size) return null

            if (chunkId == "fmt " && chunkSize >= FMT_CHUNK_MIN_SIZE) {
                format = Format(
                    audioFormat = buffer.getShort(bodyStart).toInt(),
                    channels = buffer.getShort(bodyStart + 2).toInt(),
                    sampleRate = buffer.getInt(bodyStart + 4),
                    bitsPerSample = buffer.getShort(bodyStart + 14).toInt(),
                )
            }
            if (chunkId == "data") {
                val readFormat = format ?: return null
                return readSamples(buffer, bodyStart, chunkSize, readFormat)
            }
            // チャンクは偶数バイトにそろえて並ぶ
            position = bodyStart + chunkSize + (chunkSize and 1)
        }
        return null
    }

    private class Format(
        val audioFormat: Int,
        val channels: Int,
        val sampleRate: Int,
        val bitsPerSample: Int,
    )

    private fun readSamples(
        buffer: ByteBuffer,
        start: Int,
        size: Int,
        format: Format,
    ): Decoded? {
        if (format.channels <= 0 || format.sampleRate <= 0) return null

        val readSample: (Int) -> Float =
            if (format.audioFormat == FORMAT_PCM && format.bitsPerSample == 16) {
                { offset -> buffer.getShort(offset) / PCM_16_MAX }
            } else if (format.audioFormat == FORMAT_IEEE_FLOAT && format.bitsPerSample == 32) {
                { offset -> buffer.getFloat(offset) }
            } else {
                return null
            }
        val bytesPerSample = format.bitsPerSample / 8
        val frameSize = bytesPerSample * format.channels
        val frameCount = size / frameSize
        val samples =
            FloatArray(frameCount) { frame ->
                val frameStart = start + frame * frameSize
                val sum = (0 until format.channels).sumOf { channel -> readSample(frameStart + channel * bytesPerSample).toDouble() }
                (sum / format.channels).toFloat()
            }
        return Decoded(samples = samples, sampleRate = format.sampleRate)
    }

    private fun ByteArray.ascii(offset: Int): String = String(this, offset, 4, Charsets.US_ASCII)
}
