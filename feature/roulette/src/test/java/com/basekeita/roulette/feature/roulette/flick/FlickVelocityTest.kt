package com.basekeita.roulette.feature.roulette.flick

import androidx.compose.ui.geometry.Offset
import com.basekeita.roulette.domain.spin.FlickSpin
import com.basekeita.roulette.domain.spin.SpinConfig
import com.basekeita.roulette.ui.rule.WheelDrag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class FlickVelocityTest {
    private val center = Offset(160f, 160f)

    /**
     * 中心から `radius` の円周上を、`start` 度から `degreesPerSecond` で `duration` 秒動いた記録。60Hz で刻む。
     * 角度は 3 時を 0 度、画面上の時計回りを正とする。
     */
    private fun arc(
        radius: Double = 100.0,
        start: Double = -90.0,
        degreesPerSecond: Double,
        duration: Double,
        startTime: Double = 0.0,
    ): List<FlickSample> {
        val steps = (duration * 60).roundToInt()
        return (0..steps).map { step ->
            val time = step / 60.0
            val radians = (start + degreesPerSecond * time) * PI / 180
            FlickSample(
                timeSeconds = startTime + time,
                position = Offset((center.x + radius * cos(radians)).toFloat(), (center.y + radius * sin(radians)).toFloat()),
            )
        }
    }

    private fun velocityOf(samples: List<FlickSample>): Double = FlickVelocity.angularVelocity(samples, center)

    @Test
    fun `時計回りに回すと正になる`() {
        assertEquals(600.0, velocityOf(arc(degreesPerSecond = 600.0, duration = 0.2)), 1.0)
    }

    @Test
    fun `反時計回りに回すと負になる`() {
        assertEquals(-600.0, velocityOf(arc(degreesPerSecond = -600.0, duration = 0.2)), 1.0)
    }

    @Test
    fun `直前の窓の動きだけを見る`() {
        // ゆっくり回してから最後の 0.15 秒だけ速く払う
        val slow = arc(degreesPerSecond = 60.0, duration = 0.5)
        val fast = arc(start = -90.0 + 30, degreesPerSecond = 900.0, duration = FLICK_SAMPLE_WINDOW_SECONDS, startTime = 0.5 + 1.0 / 60)
        assertTrue(velocityOf(slow + fast) > 700)
    }

    @Test
    fun `離す直前に少し止めても勢いを残す`() {
        // 速く払ったあと 50ms 止めてから離す。離した瞬間の速度は 0 だが、窓の平均で閾値を越える
        val samples = arc(degreesPerSecond = 900.0, duration = 0.2)
        val last = samples.last()
        val velocity = velocityOf(samples + FlickSample(last.timeSeconds + 0.05, last.position))
        assertNotNull(FlickSpin.fullSpins(velocity))
    }

    @Test
    fun `止めたまま離すと回さない`() {
        val samples = arc(degreesPerSecond = 900.0, duration = 0.2)
        val last = samples.last()
        // 窓より長く止めてから離す
        assertEquals(0.0, velocityOf(samples + FlickSample(last.timeSeconds + FLICK_SAMPLE_WINDOW_SECONDS + 1.0 / 60, last.position)), 0.0)
    }

    @Test
    fun `窓をまたぐ区間は使わない`() {
        // 速く払ったあと、止めた位置の記録が無いまま窓より長く経ってから離す
        val samples = arc(degreesPerSecond = 900.0, duration = 0.2)
        val last = samples.last()
        val stopped = Offset((center.x + 100 * cos(PI / 180 * 120)).toFloat(), (center.y + 100 * sin(PI / 180 * 120)).toFloat())
        assertEquals(0.0, velocityOf(samples + FlickSample(last.timeSeconds + FLICK_SAMPLE_WINDOW_SECONDS * 1.5, stopped)), 0.0)
    }

    @Test
    fun `中心付近で止めてから離すと回さない`() {
        // 速く回したあと中心付近へ指を入れ、窓より長く置いてから離す
        val samples = arc(degreesPerSecond = 900.0, duration = 0.2)
        val last = samples.last()
        val withStop =
            samples +
                FlickSample(last.timeSeconds + 1.0 / 60, center) +
                FlickSample(last.timeSeconds + 1.0 / 60 + FLICK_SAMPLE_WINDOW_SECONDS * 1.2, center)
        assertEquals(0.0, velocityOf(withStop), 0.0)
    }

    @Test
    fun `直線の払いでも中心のまわりの成分を拾う`() {
        // 盤面上側を右へまっすぐ 1500dp/秒
        val samples = (0..12).map { step -> FlickSample(step / 60.0, Offset(70f + 25f * step, 60f)) }
        assertTrue(velocityOf(samples) > SpinConfig.FLICK_MIN_ANGULAR_VELOCITY)
    }

    @Test
    fun `半径方向の動きは角速度にならない`() {
        val samples = (0..6).map { step -> FlickSample(step / 60.0, Offset(200f + 20f * step, 160f)) }
        assertTrue(abs(velocityOf(samples)) < 1e-6)
    }

    @Test
    fun `九時をまたいでも一周分跳ねない`() {
        // atan2 が ±180 度で折り返す位置（9 時）を時計回りにまたぐ
        assertEquals(600.0, velocityOf(arc(start = 160.0, degreesPerSecond = 600.0, duration = 0.1)), 1.0)
    }

    @Test
    fun `中心付近の点は使わない`() {
        val inside = WheelDrag.DEAD_ZONE_RADIUS - 1
        assertEquals(0.0, velocityOf(arc(radius = inside, degreesPerSecond = 3000.0, duration = 0.1)), 0.0)
    }

    @Test
    fun `中心付近を挟んだ前後の点をつながない`() {
        // 9 時 → 中心 → 3 時と払う。中心の点を除いて左右をつなぐと半周回ったことになる
        val samples =
            listOf(
                FlickSample(0.0, Offset(center.x - 100, center.y)),
                FlickSample(0.05, center),
                FlickSample(0.1, Offset(center.x + 100, center.y)),
            )
        assertEquals(0.0, velocityOf(samples), 0.0)
    }

    @Test
    fun `記録が1点以下なら0`() {
        assertEquals(0.0, velocityOf(emptyList()), 0.0)
        assertEquals(0.0, velocityOf(arc(degreesPerSecond = 600.0, duration = 0.1).take(1)), 0.0)
    }

    @Test
    fun `間が空いたら前の記録を捨てる`() {
        val buffer = FlickSampleBuffer()
        buffer.append(FlickSample(0.0, Offset(260f, 160f)))
        buffer.append(FlickSample(1.0, Offset(160f, 60f)))
        assertEquals(1, buffer.samples.size)
    }

    @Test
    fun `窓の2倍より古い点は捨てる`() {
        val buffer = FlickSampleBuffer()
        arc(degreesPerSecond = 600.0, duration = 1.0).forEach { sample -> buffer.append(sample) }
        val samples = buffer.samples
        assertTrue(samples.last().timeSeconds - samples.first().timeSeconds <= FLICK_SAMPLE_WINDOW_SECONDS * 2 + 1e-9)
    }
}
