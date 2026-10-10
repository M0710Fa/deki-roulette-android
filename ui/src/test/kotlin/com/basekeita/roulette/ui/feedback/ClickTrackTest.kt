package com.basekeita.roulette.ui.feedback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickTrackTest {
    private val rate = 1000
    private val click = floatArrayOf(1f, -0.5f, 0.25f)

    @Test
    fun `鳴らす時刻が無ければ空`() {
        assertTrue(ClickTrack.render(click, rate, emptyList(), gain = 1f).isEmpty())
        assertTrue(ClickTrack.render(FloatArray(0), rate, listOf(0.0), gain = 1f).isEmpty())
    }

    @Test
    fun `指定した位置にクリック音が入る`() {
        val track = ClickTrack.render(click, rate, listOf(0.0, 0.1), gain = 1f)
        assertEquals(100 + click.size, track.size)
        assertArrayEquals(click, track.copyOfRange(0, 3), 0f)
        assertArrayEquals(click, track.copyOfRange(100, 103), 0f)
        assertEquals(0f, track[50])
    }

    @Test
    fun `音量を掛ける`() {
        val track = ClickTrack.render(click, rate, listOf(0.0), gain = 0.5f)
        assertArrayEquals(floatArrayOf(0.5f, -0.25f, 0.125f), track, 0f)
    }

    @Test
    fun `重なった分は足し合わされて範囲に収まる`() {
        val track = ClickTrack.render(floatArrayOf(1f, 1f), rate, listOf(0.0, 0.001), gain = 1f)
        assertArrayEquals(floatArrayOf(1f, 1f, 1f), track, 0f)
    }
}
