package com.basekeita.roulette.local

/** 項目リストの保存キー。ルーレットと順番決めは別のリストを持つ。キー名は iOS と同じ。 */
enum class StoredItemsKey(
    internal val preferenceName: String,
) {
    ROULETTE("items.roulette"),
    ORDER("items.order"),
}
