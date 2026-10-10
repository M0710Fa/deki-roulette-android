package com.basekeita.roulette.data.definition.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedListTest {
    @Test
    fun `名前は空白を潰して上限で切る`() {
        assertEquals("ランチ 候補", SavedList.normalizeName("  ランチ   候補 \n"))
        val long = "あ".repeat(SavedList.MAX_NAME_LENGTH + 5)
        assertEquals(SavedList.MAX_NAME_LENGTH, SavedList.normalizeName(long).length)
    }

    @Test
    fun `空白だけの名前は空文字になる`() {
        assertTrue(SavedList.normalizeName(" \n ").isEmpty())
    }
}
