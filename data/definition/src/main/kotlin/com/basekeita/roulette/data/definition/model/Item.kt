package com.basekeita.roulette.data.definition.model

import java.util.UUID

/**
 * 抽選の項目。保存するのは id と label だけで、当たり・先頭・末尾の指定は持たない（SPEC §12）。
 */
data class Item(
    val id: UUID = UUID.randomUUID(),
    val label: String,
) {
    companion object {
        /** 抽選を始められる最少の件数。 */
        const val MIN_COUNT = 2

        /** 1 つのリストに置ける件数の上限。 */
        const val MAX_COUNT = 24

        /** ラベルの最大文字数（正規化後）。 */
        const val MAX_LABEL_LENGTH = 20

        /** 連続する空白を 1 つに潰し、前後を落として上限の文字数で切る。Web 版 `normalizeLabel` と同じ規則。 */
        fun normalizeLabel(raw: String): String = raw.collapseWhitespace().takeCharacters(MAX_LABEL_LENGTH)
    }
}
