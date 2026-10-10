package com.basekeita.roulette.ui.rule

import java.util.UUID

/**
 * 同名の項目の判定。「同名」は `ItemLabelInput.normalize` を通したラベルの完全一致で、
 * 大文字小文字・全角半角の違いは別物として扱う。警告に使うだけで、追加や並びは変えない。
 */
object DuplicateLabels {
    /**
     * 正規化後のラベルが他の項目と一致する項目の id。同名の組に属する項目をすべて含む。
     *
     * @param labels 項目の id とラベルの組
     */
    fun ids(
        labels: List<Pair<UUID, String>>,
        maxLength: Int,
    ): Set<UUID> = labels
        .groupBy { (_, label) -> ItemLabelInput.normalize(label, maxLength) }
        .values
        .filter { group -> group.size > 1 }
        .flatMap { group -> group.map { (id, _) -> id } }
        .toSet()

    /**
     * 追加しようとしているラベルが、既存の項目か入力内の他の行と同名か。
     * ラベルは比較の前に正規化し、空になるものは数えない。
     */
    fun conflicts(
        adding: List<String>,
        existingLabels: List<String>,
        maxLength: Int,
    ): Boolean {
        val seen = existingLabels.map { label -> ItemLabelInput.normalize(label, maxLength) }.toMutableSet()
        adding
            .map { label -> ItemLabelInput.normalize(label, maxLength) }
            .filter { label -> label.isNotEmpty() }
            .forEach { label -> if (!seen.add(label)) return true }
        return false
    }
}
