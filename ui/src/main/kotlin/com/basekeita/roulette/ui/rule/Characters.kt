package com.basekeita.roulette.ui.rule

import java.text.BreakIterator

/**
 * 先頭から `maxCount` 文字を取る。文字は見た目の 1 文字（書記素クラスタ）で数え、絵文字やサロゲートペアを途中で切らない。
 * iOS の `String.prefix` と同じ数え方。
 */
internal fun String.takeCharacters(maxCount: Int): String {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var end = 0
    repeat(maxCount) {
        val next = iterator.next()
        if (next == BreakIterator.DONE) return this
        end = next
    }
    return substring(0, end)
}

/** 見た目の 1 文字（書記素クラスタ）で数えた文字数。iOS の `String.count` と同じ数え方。 */
internal fun String.characterCount(): Int {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var count = 0
    while (iterator.next() != BreakIterator.DONE) {
        count += 1
    }
    return count
}
