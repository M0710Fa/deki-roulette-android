package com.basekeita.roulette.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** ルーレットのタブの最初の画面。 */
@Serializable
data object RouletteRoute : NavKey

/** 順番決めのタブの最初の画面。 */
@Serializable
data object OrderRoute : NavKey

/** 設定。両タブのヘッダの歯車から開く。 */
@Serializable
data object SettingsRoute : NavKey

/** 保存したリストの管理。項目リストの見出しのメニューから開く。 */
@Serializable
data object SavedListsRoute : NavKey
