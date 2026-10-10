package com.basekeita.roulette.domain.haptic

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.domain.order.RevealTiming
import com.basekeita.roulette.domain.spin.RouletteMath
import com.basekeita.roulette.domain.spin.SpinConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HapticScheduleTest {
    private val easing = SpinConfig.EASING
    private val duration = SpinConfig.DURATION_SECONDS

    private fun crossings(
        from: Double,
        to: Double,
        count: Int,
        minIntervalSeconds: Double = 0.0,
    ): List<Double> = HapticSchedule.boundaryCrossings(from, to, count, duration, easing, minIntervalSeconds)

    /** 開始からの秒 `time` における回転角。回転のアニメーションと同じ曲線で補間する。 */
    private fun rotationAt(
        time: Double,
        from: Double,
        to: Double,
    ): Double = from + (to - from) * easing.progressAtTime(time / duration)

    @Test
    fun `境目の数は通過するスライスの数に等しい`() {
        // 0 度から 2 周 + 45 度、4 スライス（90 度）なら 90, 180, ..., 720 の 8 本を越える
        assertEquals(8, crossings(0.0, 765.0, 4).size)
        // 開始位置がちょうど境目のときは、その境目は数えない
        assertEquals(7, crossings(90.0, 765.0, 4).size)
        // 終了位置がちょうど境目のときは、その境目を数える
        assertEquals(8, crossings(0.0, 720.0, 4).size)
    }

    @Test
    fun `各時刻で針の下の添字が切り替わる`() {
        val random = Random(5)
        repeat(200) {
            val count = random.nextInt(2, Item.MAX_COUNT + 1)
            val from = random.nextDouble(0.0, 3600.0)
            val to = RouletteMath.nextRotation(from, 0, count, random)
            for (time in crossings(from, to, count)) {
                val before = RouletteMath.indexUnderPointer(rotationAt(time - 1e-4, from, to), count)
                val after = RouletteMath.indexUnderPointer(rotationAt(time + 1e-4, from, to), count)
                assertNotEquals(before, after)
            }
        }
    }

    @Test
    fun `時刻は昇順で回転時間の中に収まる`() {
        val times = crossings(123.0, 123.0 + 6 * 360 + 200, 7)
        assertTrue(times.isNotEmpty())
        assertEquals(times.sorted(), times)
        assertTrue(times.first() > 0)
        assertTrue(times.last() <= duration + 1e-9)
    }

    @Test
    fun `終盤は減速に合わせて間隔が開く`() {
        val times = crossings(0.0, 6 * 360.0, 4)
        val firstGap = times[1] - times[0]
        val lastGap = times[times.size - 1] - times[times.size - 2]
        assertTrue(lastGap > firstGap * 5)
    }

    @Test
    fun `最短間隔で間引く`() {
        val all = crossings(0.0, 8 * 360.0, 24)
        val throttled = crossings(0.0, 8 * 360.0, 24, SpinConfig.HAPTIC_MIN_INTERVAL_SECONDS)
        assertTrue(throttled.size < all.size)
        for ((earlier, later) in throttled.zipWithNext()) {
            assertTrue(later - earlier >= SpinConfig.HAPTIC_MIN_INTERVAL_SECONDS - 1e-9)
        }
        // 最初の境目と、減速した終盤の境目は残る
        assertEquals(all.first(), throttled.first(), 0.0)
        assertEquals(all.last(), throttled.last(), 0.0)
    }

    @Test
    fun `回らないなら何も鳴らない`() {
        assertTrue(crossings(100.0, 100.0, 4).isEmpty())
        assertTrue(crossings(0.0, 360.0, 0).isEmpty())
        assertTrue(crossings(0.0, -360.0, 0).isEmpty())
        // 1 スライスしか進まないうちに止まる
        assertTrue(crossings(10.0, 80.0, 4).isEmpty())
        assertTrue(crossings(80.0, 10.0, 4).isEmpty())
    }

    @Test
    fun `反時計回りでも境目の数は通過するスライスの数に等しい`() {
        assertEquals(8, crossings(0.0, -765.0, 4).size)
        // 開始位置がちょうど境目のときは、その境目は数えない
        assertEquals(7, crossings(-90.0, -765.0, 4).size)
        // 終了位置がちょうど境目のときは、その境目を数える
        assertEquals(8, crossings(0.0, -720.0, 4).size)
        assertEquals(1, crossings(100.0, 50.0, 4).size)
    }

    @Test
    fun `反時計回りでも各時刻で針の下の添字が切り替わる`() {
        val random = Random(6)
        repeat(200) {
            val count = random.nextInt(2, Item.MAX_COUNT + 1)
            val from = random.nextDouble(-3600.0, 3600.0)
            val to = from - random.nextDouble(4 * 360.0, 10 * 360.0)
            val times = crossings(from, to, count)
            assertTrue(times.isNotEmpty())
            assertEquals(times.sorted(), times)
            assertTrue(times.last() <= duration + 1e-9)
            for (time in times) {
                val before = RouletteMath.indexUnderPointer(rotationAt(time - 1e-4, from, to), count)
                val after = RouletteMath.indexUnderPointer(rotationAt(time + 1e-4, from, to), count)
                assertNotEquals(before, after)
            }
        }
    }

    @Test
    fun `反時計回りの時刻は時計回りと対称になる`() {
        val forward = crossings(123.0, 123.0 + 6 * 360 + 200, 7, SpinConfig.HAPTIC_MIN_INTERVAL_SECONDS)
        val backward = crossings(-123.0, -(123.0 + 6 * 360 + 200), 7, SpinConfig.HAPTIC_MIN_INTERVAL_SECONDS)
        assertTrue(backward.isNotEmpty())
        assertEquals(forward, backward)
    }

    @Test
    fun `順番決めは行が現れる時刻に刻む`() {
        val ticks = HapticSchedule.revealTicks(4, reducedMotion = false)
        assertEquals(4, ticks.size)
        ticks.forEachIndexed { index, time ->
            assertEquals(RevealTiming.delay(index, reducedMotion = false), time, 1e-9)
        }
        assertTrue(HapticSchedule.revealTicks(4, reducedMotion = true).isEmpty())
        assertTrue(HapticSchedule.revealTicks(0, reducedMotion = false).isEmpty())
    }
}
