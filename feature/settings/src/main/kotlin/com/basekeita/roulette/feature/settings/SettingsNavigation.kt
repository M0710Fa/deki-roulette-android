package com.basekeita.roulette.feature.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.roulette.navigation.SettingsRoute

/**
 * 設定を登録する。ルーレットの詳細設定は設定の中のページなので、ここでは登録しない。
 *
 * @param metadata 表示の仕方（ボトムシートとして出すなど）。app が決めて渡す。
 */
fun EntryProviderScope<NavKey>.settingsEntry(metadata: Map<String, Any>) {
    entry<SettingsRoute>(metadata = metadata) {
        SettingsScreen()
    }
}
