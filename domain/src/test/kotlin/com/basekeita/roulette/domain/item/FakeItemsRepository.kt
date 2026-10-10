package com.basekeita.roulette.domain.item

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** メモリに持つ `ItemsRepository`。`reset` は `defaultItems` に戻す。 */
class FakeItemsRepository(
    initial: Map<ListKind, List<Item>> = emptyMap(),
    private val defaultItems: List<Item> = emptyList(),
) : ItemsRepository {
    private val stored = ListKind.entries.associateWith { MutableStateFlow(initial[it] ?: defaultItems) }

    /** `save` が呼ばれた回数。保存の有無を確かめるために使う。 */
    var saveCount = 0
        private set

    fun current(kind: ListKind): List<Item> = stored.getValue(kind).value

    override fun items(kind: ListKind): Flow<List<Item>> = stored.getValue(kind)

    override suspend fun save(
        kind: ListKind,
        items: List<Item>,
    ) {
        saveCount += 1
        stored.getValue(kind).value = items
    }

    override suspend fun reset(kind: ListKind) {
        stored.getValue(kind).value = defaultItems
    }
}
