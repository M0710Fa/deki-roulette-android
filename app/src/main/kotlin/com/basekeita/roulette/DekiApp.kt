package com.basekeita.roulette

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.basekeita.roulette.feature.order.OrderNavigationEvent
import com.basekeita.roulette.feature.order.orderEntry
import com.basekeita.roulette.feature.roulette.RouletteNavigationEvent
import com.basekeita.roulette.feature.roulette.rouletteEntry
import com.basekeita.roulette.feature.savedlists.savedListsEntry
import com.basekeita.roulette.feature.settings.settingsEntry
import com.basekeita.roulette.navigation.OrderRoute
import com.basekeita.roulette.navigation.RouletteRoute
import com.basekeita.roulette.navigation.SavedListsRoute
import com.basekeita.roulette.navigation.SettingsRoute

/** タブ。項目リストや指定はタブ間で共有しない。 */
private enum class DekiTab(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    ROULETTE(R.string.tab_roulette, R.drawable.ic_tab_roulette),
    ORDER(R.string.tab_order, R.drawable.ic_tab_order),
}

/**
 * ルーレットと順番決めの 2 画面をタブで切り替える。
 *
 * タブは `NavigationSuiteScaffold` で、ウィンドウの大きさに応じて M3 の基準でナビゲーションバーとレールを切り替える。
 * タブごとにバックスタックを持ち、切り替えても各タブの画面の状態（ViewModel を含む）を残す。
 * 設定と保存したリストの管理は、開いたタブのバックスタックに積み、ボトムシートとして出す。
 */
@Composable
fun DekiApp() {
    var currentTab by rememberSaveable { mutableStateOf(DekiTab.ROULETTE) }
    val rouletteBackStack = rememberNavBackStack(RouletteRoute)
    val orderBackStack = rememberNavBackStack(OrderRoute)

    val currentBackStack = if (currentTab == DekiTab.ROULETTE) {
        rouletteBackStack
    } else {
        orderBackStack
    }

    // 両タブとも毎回組み立て、選んでいない側のエントリの状態も破棄しない
    val rouletteEntries = rememberTabEntries(rouletteBackStack)
    val orderEntries = rememberTabEntries(orderBackStack)

    val currentEntries = if (currentTab == DekiTab.ROULETTE) {
        rouletteEntries
    } else {
        orderEntries
    }

    NavigationSuiteScaffold(
        navigationItems = {
            DekiTab.entries.forEach { tab ->
                NavigationSuiteItem(
                    selected = tab == currentTab,
                    onClick = { currentTab = tab },
                    icon = { Icon(painter = painterResource(tab.iconRes), contentDescription = null) },
                    label = { Text(text = stringResource(tab.labelRes)) },
                )
            }
        },
    ) {
        NavDisplay(
            entries = currentEntries,
            // ナビゲーションの分は NavigationSuiteScaffold が除くので、残りのシステムバーの分だけ空ける
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            sceneStrategies = listOf(BottomSheetSceneStrategy()),
            onBack = { currentBackStack.removeLastOrNull() },
        )
    }
}

/** 1 つのタブのエントリ。画面の保存状態と ViewModel をエントリごとに持たせる。 */
@Composable
private fun rememberTabEntries(backStack: NavBackStack<NavKey>): List<NavEntry<NavKey>> {
    val decorators = listOf<NavEntryDecorator<NavKey>>(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )
    return rememberDecoratedNavEntries(backStack, decorators, tabEntryProvider(backStack))
}

/**
 * 1 つのタブで出す画面。各画面の遷移の要求は、そのタブのバックスタックに積む。
 * 設定と保存したリストの管理はボトムシートとして出す。
 */
private fun tabEntryProvider(backStack: NavBackStack<NavKey>): (NavKey) -> NavEntry<NavKey> {
    val bottomSheet = BottomSheetSceneStrategy.bottomSheet()
    return entryProvider {
        rouletteEntry { event ->
            when (event) {
                RouletteNavigationEvent.OpenSettings -> backStack.add(SettingsRoute)
                RouletteNavigationEvent.OpenSavedLists -> backStack.add(SavedListsRoute)
            }
        }
        orderEntry { event ->
            when (event) {
                OrderNavigationEvent.OpenSettings -> backStack.add(SettingsRoute)
                OrderNavigationEvent.OpenSavedLists -> backStack.add(SavedListsRoute)
            }
        }
        settingsEntry(metadata = bottomSheet)
        savedListsEntry(metadata = bottomSheet)
    }
}
