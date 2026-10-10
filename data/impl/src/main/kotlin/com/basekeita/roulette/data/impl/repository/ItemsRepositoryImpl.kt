package com.basekeita.roulette.data.impl.repository

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.impl.DefaultItemLabels
import com.basekeita.roulette.data.impl.storedItemsKey
import com.basekeita.roulette.data.impl.toItems
import com.basekeita.roulette.data.impl.toStored
import com.basekeita.roulette.local.DekiLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ItemsRepositoryImpl
@Inject
constructor(
    private val localDataSource: DekiLocalDataSource,
    private val defaultItemLabels: DefaultItemLabels,
) : ItemsRepository {
    /**
     * 保存データが無い間に表示した初期項目。読み直すたびに id を振り直すと、表示中の行の id と
     * UseCase が読み直した項目の id が食い違うため、ラベルが変わるまで（言語の変更まで）同じものを返す。
     */
    private val defaultItemsCache = ConcurrentHashMap<ListKind, List<Item>>()

    override fun items(kind: ListKind): Flow<List<Item>> = localDataSource.items(kind.storedItemsKey).map { stored ->
        stored?.toItems() ?: defaultItems(kind)
    }

    override suspend fun save(
        kind: ListKind,
        items: List<Item>,
    ) {
        localDataSource.saveItems(kind.storedItemsKey, items.map { it.toStored() })
    }

    override suspend fun reset(kind: ListKind) {
        localDataSource.clearItems(kind.storedItemsKey)
    }

    private fun defaultItems(kind: ListKind): List<Item> {
        val labels = defaultItemLabels.labels(kind)
        return defaultItemsCache.compute(kind) { _, cached ->
            if (cached?.map { it.label } == labels) {
                cached
            } else {
                labels.map { Item(label = it) }
            }
        }.orEmpty()
    }
}
