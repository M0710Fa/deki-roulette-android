package com.basekeita.roulette.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.UserSettings
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.feature.settings.viewdata.GlowStyleUiModel
import com.basekeita.roulette.feature.settings.viewdata.ResetTargetUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 設定画面とルーレットの詳細設定。値は保存と読み込みだけなので Repository を直接呼ぶ。
 *
 * 項目を初期状態に戻すと、ルーレット・順番決めの画面は Repository から届く項目の変化で結果と指定を消す。
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val itemsRepository: ItemsRepository,
) : ViewModel() {
    sealed interface ViewState {
        data object Loading : ViewState

        data class Error(
            val message: String,
        ) : ViewState

        data class Success(
            val uiState: UiState,
        ) : ViewState
    }

    data class UiState(
        val hapticsEnabled: Boolean,
        val soundEnabled: Boolean,
        val glowStyle: GlowStyleUiModel,
    )

    val viewState: StateFlow<ViewState> =
        userSettingsRepository.settings
            .map { settings -> ViewState.Success(settings.toUiState()) as ViewState }
            .catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    fun onHapticsEnabledChange(enabled: Boolean) {
        viewModelScope.launch { userSettingsRepository.setHapticsEnabled(enabled) }
    }

    fun onSoundEnabledChange(enabled: Boolean) {
        viewModelScope.launch { userSettingsRepository.setSoundEnabled(enabled) }
    }

    fun onGlowStyleSelect(style: GlowStyleUiModel) {
        viewModelScope.launch { userSettingsRepository.setGlowStyle(style.toModel()) }
    }

    /** 項目を初期状態に戻す。保存データも消すので、以降は言語設定に応じた初期項目に追従する。 */
    fun onResetItems(target: ResetTargetUiModel) {
        viewModelScope.launch { itemsRepository.reset(target.toListKind()) }
    }
}

private fun UserSettings.toUiState(): SettingsViewModel.UiState = SettingsViewModel.UiState(
    hapticsEnabled = hapticsEnabled,
    soundEnabled = soundEnabled,
    glowStyle = glowStyle.toUiModel(),
)

private fun GlowStyle.toUiModel(): GlowStyleUiModel = when (this) {
    GlowStyle.WHITE -> GlowStyleUiModel.WHITE
    GlowStyle.SLICE -> GlowStyleUiModel.SLICE
    GlowStyle.RAINBOW -> GlowStyleUiModel.RAINBOW
}

private fun GlowStyleUiModel.toModel(): GlowStyle = when (this) {
    GlowStyleUiModel.WHITE -> GlowStyle.WHITE
    GlowStyleUiModel.SLICE -> GlowStyle.SLICE
    GlowStyleUiModel.RAINBOW -> GlowStyle.RAINBOW
}

private fun ResetTargetUiModel.toListKind(): ListKind = when (this) {
    ResetTargetUiModel.ROULETTE -> ListKind.ROULETTE
    ResetTargetUiModel.ORDER -> ListKind.ORDER
}
