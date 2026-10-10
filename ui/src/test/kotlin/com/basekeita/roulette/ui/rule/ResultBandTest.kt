package com.basekeita.roulette.ui.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ResultBandTest {
    private val diameters = listOf(120.0, 200.0, 320.0, 480.0)

    /** 項目の件数の上限（`Item.MAX_COUNT`）。ui は data:definition に依存しないので値で持つ。 */
    private val maxItemCount = 24

    @Test
    fun `基準の直径では基準の文字サイズになる`() {
        assertEquals(ResultBand.BASE_FONT_SIZE, ResultBand.fontSize(WheelLabel.REFERENCE_DIAMETER), 0.0)
        assertTrue(ResultBand.BASE_FONT_SIZE in 26.0..30.0)
    }

    @Test
    fun `文字サイズと幅と位置は直径に比例する`() {
        assertEquals(ResultBand.BASE_FONT_SIZE * 1.5, ResultBand.fontSize(480.0), 1e-9)
        assertEquals(480 * ResultBand.MAX_WIDTH_RATIO, ResultBand.maxWidth(480.0), 1e-9)
        assertEquals(ResultBand.centerOffset(320.0) * 1.5, ResultBand.centerOffset(480.0), 1e-9)
        assertEquals(ResultBand.height(320.0) * 1.5, ResultBand.height(480.0), 1e-9)
    }

    @Test
    fun `帯は中心より下に置きハブと重ならない`() {
        for (diameter in diameters) {
            val top = ResultBand.centerOffset(diameter) - ResultBand.height(diameter) / 2
            assertTrue("diameter=$diameter", top > ResultBand.hubRadius(diameter))
        }
    }

    @Test
    fun `帯はスライスの円に収まる`() {
        for (diameter in diameters) {
            // 帯はカプセル。いちばん外に出るのは下側の両端の半円
            val height = ResultBand.height(diameter)
            val halfStraight = ResultBand.maxWidth(diameter) / 2 - height / 2
            val farthest = hypot(halfStraight, ResultBand.centerOffset(diameter)) + height / 2
            assertTrue("diameter=$diameter", farthest < ResultBand.sliceRadius(diameter))
        }
    }

    @Test
    fun `帯は盤面のラベルより大きい文字で出す`() {
        for (count in 1..maxItemCount) {
            assertTrue(ResultBand.fontSize(320.0) > WheelLabel.fontSize(count, 320.0))
        }
    }

    @Test
    fun `負の直径は0として扱う`() {
        assertEquals(0.0, ResultBand.fontSize(-10.0), 0.0)
        assertEquals(0.0, ResultBand.maxWidth(-10.0), 0.0)
        assertEquals(0.0, ResultBand.centerOffset(-10.0), 0.0)
        assertEquals(0.0, ResultBand.height(-10.0), 0.0)
        assertEquals(0.0, ResultBand.sliceRadius(-10.0), 0.0)
    }
}
