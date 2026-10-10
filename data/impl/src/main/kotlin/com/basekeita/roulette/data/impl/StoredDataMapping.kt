package com.basekeita.roulette.data.impl

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.local.StoredItem
import com.basekeita.roulette.local.StoredItemsKey
import com.basekeita.roulette.local.StoredSavedList
import java.util.UUID

internal val ListKind.storedItemsKey: StoredItemsKey
    get() =
        when (this) {
            ListKind.ROULETTE -> StoredItemsKey.ROULETTE
            ListKind.ORDER -> StoredItemsKey.ORDER
        }

internal fun Item.toStored(): StoredItem = StoredItem(id = id, label = label)

internal fun SavedList.toStored(): StoredSavedList = StoredSavedList(id = id, name = name, items = items.map { it.toStored() })

/**
 * 読み込んだ項目を追加時と同じ正規化に通し、空になったもの・id が重複するものは落とし、上限で切る（iOS の `ItemStore.sanitize`）。
 */
internal fun List<StoredItem>.toItems(): List<Item> {
    val seenIds = mutableSetOf<UUID>()
    val items = mutableListOf<Item>()
    for (stored in this) {
        val label = Item.normalizeLabel(stored.label)
        if (label.isEmpty()) continue

        if (!seenIds.add(stored.id)) continue

        items += Item(id = stored.id, label = label)
        if (items.size == Item.MAX_COUNT) break
    }
    return items
}

/**
 * 名前が空になるリスト・id が重複するリストは落とし、上限で切る。項目は `toItems` と同じ規則で整える（iOS の `SavedListStore.decode`）。
 */
internal fun List<StoredSavedList>.toSavedLists(): List<SavedList> {
    val seenIds = mutableSetOf<UUID>()
    val lists = mutableListOf<SavedList>()
    for (stored in this) {
        val name = SavedList.normalizeName(stored.name)
        if (name.isEmpty()) continue

        if (!seenIds.add(stored.id)) continue

        lists += SavedList(id = stored.id, name = name, items = stored.items.toItems())
        if (lists.size == SavedList.MAX_COUNT) break
    }
    return lists
}
