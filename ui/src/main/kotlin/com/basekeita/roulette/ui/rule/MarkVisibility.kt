package com.basekeita.roulette.ui.rule

/** 印を出すかどうかの判定。Web 版 `ItemList` の `revealMarks` に対応する。 */
object MarkVisibility {
    /**
     * 印は「伏せていない」「画面がキャプチャされていない」を満たしたうえで、
     * 行を押している間か指定直後だけ出す。
     */
    fun reveals(
        concealed: Boolean,
        captured: Boolean,
        pressing: Boolean,
        hinting: Boolean,
    ): Boolean = !concealed && !captured && (pressing || hinting)
}
