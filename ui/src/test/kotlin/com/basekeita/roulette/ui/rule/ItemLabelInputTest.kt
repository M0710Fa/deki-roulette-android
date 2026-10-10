package com.basekeita.roulette.ui.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemLabelInputTest {
    private val maxLength = 20

    @Test
    fun `normalizeは連続する空白を1つにして前後を落とす`() {
        assertEquals("佐藤 太郎", ItemLabelInput.normalize(" 佐藤 　 太郎 ", maxLength))
        assertEquals("", ItemLabelInput.normalize("   ", maxLength))
    }

    @Test
    fun `splitLinesは改行で分けて行ごとに正規化し空行を除く`() {
        assertEquals(listOf("田中", "佐藤 太郎", "鈴木"), ItemLabelInput.splitLines("田中\n 佐藤  太郎 \n\n\r\n鈴木\n", maxLength))
    }

    @Test
    fun `splitLinesは1行なら1件になる`() {
        assertEquals(listOf("ラーメン"), ItemLabelInput.splitLines("  ラーメン ", maxLength))
        assertTrue(ItemLabelInput.splitLines("   \n  ", maxLength).isEmpty())
    }

    @Test
    fun `splitLinesは行ごとに上限で切る`() {
        val long = "あ".repeat(30)
        assertEquals(listOf("あ".repeat(20), "あ".repeat(20)), ItemLabelInput.splitLines("$long\n$long", maxLength))
    }

    @Test
    fun `clampLinesは改行を保ったまま行ごとに上限で切る`() {
        val long = "あ".repeat(30)
        val clamped = ItemLabelInput.clampLines("$long\n短い\n\n$long", maxLength)
        assertEquals("あ".repeat(20) + "\n短い\n\n" + "あ".repeat(20), clamped)
    }

    @Test
    fun `clampLinesは上限内の入力を変えない`() {
        assertEquals("田中\n佐藤", ItemLabelInput.clampLines("田中\n佐藤", maxLength))
        assertEquals("", ItemLabelInput.clampLines("", maxLength))
    }

    @Test
    fun `clampLinesは絵文字を途中で切らない`() {
        val family = "👨‍👩‍👧"
        assertEquals(family.repeat(20), ItemLabelInput.clampLines(family.repeat(25), maxLength))
    }
}
