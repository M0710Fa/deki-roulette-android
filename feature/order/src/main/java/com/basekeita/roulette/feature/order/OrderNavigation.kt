package com.basekeita.roulette.feature.order

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.roulette.navigation.OrderRoute

/** 順番決めのタブの最初の画面を登録する。 */
fun EntryProviderScope<NavKey>.orderEntry(onNavigationEvent: (OrderNavigationEvent) -> Unit) {
    entry<OrderRoute> {
        OrderScreen(onNavigationEvent = onNavigationEvent)
    }
}
