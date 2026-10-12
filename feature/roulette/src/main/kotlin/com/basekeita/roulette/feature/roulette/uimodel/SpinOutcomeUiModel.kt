package com.basekeita.roulette.feature.roulette.uimodel

import androidx.compose.runtime.Immutable

/**
 * 止まったスライス。結果の帯の枠と強調するスライスを決めるため、ラベルだけでなく盤面上の添字も持つ
 * （同じラベルの項目があっても色が一意に決まる）。
 */
@Immutable
data class SpinOutcomeUiModel(
    val index: Int,
    val label: String,
)
