package com.basekeita.roulette.domain.order

import org.junit.Assert.assertEquals
import org.junit.Test

class RevealTimingTest {
    @Test
    fun `遅延は添字に比例する`() {
        assertEquals(0.0, RevealTiming.delay(0, reducedMotion = false), 0.0)
        assertEquals(3 * RevealTiming.STEP_SECONDS, RevealTiming.delay(3, reducedMotion = false), 1e-12)
    }

    @Test
    fun `動きを減らす設定では遅延なし`() {
        assertEquals(0.0, RevealTiming.delay(5, reducedMotion = true), 0.0)
        assertEquals(RevealTiming.REDUCED_MOTION_DURATION_SECONDS, RevealTiming.duration(24, reducedMotion = true), 0.0)
    }

    @Test
    fun `演出の長さは最後の行が出終わる時刻`() {
        val expected = 3 * RevealTiming.STEP_SECONDS + RevealTiming.ANIMATION_SECONDS
        assertEquals(expected, RevealTiming.duration(4, reducedMotion = false), 1e-9)
    }
}
