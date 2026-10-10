package com.basekeita.roulette.domain.item

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.RemovedItem
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 削除した項目を元の位置に戻して保存する。指定（当たり・先頭・末尾）は復元しない。 */
class RestoreItemUseCase
@Inject
constructor(
    private val itemsRepository: ItemsRepository,
) {
    /** 同じ項目がすでにあるとき、上限に達しているときは何もしない。元の位置が範囲外なら末尾に戻す。 */
    suspend operator fun invoke(
        kind: ListKind,
        removed: RemovedItem,
    ) {
        val current = itemsRepository.items(kind).first()
        if (current.any { it.id == removed.item.id }) return

        if (current.size >= Item.MAX_COUNT) return

        val restored = current.toMutableList()
        restored.add(minOf(removed.index, current.size), removed.item)
        itemsRepository.save(kind, restored)
    }
}
