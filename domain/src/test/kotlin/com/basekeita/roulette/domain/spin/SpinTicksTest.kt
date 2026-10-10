package com.basekeita.roulette.domain.spin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpinTicksTest {
    private val easing = SpinConfig.EASING

    private fun crossings(
        from: Double = 0.0,
        to: Double,
        count: Int,
        durationSeconds: Double = SpinConfig.DURATION_SECONDS,
        minIntervalSeconds: Double = 0.0,
    ): List<Double> = SpinTicks.boundaryCrossings(from, to, count, durationSeconds, easing, minIntervalSeconds)

    private fun gaps(times: List<Double>): List<Double> = times.zipWithNext { earlier, later -> later - earlier }

    @Test
    fun `またいだ境目の数だけ鳴る`() {
        assertEquals(4, crossings(to = 360.0, count = 4).size)
        assertEquals(8, crossings(to = 720.0, count = 4).size)
        assertEquals(4, crossings(from = 45.0, to = 405.0, count = 4).size)
    }

    @Test
    fun `時刻は昇順で回転時間に収まる`() {
        val times = crossings(to = 1830.0, count = 8)
        assertEquals(times.sorted(), times)
        assertTrue(times.first() > 0)
        assertTrue(times.last() <= SpinConfig.DURATION_SECONDS)
    }

    @Test
    fun `鳴る瞬間に針の下のスライスが切り替わる`() {
        val to = 1830.0
        val count = 8
        for (time in crossings(to = to, count = count)) {
            val angle = easing.progressAtTime(time / SpinConfig.DURATION_SECONDS) * to
            val before = RouletteMath.indexUnderPointer(angle - 0.05, count)
            val after = RouletteMath.indexUnderPointer(angle + 0.05, count)
            assertNotEquals(before, after)
        }
    }

    @Test
    fun `減速に合わせて間隔が開く`() {
        val gaps = gaps(crossings(to = 1830.0, count = 8, minIntervalSeconds = SpinConfig.CLICK_MIN_INTERVAL_SECONDS))
        assertTrue(gaps.size > 10)
        assertTrue(gaps.first() < gaps.last())
        // 序盤は間引きの下限まで詰まり、終盤はそれよりはっきり開く
        assertTrue(gaps.first() >= SpinConfig.CLICK_MIN_INTERVAL_SECONDS)
        assertTrue(gaps.last() > SpinConfig.CLICK_MIN_INTERVAL_SECONDS * 4)
    }

    @Test
    fun `最短間隔より詰まった時刻は間引かれる`() {
        val dense = crossings(to = 1830.0, count = 24)
        val thinned = crossings(to = 1830.0, count = 24, minIntervalSeconds = SpinConfig.CLICK_MIN_INTERVAL_SECONDS)
        assertTrue(thinned.size < dense.size)
        for (gap in gaps(thinned)) {
            assertTrue(gap >= SpinConfig.CLICK_MIN_INTERVAL_SECONDS)
        }
    }

    @Test
    fun `回らないときや項目が無いときは鳴らない`() {
        assertTrue(crossings(to = 0.0, count = 4).isEmpty())
        assertTrue(crossings(from = 100.0, to = 100.0, count = 4).isEmpty())
        assertTrue(crossings(to = 360.0, count = 0).isEmpty())
        assertTrue(crossings(to = -360.0, count = 0).isEmpty())
        assertTrue(crossings(to = -10.0, count = 4).isEmpty())
        assertTrue(crossings(to = -360.0, count = 4, durationSeconds = 0.0).isEmpty())
        assertTrue(crossings(to = 10.0, count = 4).isEmpty())
        assertTrue(crossings(to = 360.0, count = 4, durationSeconds = 0.0).isEmpty())
    }

    @Test
    fun `反時計回りでもまたいだ境目の数だけ鳴る`() {
        assertEquals(4, crossings(to = -360.0, count = 4).size)
        assertEquals(8, crossings(to = -720.0, count = 4).size)
        assertEquals(4, crossings(from = -45.0, to = -405.0, count = 4).size)
        assertEquals(1, crossings(from = 100.0, to = 50.0, count = 4).size)
    }

    @Test
    fun `反時計回りの時刻は時計回りと対称になる`() {
        val forward = crossings(from = 37.0, to = 1867.0, count = 8, minIntervalSeconds = SpinConfig.CLICK_MIN_INTERVAL_SECONDS)
        val backward = crossings(from = -37.0, to = -1867.0, count = 8, minIntervalSeconds = SpinConfig.CLICK_MIN_INTERVAL_SECONDS)
        assertTrue(backward.isNotEmpty())
        assertEquals(forward, backward)
    }

    @Test
    fun `反時計回りでも鳴る瞬間に針の下のスライスが切り替わる`() {
        val from = 200.0
        val to = from - 1830
        val count = 8
        val times = crossings(from = from, to = to, count = count)
        assertTrue(times.isNotEmpty())
        assertEquals(times.sorted(), times)
        assertTrue(times.last() <= SpinConfig.DURATION_SECONDS)
        for (time in times) {
            val angle = from + (to - from) * easing.progressAtTime(time / SpinConfig.DURATION_SECONDS)
            val before = RouletteMath.indexUnderPointer(angle + 0.05, count)
            val after = RouletteMath.indexUnderPointer(angle - 0.05, count)
            assertNotEquals(before, after)
        }
    }
}
