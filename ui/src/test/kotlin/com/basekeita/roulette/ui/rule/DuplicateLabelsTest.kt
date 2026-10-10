package com.basekeita.roulette.ui.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DuplicateLabelsTest {
    private val maxLength = 20

    private fun labeled(vararg labels: String): List<Pair<UUID, String>> = labels.map { label -> UUID.randomUUID() to label }

    private fun ids(labels: List<Pair<UUID, String>>): Set<UUID> = DuplicateLabels.ids(labels, maxLength)

    @Test
    fun `重複がなければ空になる`() {
        assertTrue(ids(labeled("A", "B", "C")).isEmpty())
    }

    @Test
    fun `空配列なら空になる`() {
        assertTrue(ids(emptyList()).isEmpty())
    }

    @Test
    fun `同名の2件を両方返す`() {
        val items = labeled("A", "B", "A")
        assertEquals(setOf(items[0].first, items[2].first), ids(items))
    }

    @Test
    fun `同名の3件以上をすべて返す`() {
        val items = labeled("A", "A", "B", "A")
        assertEquals(setOf(items[0].first, items[1].first, items[3].first), ids(items))
    }

    @Test
    fun `複数の組をまとめて返す`() {
        val items = labeled("A", "B", "C", "B", "A")
        assertEquals(setOf(items[0].first, items[1].first, items[3].first, items[4].first), ids(items))
    }

    @Test
    fun `大文字小文字の違いは同名でない`() {
        assertTrue(ids(labeled("Ramen", "ramen", "RAMEN")).isEmpty())
    }

    @Test
    fun `全角半角の違いは同名でない`() {
        assertTrue(ids(labeled("ABC", "ＡＢＣ", "ｶﾚｰ", "カレー")).isEmpty())
    }

    @Test
    fun `正規化すると一致するラベルは同名とみなす`() {
        val items = labeled("佐藤 太郎", " 佐藤   太郎 ")
        assertEquals(items.map { (id, _) -> id }.toSet(), ids(items))
    }

    @Test
    fun `追加するラベルが既存と同名なら衝突する`() {
        assertTrue(DuplicateLabels.conflicts(listOf("B"), listOf("A", "B"), maxLength))
        assertTrue(DuplicateLabels.conflicts(listOf("  B "), listOf("A", "B"), maxLength))
    }

    @Test
    fun `追加するラベルが既存と違えば衝突しない`() {
        assertFalse(DuplicateLabels.conflicts(listOf("C"), listOf("A", "B"), maxLength))
        assertFalse(DuplicateLabels.conflicts(listOf("a", "Ｂ"), listOf("A", "B"), maxLength))
    }

    @Test
    fun `入力内の行同士が同名なら衝突する`() {
        assertTrue(DuplicateLabels.conflicts(listOf("C", "D", "C"), listOf("A"), maxLength))
        assertTrue(DuplicateLabels.conflicts(listOf("C", "C"), emptyList(), maxLength))
    }

    @Test
    fun `複数行の一部だけが既存と同名でも衝突する`() {
        assertTrue(DuplicateLabels.conflicts(listOf("C", "A", "D"), listOf("A", "B"), maxLength))
    }

    @Test
    fun `空のラベルは数えない`() {
        assertFalse(DuplicateLabels.conflicts(listOf("", "  ", "C"), emptyList(), maxLength))
        assertFalse(DuplicateLabels.conflicts(emptyList(), listOf("A", "A"), maxLength))
    }
}
