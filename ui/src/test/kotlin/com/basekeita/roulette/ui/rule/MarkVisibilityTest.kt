package com.basekeita.roulette.ui.rule

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkVisibilityTest {
    @Test
    fun `押している間か指定直後だけ出す`() {
        assertTrue(MarkVisibility.reveals(concealed = false, captured = false, pressing = true, hinting = false))
        assertTrue(MarkVisibility.reveals(concealed = false, captured = false, pressing = false, hinting = true))
        assertFalse(MarkVisibility.reveals(concealed = false, captured = false, pressing = false, hinting = false))
    }

    @Test
    fun `伏せているときは押していても出さない`() {
        assertFalse(MarkVisibility.reveals(concealed = true, captured = false, pressing = true, hinting = true))
    }

    @Test
    fun `キャプチャ中は押していても指定直後でも出さない`() {
        assertFalse(MarkVisibility.reveals(concealed = false, captured = true, pressing = true, hinting = false))
        assertFalse(MarkVisibility.reveals(concealed = false, captured = true, pressing = false, hinting = true))
    }
}
