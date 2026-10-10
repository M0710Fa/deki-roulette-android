package com.basekeita.roulette.ui.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeLayoutTest {
    @Test
    fun `標準サイズではラベルは1行`() {
        assertEquals(1, TypeLayout.labelMaxLines(fontScale = 0.85f))
        assertEquals(1, TypeLayout.labelMaxLines(fontScale = 1.0f))
        // しきい値のすぐ下でもまだ 1 行
        assertEquals(1, TypeLayout.labelMaxLines(fontScale = 1.3f))
    }

    @Test
    fun `アクセシビリティサイズではラベルは2行`() {
        // しきい値は 1.5
        assertEquals(2, TypeLayout.labelMaxLines(fontScale = 1.5f))
        assertEquals(2, TypeLayout.labelMaxLines(fontScale = 2.0f))
    }

    @Test
    fun `固定高さを外すのはアクセシビリティサイズだけ`() {
        assertFalse(TypeLayout.growsFixedAreas(fontScale = 1.0f))
        assertFalse(TypeLayout.growsFixedAreas(fontScale = 1.3f))
        assertTrue(TypeLayout.growsFixedAreas(fontScale = 1.5f))
        assertTrue(TypeLayout.growsFixedAreas(fontScale = 2.0f))
    }
}
