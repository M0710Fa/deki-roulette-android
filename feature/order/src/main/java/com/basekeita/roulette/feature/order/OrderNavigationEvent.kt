package com.basekeita.roulette.feature.order

/** 順番決め画面から app に返す遷移の要求。 */
sealed interface OrderNavigationEvent {
    /** ヘッダの歯車から設定を開く。 */
    data object OpenSettings : OrderNavigationEvent

    /** 項目リストのメニューから保存したリストの管理を開く。 */
    data object OpenSavedLists : OrderNavigationEvent
}
