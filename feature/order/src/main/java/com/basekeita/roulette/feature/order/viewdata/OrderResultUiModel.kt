package com.basekeita.roulette.feature.order.viewdata

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * 並べ替えの結果。
 *
 * @property shuffleCount 並べ替えた回数。同じ並びが続いても、結果の行を作り直して演出をやり直すために使う
 * @property rows 1 位から順に並べた行
 */
@Immutable
data class OrderResultUiModel(
    val shuffleCount: Int,
    val rows: List<OrderedItemUiModel>,
)

/**
 * 結果の 1 行。
 *
 * @property revealDelayMillis 結果が出始めてからこの行が現れるまでの時間
 */
@Immutable
data class OrderedItemUiModel(
    val id: UUID,
    val label: String,
    val revealDelayMillis: Int,
)
