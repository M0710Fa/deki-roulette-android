package com.basekeita.roulette.feature.roulette

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.roulette.feature.roulette.RouletteViewModel.UiState
import com.basekeita.roulette.feature.roulette.RouletteViewModel.ViewState
import com.basekeita.roulette.feature.roulette.ui.molecules.ResultBandLabel
import com.basekeita.roulette.feature.roulette.ui.organisms.RouletteWheel
import com.basekeita.roulette.feature.roulette.viewdata.GlowStyleUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinAnimationUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinEasingUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinOutcomeUiModel
import com.basekeita.roulette.feature.roulette.viewdata.WheelSliceUiModel
import com.basekeita.roulette.ui.component.atom.PrimaryActionButton
import com.basekeita.roulette.ui.component.molecules.ResultActions
import com.basekeita.roulette.ui.component.organisms.ItemList
import com.basekeita.roulette.ui.component.template.AdaptiveStack
import com.basekeita.roulette.ui.component.template.FooterText
import com.basekeita.roulette.ui.component.template.HelpHeading
import com.basekeita.roulette.ui.component.template.PageFrame
import com.basekeita.roulette.ui.component.template.isExpandedWidth
import com.basekeita.roulette.ui.modifier.revealOnAppear
import com.basekeita.roulette.ui.rule.ResultBand
import com.basekeita.roulette.ui.rule.ResultText
import com.basekeita.roulette.ui.theme.DekiLayout
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import com.basekeita.roulette.ui.theme.sliceColor
import com.basekeita.roulette.ui.viewdata.ItemListLimits
import com.basekeita.roulette.ui.viewdata.ItemUiModel
import java.util.UUID

/** 結果の下のコピー・共有の領域の高さ（押せる範囲の 48dp）。結果の有無で下の要素が動かないよう、結果が無いときも確保する。 */
private val resultActionsHeight = DekiLayout.minTouchTarget

/** 盤面・ボタン・結果の領域の間隔。 */
private val wheelSectionSpacing = 24.dp

/** ルーレット画面の Route。ViewModel を持つのはこの関数だけで、子には意味のあるコールバックを個別に渡す。 */
@Composable
internal fun RouletteScreen(
    onNavigationEvent: (RouletteNavigationEvent) -> Unit,
    viewModel: RouletteViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    // 画面から離れたら、鳴らしている途中の回転音を止める
    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenHidden() }
    }

    when (val state = viewState) {
        ViewState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(DekiTheme.colorScheme.ink900))
        }

        is ViewState.Error -> {
            RouletteError(message = state.message)
        }

        is ViewState.Success -> {
            RouletteContent(
                uiState = state.uiState,
                onSettingsClick = { onNavigationEvent(RouletteNavigationEvent.OpenSettings) },
                onSpinClick = viewModel::onSpinClick,
                onSpinFinished = viewModel::onSpinFinished,
                onWheelBoundaryCross = viewModel::onWheelBoundaryCrossed,
                onWheelRelease = viewModel::onWheelRelease,
                onCopyResult = viewModel::onCopyResult,
                onShareResult = viewModel::onShareResult,
                onAddItems = viewModel::onAddItems,
                onRemoveItem = viewModel::onRemoveItem,
                onUndoRemove = viewModel::onUndoRemove,
                onRemoveAllItems = viewModel::onRemoveAllItems,
                onItemLongPress = viewModel::onItemLongPress,
                onSaveList = viewModel::onSaveList,
                onLoadList = viewModel::onLoadList,
                onManageListsClick = { onNavigationEvent(RouletteNavigationEvent.OpenSavedLists) },
            )
        }
    }
}

@Composable
private fun RouletteContent(
    uiState: UiState,
    onSettingsClick: () -> Unit,
    onSpinClick: () -> Unit,
    onSpinFinished: () -> Unit,
    onWheelBoundaryCross: (Double) -> Unit,
    onWheelRelease: (Double?, Double) -> Unit,
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
        title = stringResource(R.string.roulette_title),
        tagline = stringResource(R.string.roulette_tagline),
        busy = uiState.spinning,
        useCases = stringResource(R.string.roulette_use_cases),
        snackbarHostState = snackbarHostState,
        onSettingsClick = onSettingsClick,
        help = {
            FooterText(text = stringResource(R.string.roulette_help_basic))
            HelpHeading(text = stringResource(R.string.roulette_help_aim_title))
            FooterText(text = stringResource(R.string.roulette_help_aim))
            FooterText(text = stringResource(R.string.roulette_help_aim_stealth))
            FooterText(text = stringResource(R.string.roulette_help_aim_random))
        },
    ) {
        val horizontal = isExpandedWidth()
        AdaptiveStack(
            horizontal = horizontal,
            spacing = DekiLayout.columnSpacing,
            sideMaxWidth = DekiLayout.listWidthExpanded,
            main = { sectionModifier ->
                WheelSection(
                    uiState = uiState,
                    // 横並びでは盤面を基準より大きく描く
                    wheelMaxWidth = if (horizontal) {
                        DekiLayout.wheelMaxWidthExpanded
                    } else {
                        DekiLayout.wheelMaxWidthCompact
                    },
                    onSpinClick = onSpinClick,
                    onSpinFinished = onSpinFinished,
                    onWheelBoundaryCross = onWheelBoundaryCross,
                    onWheelRelease = onWheelRelease,
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
                    busy = uiState.spinning,
                    concealMarks = uiState.spinning,
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

/** 盤面（結果の帯を重ねる）、スピンボタン、結果のコピー・共有。 */
@Composable
private fun WheelSection(
    uiState: UiState,
    wheelMaxWidth: Dp,
    onSpinClick: () -> Unit,
    onSpinFinished: () -> Unit,
    onWheelBoundaryCross: (Double) -> Unit,
    onWheelRelease: (Double?, Double) -> Unit,
    onCopyResult: (String) -> Unit,
    onShareResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(wheelSectionSpacing),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = wheelMaxWidth)
                .fillMaxWidth(),
        ) {
            RouletteWheel(
                slices = uiState.slices,
                spin = uiState.spin,
                spinning = uiState.spinning,
                interactive = !uiState.spinning,
                // 結果が出ている間だけ止まったスライスを強調する。項目を触って結果が消えれば強調も解ける
                highlightedIndex = uiState.outcome?.index,
                glowStyle = uiState.glowStyle,
                reducedMotion = uiState.reducedMotion,
                onSpinFinished = onSpinFinished,
                onBoundaryCross = onWheelBoundaryCross,
                onRelease = onWheelRelease,
                modifier = Modifier.fillMaxWidth(),
            )
            val outcome = uiState.outcome
            if (outcome != null) {
                // 出し直しはスピンの回数で決め、同じ結果が続いても現れ直す
                key(uiState.spin.spinCount) {
                    WheelResultBand(
                        outcome = outcome,
                        sliceCount = uiState.slices.size,
                        reducedMotion = uiState.reducedMotion,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }

        PrimaryActionButton(
            title = if (uiState.spinning) {
                stringResource(R.string.roulette_spinning)
            } else {
                stringResource(R.string.roulette_spin)
            },
            enabled = uiState.canSpin,
            onClick = onSpinClick,
        )

        Box(
            modifier = Modifier.height(resultActionsHeight),
            contentAlignment = Alignment.Center,
        ) {
            // 結果が出ている間だけ出す。順番決めと同じ位置・同じ見た目。結果が変わると作り直し、「コピーしました」も戻す
            val outcome = uiState.outcome
            if (outcome != null) {
                key(uiState.spin.spinCount) {
                    ResultActions(
                        copyText = ResultText.roulette(label = outcome.label, heading = stringResource(R.string.roulette_result_heading)),
                        onCopy = onCopyResult,
                        onShare = onShareResult,
                    )
                }
            }
        }
    }
}

/**
 * 盤面の中心より下に重ねる結果の帯。盤面のラベルは省略されるので、結果のラベルはここで全文を出す。
 * 盤面と同じく装飾扱いで、帯そのものは読み上げない。結果は、帯が現れたときに `paneTitle`（「結果は〇〇です」）で伝える
 * （`announceForAccessibility` は API 36 で非推奨のため使わない）。スピンのたびに作り直すので、同じ結果が続いても伝え直す。
 */
@Composable
private fun WheelResultBand(
    outcome: SpinOutcomeUiModel,
    sliceCount: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val announcement = stringResource(R.string.roulette_result_announce, outcome.label)
    BoxWithConstraints(
        modifier = modifier.clearAndSetSemantics { paneTitle = announcement },
        contentAlignment = Alignment.Center,
    ) {
        // 盤面は正方形なので、枠の中心が盤面の中心
        val diameter = minOf(maxWidth, maxHeight).value
        ResultBandLabel(
            label = outcome.label,
            // 項目を変えると結果は消えるので、件数はスピンを始めたときと同じ
            accent = sliceColor(outcome.index, sliceCount),
            diameter = diameter,
            modifier = Modifier
                .offset(y = ResultBand.centerOffset(diameter.toDouble()).toFloat().dp)
                .revealOnAppear(reducedMotion = reducedMotion),
        )
    }
}

@Composable
private fun RouletteError(message: String) {
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

@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun RouletteContentPreview() {
    val labels = listOf("ラーメン", "カレー", "寿司", "焼肉")
    val ids = labels.map { UUID.randomUUID() }
    DekiTheme {
        RouletteContent(
            uiState = UiState(
                items = labels.zip(ids) { label, id -> ItemUiModel(id = id, label = label, isDuplicate = false) },
                slices = labels.zip(ids) { label, id -> WheelSliceUiModel(id = id, label = label) },
                marks = emptyMap(),
                savedLists = emptyList(),
                limits = ItemListLimits(minItemCount = 2, maxItemCount = 24, maxLabelLength = 20, maxSavedListCount = 20),
                spinning = false,
                canSpin = true,
                spin = SpinAnimationUiModel(
                    spinCount = 1,
                    targetRotation = 210f,
                    easing = SpinEasingUiModel(0.15f, 0.85f, 0.3f, 1f),
                    durationMillis = 4_500,
                ),
                outcome = SpinOutcomeUiModel(index = 1, label = "カレー"),
                glowStyle = GlowStyleUiModel.WHITE,
                reducedMotion = false,
                screenCaptured = false,
            ),
            onSettingsClick = {},
            onSpinClick = {},
            onSpinFinished = {},
            onWheelBoundaryCross = {},
            onWheelRelease = { _, _ -> },
            onCopyResult = {},
            onShareResult = {},
            onAddItems = {},
            onRemoveItem = {},
            onUndoRemove = {},
            onRemoveAllItems = {},
            onItemLongPress = {},
            onSaveList = {},
            onLoadList = {},
            onManageListsClick = {},
        )
    }
}
