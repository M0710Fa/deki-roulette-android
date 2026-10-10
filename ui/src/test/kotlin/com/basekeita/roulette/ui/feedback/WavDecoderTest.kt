package com.basekeita.roulette.ui.feedback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavDecoderTest {
    /** WAV を組み立てる。`extraChunk` を渡すと fmt と data の間に挟む。 */
    private fun wav(
        audioFormat: Int,
        channels: Int,
        sampleRate: Int,
        bitsPerSample: Int,
        data: ByteArray,
        extraChunk: ByteArray? = null,
    ): ByteArray {
        val fmt =
            ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)
                putShort(audioFormat.toShort())
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(sampleRate * channels * bitsPerSample / 8)
                putShort((channels * bitsPerSample / 8).toShort())
                putShort(bitsPerSample.toShort())
            }.array()
        val dataChunk =
            ByteBuffer.allocate(8 + data.size).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(data.size)
                put(data)
            }.array()
        val body = fmt + (extraChunk ?: ByteArray(0)) + dataChunk
        return ByteBuffer.allocate(12 + body.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(4 + body.size)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put(body)
        }.array()
    }

    private fun pcm16(vararg samples: Int): ByteArray = ByteBuffer
        .allocate(samples.size * 2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply { samples.forEach { sample -> putShort(sample.toShort()) } }
        .array()

    @Test
    fun `16bitのPCMを-1から1に読む`() {
        val decoded = WavDecoder.decode(wav(1, 1, 44_100, 16, pcm16(0, 16_384, -32_768)))
        assertNotNull(decoded)
        assertEquals(44_100, decoded!!.sampleRate)
        assertArrayEquals(floatArrayOf(0f, 0.5f, -1f), decoded.samples, 0f)
    }

    @Test
    fun `複数チャンネルは平均してモノラルにする`() {
        val decoded = WavDecoder.decode(wav(1, 2, 8_000, 16, pcm16(16_384, 0, -16_384, -16_384)))
        assertArrayEquals(floatArrayOf(0.25f, -0.5f), decoded!!.samples, 0f)
    }

    @Test
    fun `32bitの浮動小数点を読む`() {
        val data = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putFloat(0.25f).putFloat(-0.75f).array()
        val decoded = WavDecoder.decode(wav(3, 1, 48_000, 32, data))
        assertArrayEquals(floatArrayOf(0.25f, -0.75f), decoded!!.samples, 0f)
    }

    @Test
    fun `知らないチャンクは飛ばす`() {
        // 奇数バイトのチャンクは 1 バイト詰めて並ぶ
        val extra = byteArrayOf('L'.code.toByte(), 'I'.code.toByte(), 'S'.code.toByte(), 'T'.code.toByte(), 3, 0, 0, 0, 1, 2, 3, 0)
        val decoded = WavDecoder.decode(wav(1, 1, 44_100, 16, pcm16(16_384), extraChunk = extra))
        assertArrayEquals(floatArrayOf(0.5f), decoded!!.samples, 0f)
    }

    @Test
    fun `WAVでなければ読まない`() {
        assertNull(WavDecoder.decode(ByteArray(0)))
        assertNull(WavDecoder.decode("not a wav file".toByteArray()))
    }

    @Test
    fun `対応しない形式は読まない`() {
        assertNull(WavDecoder.decode(wav(1, 1, 44_100, 8, byteArrayOf(0, 1))))
    }

    @Test
    fun `同梱の回転音を読める`() {
        val decoded = WavDecoder.decode(File("src/main/res/raw/click.wav").readBytes())
        assertNotNull(decoded)
        assertEquals(44_100, decoded!!.sampleRate)
        assertEquals(1_543, decoded.samples.size)
    }
}
