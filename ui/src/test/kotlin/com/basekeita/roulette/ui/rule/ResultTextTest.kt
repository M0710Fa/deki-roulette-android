package com.basekeita.roulette.ui.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTextTest {
    @Test
    fun `ルーレットの結果は見出しとラベルを半角コロンでつなぐ`() {
        assertEquals("結果: ラーメン", ResultText.roulette(label = "ラーメン", heading = "結果"))
        assertEquals("Result: Ramen", ResultText.roulette(label = "Ramen", heading = "Result"))
    }

    @Test
    fun `順番決めの結果は1始まりの番号付きで改行区切り`() {
        assertEquals("1. A チーム\n2. C チーム\n3. B チーム", ResultText.order(listOf("A チーム", "C チーム", "B チーム")))
    }

    @Test
    fun `順番決めの結果が空なら空文字`() {
        assertEquals("", ResultText.order(emptyList()))
    }

    @Test
    fun `共有本文は本文の後に空行を挟んでアプリ名を足す`() {
        val body = ResultText.order(listOf("A", "B"))
        assertEquals("1. A\n2. B\n\nデキレーレット", ResultText.share(body, appName = "デキレーレット"))
        assertEquals("Result: Ramen\n\nDekiRoulette", ResultText.share("Result: Ramen", appName = "DekiRoulette"))
    }

    @Test
    fun `共有本文はコピー本文で始まりアプリ名で終わる`() {
        val body = ResultText.roulette(label = "寿司", heading = "結果")
        val shared = ResultText.share(body, appName = "デキレーレット")
        assertTrue(shared.startsWith(body))
        assertTrue(shared.endsWith("デキレーレット"))
        // 結果以外の情報（指定の有無など）を混ぜていないこと
        assertEquals(3, shared.split("\n").size)
    }
}
