package com.basekeita.roulette.domain.spin

/**
 * スピンの結果。結果表示の色を止まったスライスに合わせるため、ラベルだけでなく盤面上の添字も持つ
 * （同じラベルの項目があっても色が一意に決まる）。
 */
data class SpinOutcome(
    val index: Int,
    val label: String,
)
