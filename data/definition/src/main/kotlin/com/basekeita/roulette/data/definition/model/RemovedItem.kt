package com.basekeita.roulette.data.definition.model

/** 削除した項目と、削除前にあった位置。「元に戻す」で同じ位置に差し戻すために使う。 */
data class RemovedItem(
    val item: Item,
    val index: Int,
)
