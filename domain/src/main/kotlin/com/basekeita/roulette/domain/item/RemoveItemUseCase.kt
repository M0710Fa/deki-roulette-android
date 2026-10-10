package com.basekeita.roulette.domain.item

import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.RemovedItem
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/** 項目を 1 件削除して保存する。 */
class RemoveItemUseCase
@Inject
constructor(
    private val itemsRepository: ItemsRepository,
) {
    /** 削除した項目と元の位置を返す（「元に戻す」に使う）。該当が無いときは null。 */
    suspend operator fun invoke(
        kind: ListKind,
        id: UUID,
    ): RemovedItem? {
        val current = itemsRepository.items(kind).first()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return null

        itemsRepository.save(kind, current.filterIndexed { position, _ -> position != index })
        return RemovedItem(current[index], index)
    }
}
