package com.basekeita.roulette.domain.spin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CubicBezierCurveTest {
    private val spin = SpinConfig.EASING
    private val linear = CubicBezierCurve(0.25, 0.25, 0.75, 0.75)

    @Test
    fun `端点は0と1`() {
        assertEquals(0.0, spin.progressAtTime(0.0), 1e-9)
        assertEquals(1.0, spin.progressAtTime(1.0), 1e-9)
        assertEquals(0.0, spin.timeAtProgress(0.0), 1e-9)
        assertEquals(1.0, spin.timeAtProgress(1.0), 1e-9)
    }

    @Test
    fun `対角線上の制御点なら恒等写像`() {
        for (step in 0..20) {
            val time = step / 20.0
            assertEquals(time, linear.progressAtTime(time), 1e-6)
            assertEquals(time, linear.timeAtProgress(time), 1e-6)
        }
    }

    @Test
    fun `スピンの曲線は前半で大きく進んで減速する`() {
        // 半分の時刻で 9 割以上進んでいる（cubic-bezier(0.15, 0.85, 0.3, 1) は強いイーズアウト）
        assertTrue(spin.progressAtTime(0.5) > 0.9)
        // 序盤の傾きは終盤より大きい
        val early = spin.progressAtTime(0.1) - spin.progressAtTime(0.0)
        val late = spin.progressAtTime(1.0) - spin.progressAtTime(0.9)
        assertTrue(early > late * 10)
    }

    @Test
    fun `進み具合は時刻に対して単調増加`() {
        var previous = -1.0
        for (step in 0..100) {
            val value = spin.progressAtTime(step / 100.0)
            assertTrue(value >= previous)
            previous = value
        }
    }

    @Test
    fun `時刻と進み具合は互いに逆関数`() {
        for (step in 1 until 20) {
            val time = step / 20.0
            assertEquals(time, spin.timeAtProgress(spin.progressAtTime(time)), 1e-6)
        }
    }

    @Test
    fun `範囲外の入力は端に丸める`() {
        assertEquals(0.0, spin.progressAtTime(-0.5), 1e-9)
        assertEquals(1.0, spin.progressAtTime(1.5), 1e-9)
    }
}
