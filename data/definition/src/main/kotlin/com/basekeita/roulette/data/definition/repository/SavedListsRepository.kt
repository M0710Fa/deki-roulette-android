package com.basekeita.roulette.data.definition.repository

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.SavedList
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 名前を付けて保存した項目リスト。保存先は画面に依らず 1 つ。 */
interface SavedListsRepository {
    /** 保存済みのリスト。無い・読めないときは空。 */
    val lists: Flow<List<SavedList>>

    /** 末尾に保存する。名前が空、または上限に達しているときは保存せず false。 */
    suspend fun save(
        name: String,
        items: List<Item>,
    ): Boolean

    /** 名前を付け替える。名前が空、または該当が無いときは変えず false。 */
    suspend fun rename(
        id: UUID,
        name: String,
    ): Boolean

    /** 削除する。該当が無いときは false。 */
    suspend fun delete(id: UUID): Boolean
}
