package com.basekeita.roulette.data.definition.repository

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import kotlinx.coroutines.flow.Flow

/** 画面ごとの項目リスト。当たり・先頭・末尾の指定は渡さない（保存しない仕様。SPEC §12）。 */
interface ItemsRepository {
    /** 保存済みの項目。無い・読めないときは初期項目（そのときの言語）。 */
    fun items(kind: ListKind): Flow<List<Item>>

    suspend fun save(
        kind: ListKind,
        items: List<Item>,
    )

    /** 保存データを消す。以降は初期項目（そのときの言語）に追従する。 */
    suspend fun reset(kind: ListKind)
}
