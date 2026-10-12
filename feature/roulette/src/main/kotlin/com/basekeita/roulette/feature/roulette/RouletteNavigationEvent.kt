package com.basekeita.roulette.feature.roulette

/** ルーレット画面から app に返す遷移の要求。 */
sealed interface RouletteNavigationEvent {
    /** ヘッダの歯車から設定を開く。 */
    data object OpenSettings : RouletteNavigationEvent

    /** 項目リストのメニューから保存したリストの管理を開く。 */
    data object OpenSavedLists : RouletteNavigationEvent
}
