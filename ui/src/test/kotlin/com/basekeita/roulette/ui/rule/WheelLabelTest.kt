package com.basekeita.roulette.ui.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class WheelLabelTest {
    private val reference = WheelLabel.REFERENCE_DIAMETER

    /** 項目の件数の上限（`Item.MAX_COUNT`）。ui は data:definition に依存しないので値で持つ。 */
    private val maxItemCount = 24

    @Test
    fun `基準の直径では仕様の表どおりになる`() {
        for (count in 2..5) {
            assertEquals(10, WheelLabel.limit(count, reference))
            assertEquals(13.0, WheelLabel.fontSize(count, reference), 0.0)
        }
        for (count in 6..8) {
            assertEquals(7, WheelLabel.limit(count, reference))
            assertEquals(11.0, WheelLabel.fontSize(count, reference), 0.0)
        }
        for (count in 9..maxItemCount) {
            assertEquals(5, WheelLabel.limit(count, reference))
            assertEquals(9.0, WheelLabel.fontSize(count, reference), 0.0)
        }
    }

    @Test
    fun `項目が1件のときは中央に大きく出す`() {
        assertEquals(15.0, WheelLabel.fontSize(1, reference), 0.0)
        assertEquals(10, WheelLabel.limit(1, reference))
    }

    @Test
    fun `基準より小さい盤面では文字数を変えず文字サイズだけ縮める`() {
        val diameter = reference * 0.875
        assertEquals(10, WheelLabel.limit(4, diameter))
        assertEquals(5, WheelLabel.limit(12, diameter))
        assertEquals(13 * 0.875, WheelLabel.fontSize(4, diameter), 1e-9)
        assertEquals(9 * 0.875, WheelLabel.fontSize(12, diameter), 1e-9)
    }

    @Test
    fun `基準より大きい盤面では文字数と文字サイズを平方根ずつ増やす`() {
        // 480dp（横並び）では倍率 1.5 の平方根 ≒ 1.22
        val diameter = 480.0
        assertEquals(12, WheelLabel.limit(4, diameter))
        assertEquals(8, WheelLabel.limit(7, diameter))
        assertEquals(6, WheelLabel.limit(12, diameter))
        assertEquals(13 * sqrt(1.5), WheelLabel.fontSize(4, diameter), 1e-9)
        assertEquals(9 * sqrt(1.5), WheelLabel.fontSize(12, diameter), 1e-9)
    }

    @Test
    fun `最長ラベルが盤面に占める割合は基準を超えない`() {
        for (count in 1..maxItemCount) {
            val baseline = WheelLabel.baseLimit(count) * WheelLabel.baseFontSize(count) / reference
            var diameter = 80.0
            while (diameter <= 1200.0) {
                val occupied = WheelLabel.limit(count, diameter) * WheelLabel.fontSize(count, diameter) / diameter
                assertTrue("count=$count diameter=$diameter", occupied <= baseline + 1e-9)
                diameter += 20.0
            }
        }
    }

    @Test
    fun `文字数と文字サイズは盤面が大きいほど減らない`() {
        for (count in listOf(2, 6, 9)) {
            var previousLimit = 0
            var previousFont = 0.0
            var diameter = 0.0
            while (diameter <= 1200.0) {
                val limit = WheelLabel.limit(count, diameter)
                val font = WheelLabel.fontSize(count, diameter)
                assertTrue(limit >= previousLimit)
                assertTrue(font >= previousFont)
                previousLimit = limit
                previousFont = font
                diameter += 10.0
            }
        }
    }

    @Test
    fun `直径0でも落ちない`() {
        assertEquals(10, WheelLabel.limit(4, 0.0))
        assertEquals(0.0, WheelLabel.fontSize(4, 0.0), 0.0)
    }

    @Test
    fun `上限を超えるラベルだけ省略記号を付ける`() {
        assertEquals("ラーメン大盛りセット", WheelLabel.truncate("ラーメン大盛りセット", 10))
        assertEquals("ラーメン大盛りセット…", WheelLabel.truncate("ラーメン大盛りセット追加", 10))
        // 文字数は見た目の 1 文字で数える
        assertEquals("🍜🍛🍣🍖🍕…", WheelLabel.truncate("🍜🍛🍣🍖🍕🍔", 5))
    }

    @Test
    fun `ラベルは半径方向に沿わせ左半分は返す`() {
        assertEquals(0.0, WheelLabel.rotation(90.0), 0.0)
        assertEquals(-45.0, WheelLabel.rotation(45.0), 0.0)
        assertEquals(90.0, WheelLabel.rotation(180.0), 0.0)
        assertEquals(360.0, WheelLabel.rotation(270.0), 0.0)
        assertEquals(405.0, WheelLabel.rotation(315.0), 0.0)
    }
}
