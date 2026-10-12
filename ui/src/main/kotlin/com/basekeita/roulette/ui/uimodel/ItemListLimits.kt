package com.basekeita.roulette.ui.uimodel

import androidx.compose.runtime.Immutable

/**
 * 項目リストの上限と下限。値の正は `data:definition` のモデル（`Item` / `SavedList`）にあり、
 * ui はそれに依存しないため、画面から渡してもらう。
 */
@Immutable
data class ItemListLimits(
    /** 抽選を始められる最少の件数。 */
    val minItemCount: Int,
    /** 1 つのリストに置ける件数の上限。 */
    val maxItemCount: Int,
    /** ラベルの最大文字数。 */
    val maxLabelLength: Int,
    /** 保存できるリストの数。 */
    val maxSavedListCount: Int,
)
