package com.basekeita.roulette.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basekeita.roulette.feature.settings.SettingsViewModel.UiState
import com.basekeita.roulette.feature.settings.SettingsViewModel.ViewState
import com.basekeita.roulette.feature.settings.ui.molecules.SettingsNavigationItem
import com.basekeita.roulette.feature.settings.ui.molecules.SettingsRadioItem
import com.basekeita.roulette.feature.settings.ui.molecules.SettingsResetItem
import com.basekeita.roulette.feature.settings.ui.molecules.SettingsSectionHeader
import com.basekeita.roulette.feature.settings.ui.molecules.SettingsSwitchItem
import com.basekeita.roulette.feature.settings.ui.molecules.SettingsTextItem
import com.basekeita.roulette.feature.settings.viewdata.GlowStyleUiModel
import com.basekeita.roulette.feature.settings.viewdata.ResetTargetUiModel
import com.basekeita.roulette.ui.component.template.SheetFrame
import com.basekeita.roulette.ui.theme.DekiTheme
import com.basekeita.roulette.ui.theme.DekiTypography
import com.basekeita.roulette.ui.R as UiR

/** 設定のシートの中のページ。詳細設定はシートを重ねず、同じシートの中で切り替える。 */
private enum class SettingsPage {
    MAIN,
    ROULETTE_ADVANCED,
}

/**
 * 設定の Route。ヘッダの歯車からボトムシートで開く。触覚フィードバックと効果音の切替、ルーレットの詳細設定、項目の初期化、著作権を置く。
 * ルーレットの詳細設定（光彩の色）は同じシートの中のページで、戻るボタンか戻る操作で設定に戻る。
 */
@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    val backToMain = { page = SettingsPage.MAIN }

    // 詳細設定にいるときの戻る操作は、シートを閉じずに設定に戻す
    BackHandler(enabled = page == SettingsPage.ROULETTE_ADVANCED, onBack = backToMain)

    AnimatedContent(targetState = page, label = "SettingsPage") { shownPage ->
        when (shownPage) {
            SettingsPage.MAIN -> {
                SheetFrame(title = stringResource(R.string.settings_title)) {
                    SettingsStateContent(viewState = viewState) { uiState ->
                        SettingsContent(
                            uiState = uiState,
                            onHapticsEnabledChange = viewModel::onHapticsEnabledChange,
                            onSoundEnabledChange = viewModel::onSoundEnabledChange,
                            onRouletteAdvancedClick = { page = SettingsPage.ROULETTE_ADVANCED },
                            onResetItems = viewModel::onResetItems,
                        )
                    }
                }
            }

            SettingsPage.ROULETTE_ADVANCED -> {
                SheetFrame(
                    title = stringResource(R.string.settings_roulette_advanced_title),
                    onBack = backToMain,
                ) {
                    SettingsStateContent(viewState = viewState) { uiState ->
                        RouletteAdvancedSettingsContent(
                            glowStyle = uiState.glowStyle,
                            onGlowStyleSelect = viewModel::onGlowStyleSelect,
                        )
                    }
                }
            }
        }
    }
}

/** 読み込み中は何も出さず、読めなければその旨だけを出す。 */
@Composable
private fun SettingsStateContent(
    viewState: ViewState,
    content: @Composable (UiState) -> Unit,
) {
    when (viewState) {
        ViewState.Loading -> Unit

        is ViewState.Error -> {
            Text(
                text = viewState.message,
                style = DekiTypography.subheadline,
                color = DekiTheme.colorScheme.muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }

        is ViewState.Success -> {
            content(viewState.uiState)
        }
    }
}

/** 設定の行を縦に並べる。中身が収まらないときはスクロールする。 */
@Composable
private fun SettingsColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        content = content,
    )
}

@Composable
private fun SettingsContent(
    uiState: UiState,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onRouletteAdvancedClick: () -> Unit,
    onResetItems: (ResetTargetUiModel) -> Unit,
) {
    // 確認ダイアログを出している対象。出していないときは null
    var resetTarget by rememberSaveable { mutableStateOf<ResetTargetUiModel?>(null) }

    SettingsColumn {
        // 文言はこの見出しだけ。何に使うかの説明は置かない（長押しの存在を示唆しないため）
        SettingsSwitchItem(
            title = stringResource(R.string.settings_haptics_title),
            checked = uiState.hapticsEnabled,
            onCheckedChange = onHapticsEnabledChange,
        )

        SettingsSectionHeader(title = stringResource(R.string.settings_sound_title))
        SettingsSwitchItem(
            title = stringResource(R.string.settings_sound_toggle),
            supportingText = stringResource(R.string.settings_sound_note),
            checked = uiState.soundEnabled,
            onCheckedChange = onSoundEnabledChange,
        )

        SettingsNavigationItem(
            title = stringResource(R.string.settings_roulette_advanced_title),
            onClick = onRouletteAdvancedClick,
        )

        SettingsSectionHeader(title = stringResource(R.string.settings_reset_items_title))
        Text(
            text = stringResource(R.string.settings_reset_items_description),
            style = DekiTypography.caption,
            color = DekiTheme.colorScheme.muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        SettingsResetItem(
            title = stringResource(R.string.settings_reset_items_roulette),
            onClick = { resetTarget = ResetTargetUiModel.ROULETTE },
        )
        SettingsResetItem(
            title = stringResource(R.string.settings_reset_items_order),
            onClick = { resetTarget = ResetTargetUiModel.ORDER },
        )

        SettingsSectionHeader(title = stringResource(R.string.settings_copyright_title))
        SettingsTextItem(
            title = stringResource(R.string.settings_copyright_owner),
            supportingText = stringResource(R.string.settings_copyright),
        )
    }

    val confirmingTarget = resetTarget
    if (confirmingTarget != null) {
        ResetItemsConfirmDialog(
            target = confirmingTarget,
            onConfirm = {
                resetTarget = null
                onResetItems(confirmingTarget)
            },
            onDismiss = { resetTarget = null },
        )
    }
}

@Composable
private fun ResetItemsConfirmDialog(
    target: ResetTargetUiModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = DekiTheme.colorScheme
    val screenName = when (target) {
        ResetTargetUiModel.ROULETTE -> stringResource(R.string.settings_screen_roulette)
        ResetTargetUiModel.ORDER -> stringResource(R.string.settings_screen_order)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_reset_items_confirm, screenName)) },
        text = { Text(text = stringResource(R.string.settings_reset_items_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.settings_reset_items_action), color = colorScheme.flareText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(UiR.string.cancel))
            }
        },
    )
}

@Composable
private fun RouletteAdvancedSettingsContent(
    glowStyle: GlowStyleUiModel,
    onGlowStyleSelect: (GlowStyleUiModel) -> Unit,
) {
    SettingsColumn {
        SettingsSectionHeader(title = stringResource(R.string.settings_glow_style_title))
        GlowStyleUiModel.entries.forEach { style ->
            SettingsRadioItem(
                title = glowStyleName(style),
                selected = style == glowStyle,
                onClick = { onGlowStyleSelect(style) },
            )
        }
    }
}

@Composable
private fun glowStyleName(style: GlowStyleUiModel): String = when (style) {
    GlowStyleUiModel.WHITE -> stringResource(R.string.settings_glow_style_white)
    GlowStyleUiModel.SLICE -> stringResource(R.string.settings_glow_style_slice)
    GlowStyleUiModel.RAINBOW -> stringResource(R.string.settings_glow_style_rainbow)
}

@Preview(widthDp = 360)
@Composable
private fun SettingsContentPreview() {
    DekiTheme {
        SheetFrame(title = "設定") {
            SettingsContent(
                uiState = UiState(hapticsEnabled = true, soundEnabled = false, glowStyle = GlowStyleUiModel.WHITE),
                onHapticsEnabledChange = {},
                onSoundEnabledChange = {},
                onRouletteAdvancedClick = {},
                onResetItems = {},
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun RouletteAdvancedSettingsContentPreview() {
    DekiTheme {
        SheetFrame(title = "ルーレットの詳細設定", onBack = {}) {
            RouletteAdvancedSettingsContent(glowStyle = GlowStyleUiModel.RAINBOW, onGlowStyleSelect = {})
        }
    }
}
