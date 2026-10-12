package com.basekeita.roulette.feature.settings

import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.UserSettings
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.feature.settings.SettingsViewModel.UiState
import com.basekeita.roulette.feature.settings.SettingsViewModel.ViewState
import com.basekeita.roulette.feature.settings.uimodel.GlowStyleUiModel
import com.basekeita.roulette.feature.settings.uimodel.ResetTargetUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** メモリに持つ `UserSettingsRepository`。 */
    private class FakeUserSettingsRepository(
        initial: UserSettings = UserSettings(),
    ) : UserSettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun setHapticsEnabled(enabled: Boolean) {
            settings.value = settings.value.copy(hapticsEnabled = enabled)
        }

        override suspend fun setSoundEnabled(enabled: Boolean) {
            settings.value = settings.value.copy(soundEnabled = enabled)
        }

        override suspend fun setGlowStyle(style: GlowStyle) {
            settings.value = settings.value.copy(glowStyle = style)
        }
    }

    /** `reset` を呼んだ相手だけを記録する `ItemsRepository`。 */
    private class RecordingItemsRepository : ItemsRepository {
        val resetKinds = mutableListOf<ListKind>()

        override fun items(kind: ListKind): Flow<List<Item>> = emptyFlow()

        override suspend fun save(
            kind: ListKind,
            items: List<Item>,
        ) = Unit

        override suspend fun reset(kind: ListKind) {
            resetKinds += kind
        }
    }

    private fun createViewModel(
        settingsRepository: UserSettingsRepository = FakeUserSettingsRepository(),
        itemsRepository: ItemsRepository = RecordingItemsRepository(),
    ): SettingsViewModel = SettingsViewModel(
        userSettingsRepository = settingsRepository,
        itemsRepository = itemsRepository,
    )

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: SettingsViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    @Test
    fun `保存した設定を出す`() = runTest {
        val settingsRepository =
            FakeUserSettingsRepository(UserSettings(hapticsEnabled = false, soundEnabled = true, glowStyle = GlowStyle.SLICE))
        val uiState = observe(createViewModel(settingsRepository = settingsRepository))

        assertEquals(UiState(hapticsEnabled = false, soundEnabled = true, glowStyle = GlowStyleUiModel.SLICE), uiState())
    }

    @Test
    fun `設定を読めなければErrorを出す`() = runTest {
        val failingRepository =
            object : UserSettingsRepository {
                override val settings: Flow<UserSettings> = flow { throw IllegalStateException("読めません") }

                override suspend fun setHapticsEnabled(enabled: Boolean) = Unit

                override suspend fun setSoundEnabled(enabled: Boolean) = Unit

                override suspend fun setGlowStyle(style: GlowStyle) = Unit
            }
        val viewModel = createViewModel(settingsRepository = failingRepository)
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        assertEquals(ViewState.Error("読めません"), viewModel.viewState.value)
    }

    @Test
    fun `触覚と効果音の切替を保存する`() = runTest {
        val settingsRepository = FakeUserSettingsRepository()
        val viewModel = createViewModel(settingsRepository = settingsRepository)
        val uiState = observe(viewModel)

        viewModel.onHapticsEnabledChange(false)
        viewModel.onSoundEnabledChange(false)
        runCurrent()

        assertEquals(false, settingsRepository.settings.value.hapticsEnabled)
        assertEquals(false, settingsRepository.settings.value.soundEnabled)
        assertEquals(false, uiState().hapticsEnabled)
        assertEquals(false, uiState().soundEnabled)
    }

    @Test
    fun `光彩の色を保存する`() = runTest {
        val settingsRepository = FakeUserSettingsRepository()
        val viewModel = createViewModel(settingsRepository = settingsRepository)
        val uiState = observe(viewModel)

        viewModel.onGlowStyleSelect(GlowStyleUiModel.RAINBOW)
        runCurrent()

        assertEquals(GlowStyle.RAINBOW, settingsRepository.settings.value.glowStyle)
        assertEquals(GlowStyleUiModel.RAINBOW, uiState().glowStyle)
    }

    @Test
    fun `選んだ画面の項目だけを初期状態に戻す`() = runTest {
        val itemsRepository = RecordingItemsRepository()
        val viewModel = createViewModel(itemsRepository = itemsRepository)

        viewModel.onResetItems(ResetTargetUiModel.ORDER)
        runCurrent()
        assertEquals(listOf(ListKind.ORDER), itemsRepository.resetKinds)

        viewModel.onResetItems(ResetTargetUiModel.ROULETTE)
        runCurrent()
        assertEquals(listOf(ListKind.ORDER, ListKind.ROULETTE), itemsRepository.resetKinds)
    }
}
