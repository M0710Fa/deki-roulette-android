package com.basekeita.roulette.data.definition.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemTest {
    @Test
    fun `前後の空白を落とし連続する空白を1つにする`() {
        assertEquals("ラーメン 大盛り", Item.normalizeLabel("  ラーメン   大盛り \n"))
    }

    @Test
    fun `全角の空白も空白として潰す`() {
        assertEquals("ラーメン 大盛り", Item.normalizeLabel("　ラーメン　　大盛り\t"))
    }

    @Test
    fun `上限の文字数で切る`() {
        val long = "あ".repeat(30)
        assertEquals(Item.MAX_LABEL_LENGTH, Item.normalizeLabel(long).length)
    }

    @Test
    fun `サロゲートペアの文字は1文字として数え途中で切らない`() {
        val emoji = "😀".repeat(30)
        val normalized = Item.normalizeLabel(emoji)
        assertEquals("😀".repeat(Item.MAX_LABEL_LENGTH), normalized)
    }

    @Test
    fun `空白だけなら空文字になる`() {
        assertTrue(Item.normalizeLabel("   ").isEmpty())
    }

    @Test
    fun `idを省くとラベルごとに別のIDを振る`() {
        val items = listOf("A", "B", "A").map { Item(label = it) }
        assertEquals(listOf("A", "B", "A"), items.map { it.label })
        assertEquals(3, items.map { it.id }.toSet().size)
    }
}
