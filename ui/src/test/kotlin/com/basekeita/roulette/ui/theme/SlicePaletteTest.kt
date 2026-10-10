package com.basekeita.roulette.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlicePaletteTest {
    private val maxCount = 24

    private fun indices(
        count: Int,
        paletteSize: Int,
    ): List<Int> = (0 until count).map { position -> SlicePalette.index(position, count, paletteSize) }

    @Test
    fun `どの件数でも隣り合うスライスは同じ色にならない`() {
        for (paletteSize in listOf(10, 12)) {
            for (count in 2..maxCount) {
                val colors = indices(count, paletteSize)
                for (position in 1 until count) {
                    assertNotEquals("count=$count position=$position", colors[position - 1], colors[position])
                }
                if (count >= 3) {
                    assertNotEquals("count=$count の継ぎ目", colors[0], colors[count - 1])
                }
            }
        }
    }

    @Test
    fun `ずらすのは継ぎ目が重なる件数の末尾だけ`() {
        for (paletteSize in listOf(10, 12)) {
            for (count in 1..maxCount) {
                val colors = indices(count, paletteSize)
                for (position in 0 until count) {
                    val shifted = count > 1 && count % paletteSize == 1 && position == count - 1
                    if (shifted) {
                        assertNotEquals("count=$count position=$position", position % paletteSize, colors[position])
                    } else {
                        assertEquals("count=$count position=$position", position % paletteSize, colors[position])
                    }
                }
            }
        }
    }

    @Test
    fun `十色では11件と21件の末尾をずらす`() {
        assertEquals(4, indices(11, 10).last())
        assertEquals(4, indices(21, 10).last())
        assertEquals(1, indices(12, 10).last())
    }

    @Test
    fun `戻り値はパレットの範囲に収まる`() {
        for (count in 1..maxCount) {
            assertTrue(indices(count, 10).all { index -> index in 0 until 10 })
        }
    }

    @Test
    fun `盤面の塗りと色見本は継ぎ目で同じようにずらす`() {
        val shifted = SlicePalette.index(10, 11, DekiFixedColors.sliceColors.size)
        assertEquals(DekiFixedColors.sliceColors[shifted], sliceColor(10, 11))
        for (scheme in listOf(lightDekiColorScheme, darkDekiColorScheme)) {
            assertEquals(scheme.sliceAccents[shifted], scheme.sliceAccent(10, 11))
        }
        assertNotEquals(sliceColor(0, 11), sliceColor(10, 11))
        assertEquals(sliceColor(0, 12), sliceColor(10, 12))
    }
}
