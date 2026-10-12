package com.basekeita.roulette.feature.roulette

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.roulette.navigation.RouletteRoute

/** ルーレットのタブの最初の画面を登録する。 */
fun EntryProviderScope<NavKey>.rouletteEntry(onNavigationEvent: (RouletteNavigationEvent) -> Unit) {
    entry<RouletteRoute> {
        RouletteScreen(onNavigationEvent = onNavigationEvent)
    }
}
