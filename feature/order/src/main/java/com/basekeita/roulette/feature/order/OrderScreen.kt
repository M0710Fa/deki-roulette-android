package com.basekeita.roulette.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.roulette.feature.order.OrderViewModel.UiState
import com.basekeita.roulette.feature.order.OrderViewModel.ViewState
import com.basekeita.roulette.feature.order.ui.organisms.OrderResultList
import com.basekeita.roulette.ui.component.atom.PrimaryActionButton
import com.basekeita.roulette.ui.component.molecules.ResultActions
import com.basekeita.roulette.ui.component.organisms.ItemList
import com.basekeita.roulette.ui.component.template.AdaptiveStack
import com.basekeita.roulette.ui.component.template.FooterText
import com.basekeita.roulette.ui.component.template.HelpHeading
import com.basekeita.roulette.ui.component.template.PageFrame
import com.basekeita.roulette.ui.component.template.isExpandedWidth
import com.basekeita.roulette.ui.rule.ResultText
import com.basekeita.roulette.ui.rule.TypeLayout
import com.basekeita.roulette.ui.theme.DekiLayout
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import java.util.UUID

/** 結果の下のコピー・共有の領域の高さ（押せる範囲の 48dp）。結果の有無で上のボタンが動かないよう、結果が無いときも確保する。 */
private val resultActionsHeight = DekiLayout.minTouchTarget

/** 結果・ボタン・結果の下の領域の間隔。 */
private val resultSectionSpacing = 24.dp

/** 順番決め画面の Route。ViewModel を持つのはこの関数だけで、子には意味のあるコールバックを個別に渡す。 */
@Composable
internal fun OrderScreen(
    onNavigationEvent: (OrderNavigationEvent) -> Unit,
    viewModel: OrderViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    when (val state = viewState) {
        ViewState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(DekiTheme.colorScheme.ink900))
        }

        is ViewState.Error -> {
            OrderError(message = state.message)
        }

        is ViewState.Success -> {
            OrderContent(
                uiState = state.uiState,
                onSettingsClick = { onNavigationEvent(OrderNavigationEvent.OpenSettings) },
                onShuffleClick = viewModel::onShuffleClick,
                onCopyResult = viewModel::onCopyResult,
                onShareResult = viewModel::onShareResult,
                onAddItems = viewModel::onAddItems,
                onRemoveItem = viewModel::onRemoveItem,
                onUndoRemove = viewModel::onUndoRemove,
                onRemoveAllItems = viewModel::onRemoveAllItems,
                onItemLongPress = viewModel::onItemLongPress,
                onSaveList = viewModel::onSaveList,
                onLoadList = viewModel::onLoadList,
                onManageListsClick = { onNavigationEvent(OrderNavigationEvent.OpenSavedLists) },
            )
        }
    }
}

@Composable
private fun OrderContent(
    uiState: UiState,
    onSettingsClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onCopyResult: (String) -> Unit,
    onShareResult: (String) -> Unit,
    onAddItems: (List<String>) -> Unit,
    onRemoveItem: (UUID) -> Unit,
    onUndoRemove: (UUID) -> Unit,
    onRemoveAllItems: () -> Unit,
    onItemLongPress: (UUID) -> Unit,
    onSaveList: (String) -> Unit,
    onLoadList: (UUID) -> Unit,
    onManageListsClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    PageFrame(
        title = stringResource(R.string.order_title),
        tagline = stringResource(R.string.order_tagline),
        busy = uiState.revealing,
        useCases = stringResource(R.string.order_use_cases),
        snackbarHostState = snackbarHostState,
        onSettingsClick = onSettingsClick,
        help = {
            FooterText(text = stringResource(R.string.order_help_basic))
            HelpHeading(text = stringResource(R.string.order_help_aim_title))
            FooterText(text = stringResource(R.string.order_help_aim))
            FooterText(text = stringResource(R.string.order_help_aim_stealth))
            FooterText(text = stringResource(R.string.order_help_aim_random))
        },
    ) {
        AdaptiveStack(
            horizontal = isExpandedWidth(),
            spacing = DekiLayout.columnSpacing,
            sideMaxWidth = DekiLayout.listWidthExpanded,
            main = { sectionModifier ->
                ResultSection(
                    uiState = uiState,
                    onShuffleClick = onShuffleClick,
                    onCopyResult = onCopyResult,
                    onShareResult = onShareResult,
                    modifier = sectionModifier,
                )
            },
            side = { listModifier ->
                ItemList(
                    items = uiState.items,
                    marks = uiState.marks,
                    savedLists = uiState.savedLists,
                    limits = uiState.limits,
                    busy = uiState.revealing,
                    // 結果が出ている間も印を伏せる
                    concealMarks = uiState.revealing || uiState.result != null,
                    screenCaptured = uiState.screenCaptured,
                    snackbarHostState = snackbarHostState,
                    onAdd = onAddItems,
                    onRemove = onRemoveItem,
                    onUndoRemove = onUndoRemove,
                    onRemoveAll = onRemoveAllItems,
                    onLongPress = onItemLongPress,
                    onSaveList = onSaveList,
                    onLoadList = onLoadList,
                    onManageListsClick = onManageListsClick,
                    modifier = listModifier,
                )
            },
        )
    }
}

/** 並べた結果、シャッフルボタン、結果のコピー・共有。 */
@Composable
private fun ResultSection(
    uiState: UiState,
    onShuffleClick: () -> Unit,
    onCopyResult: (String) -> Unit,
    onShareResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // コピーボタンの有無で上のボタンが動かないよう高さを固定する。大きい文字では最小高さだけ残す
    val resultActionsModifier = if (TypeLayout.growsFixedAreas(LocalDensity.current.fontScale)) {
        Modifier.heightIn(min = resultActionsHeight)
    } else {
        Modifier.height(resultActionsHeight)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(resultSectionSpacing),
    ) {
        OrderResultList(result = uiState.result, revealing = uiState.revealing, reducedMotion = uiState.reducedMotion)
        PrimaryActionButton(
            title = if (uiState.revealing) {
                stringResource(R.string.order_shuffling)
            } else {
                stringResource(R.string.order_shuffle)
            },
            enabled = uiState.canShuffle,
            onClick = onShuffleClick,
        )
        Box(
            modifier = resultActionsModifier,
            contentAlignment = Alignment.Center,
        ) {
            // 演出が終わって結果が出ている間だけ出す。結果が変わると作り直し、「コピーしました」も戻す
            val result = uiState.result
            if (result != null && !uiState.revealing) {
                key(result.shuffleCount) {
                    ResultActions(
                        copyText = ResultText.order(result.rows.map { row -> row.label }),
                        onCopy = onCopyResult,
                        onShare = onShareResult,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DekiTheme.colorScheme.ink900)
            .padding(DekiLayout.pageHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = DekiTypography.subheadline,
            color = DekiTheme.colorScheme.muted,
        )
    }
}
