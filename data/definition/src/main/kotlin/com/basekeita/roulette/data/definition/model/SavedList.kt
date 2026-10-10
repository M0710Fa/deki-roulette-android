package com.basekeita.roulette.data.definition.model

import java.util.UUID

/**
 * 名前を付けて保存した項目リスト。ルーレットと順番決めで共通に使う。
 * 項目の id と label だけを持ち、当たり・先頭・末尾の指定は含めない。
 */
data class SavedList(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val items: List<Item>,
) {
    companion object {
        /** 保存できるリストの数。 */
        const val MAX_COUNT = 20

        /** リストの名前の最大文字数（正規化後）。 */
        const val MAX_NAME_LENGTH = 30

        /** 項目のラベルと同じ規則で空白を潰し、リスト名の上限で切る。 */
        fun normalizeName(raw: String): String = raw.collapseWhitespace().takeCharacters(MAX_NAME_LENGTH)
    }
}
