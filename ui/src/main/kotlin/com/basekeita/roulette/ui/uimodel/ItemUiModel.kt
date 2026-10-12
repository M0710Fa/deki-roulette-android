package com.basekeita.roulette.ui.uimodel

import androidx.compose.runtime.Immutable
import java.util.UUID

/** 項目リストの 1 行。ルーレットと順番決めの両方で使う。 */
@Immutable
data class ItemUiModel(
    val id: UUID,
    val label: String,
    /** 正規化したラベルが他の項目と一致する。印とは無関係に、同名の行すべてに同じ注意を出す。 */
    val isDuplicate: Boolean,
)
