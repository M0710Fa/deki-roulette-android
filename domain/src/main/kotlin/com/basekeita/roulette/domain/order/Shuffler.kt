package com.basekeita.roulette.domain.order

import com.basekeita.roulette.data.definition.model.Item
import java.util.UUID
import kotlin.random.Random

/** 指定された項目を先頭・末尾へ移す並べ替え。Web 版 `src/shuffle.ts` に対応する。 */
object Shuffler {
    /**
     * 一様にシャッフル（標準ライブラリの `shuffled` は Fisher-Yates）してから、指定された項目を目的の位置と入れ替える。
     * 条件を満たすまで引き直す方式は、指定以外の並びに偏りを持ち込む。
     */
    fun arrange(
        items: List<Item>,
        firstId: UUID?,
        lastId: UUID?,
        random: Random,
    ): List<Item> {
        val result = items.shuffled(random).toMutableList()
        moveTo(result, firstId, 0)
        // 先頭は直前で確定済み。firstId と lastId は同じ項目になり得ないので、末尾との入れ替えが先頭を巻き込むことはない
        moveTo(result, lastId, result.lastIndex)
        return result
    }

    private fun moveTo(
        items: MutableList<Item>,
        id: UUID?,
        index: Int,
    ) {
        if (id == null) return

        val found = items.indexOfFirst { it.id == id }
        if (found < 0 || found == index) return

        items[found] = items[index].also { items[index] = items[found] }
    }
}
