package com.basekeita.roulette.domain.item

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 項目をまとめて追加して保存する。1 件の追加も同じ（要素 1 つのリストで渡す）。 */
class AddItemsUseCase
@Inject
constructor(
    private val itemsRepository: ItemsRepository,
) {
    /** ラベルを正規化して空になったものを除き、上限に収まる分だけ末尾に足す。追加できた件数を返す。 */
    suspend operator fun invoke(
        kind: ListKind,
        rawLabels: List<String>,
    ): Int {
        val current = itemsRepository.items(kind).first()
        val labels = rawLabels.map { Item.normalizeLabel(it) }.filter { it.isNotEmpty() }
        val accepted = labels.take(maxOf(0, Item.MAX_COUNT - current.size))
        if (accepted.isEmpty()) return 0

        itemsRepository.save(kind, current + accepted.map { Item(label = it) })
        return accepted.size
    }
}
