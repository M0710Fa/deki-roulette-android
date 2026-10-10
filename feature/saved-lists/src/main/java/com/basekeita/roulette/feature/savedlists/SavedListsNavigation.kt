package com.basekeita.roulette.feature.savedlists

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.basekeita.roulette.navigation.SavedListsRoute

/**
 * 保存したリストの管理を登録する。
 *
 * @param metadata 表示の仕方（ボトムシートとして出すなど）。app が決めて渡す。
 */
fun EntryProviderScope<NavKey>.savedListsEntry(metadata: Map<String, Any>) {
    entry<SavedListsRoute>(metadata = metadata) {
        SavedListsScreen()
    }
}
