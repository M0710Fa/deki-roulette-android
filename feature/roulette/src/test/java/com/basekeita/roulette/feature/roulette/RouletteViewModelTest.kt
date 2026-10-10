package com.basekeita.roulette.feature.roulette

import app.cash.turbine.test
import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.data.definition.model.SpinDirection
import com.basekeita.roulette.data.definition.model.UserSettings
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.domain.item.AddItemsUseCase
import com.basekeita.roulette.domain.item.RemoveItemUseCase
import com.basekeita.roulette.domain.item.RestoreItemUseCase
import com.basekeita.roulette.domain.spin.DecideSpinUseCase
import com.basekeita.roulette.domain.spin.FlickSpin
import com.basekeita.roulette.domain.spin.RouletteMath
import com.basekeita.roulette.domain.spin.SpinConfig
import com.basekeita.roulette.feature.roulette.RouletteViewModel.UiState
import com.basekeita.roulette.feature.roulette.RouletteViewModel.ViewState
import com.basekeita.roulette.feature.roulette.viewdata.GlowStyleUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinEasingUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinOutcomeUiModel
import com.basekeita.roulette.ui.feedback.HapticEffect
import com.basekeita.roulette.ui.feedback.HapticPlayer
import com.basekeita.roulette.ui.feedback.ReduceMotionMonitor
import com.basekeita.roulette.ui.feedback.ResultSharer
import com.basekeita.roulette.ui.feedback.ScreenCaptureMonitor
import com.basekeita.roulette.ui.feedback.SpinSoundPlayer
import com.basekeita.roulette.ui.viewdata.MarkViewData
import com.basekeita.roulette.ui.viewdata.SavedListSummaryUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class RouletteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val defaultItems = listOf("A", "B", "C", "D").map { label -> Item(label = label) }

    /** メモリに持つ `ItemsRepository`。 */
    private class FakeItemsRepository(
        initial: List<Item>,
    ) : ItemsRepository {
        private val stored = ListKind.entries.associateWith { MutableStateFlow(initial) }

        fun current(kind: ListKind): List<Item> = stored.getValue(kind).value

        override fun items(kind: ListKind): Flow<List<Item>> = stored.getValue(kind)

        override suspend fun save(
            kind: ListKind,
            items: List<Item>,
        ) {
            stored.getValue(kind).value = items
        }

        override suspend fun reset(kind: ListKind) = Unit
    }

    /** メモリに持つ `SavedListsRepository`。名前の正規化は省き、上限だけを守る。 */
    private class FakeSavedListsRepository(
        initial: List<SavedList> = emptyList(),
    ) : SavedListsRepository {
        override val lists = MutableStateFlow(initial)

        override suspend fun save(
            name: String,
            items: List<Item>,
        ): Boolean {
            if (name.isBlank() || lists.value.size >= SavedList.MAX_COUNT) return false

            lists.value = lists.value + SavedList(name = name, items = items)
            return true
        }

        override suspend fun rename(
            id: UUID,
            name: String,
        ): Boolean = false

        override suspend fun delete(id: UUID): Boolean = false
    }

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

    /** 鳴らした触覚を順に記録する。 */
    private class FakeHapticPlayer : HapticPlayer {
        val played = mutableListOf<HapticEffect>()

        override fun play(effect: HapticEffect) {
            played += effect
        }
    }

    /** 回転音の再生を記録する。 */
    private class FakeSpinSoundPlayer : SpinSoundPlayer {
        var prepareCount = 0
        val plays = mutableListOf<List<Double>>()
        var clickCount = 0
        var stopCount = 0

        override fun prepare() {
            prepareCount += 1
        }

        override fun play(timesSeconds: List<Double>) {
            plays += timesSeconds
        }

        override fun playClick() {
            clickCount += 1
        }

        override fun stop() {
            stopCount += 1
        }
    }

    private class FakeReduceMotionMonitor(
        initial: Boolean = false,
    ) : ReduceMotionMonitor {
        override val reducedMotion = MutableStateFlow(initial)
    }

    private class FakeScreenCaptureMonitor : ScreenCaptureMonitor {
        override val isCaptured = MutableStateFlow(false)
    }

    private class FakeResultSharer : ResultSharer {
        val copied = mutableListOf<String>()
        val shared = mutableListOf<String>()

        override fun copy(text: String) {
            copied += text
        }

        override fun share(text: String) {
            shared += text
        }
    }

    private val hapticPlayer = FakeHapticPlayer()
    private val soundPlayer = FakeSpinSoundPlayer()
    private val screenCaptureMonitor = FakeScreenCaptureMonitor()
    private val resultSharer = FakeResultSharer()

    private fun createViewModel(
        itemsRepository: ItemsRepository = FakeItemsRepository(defaultItems),
        savedListsRepository: SavedListsRepository = FakeSavedListsRepository(),
        settingsRepository: UserSettingsRepository = FakeUserSettingsRepository(),
        random: Random = Random(1),
        reduceMotionMonitor: ReduceMotionMonitor = FakeReduceMotionMonitor(),
    ): RouletteViewModel = RouletteViewModel(
        itemsRepository = itemsRepository,
        savedListsRepository = savedListsRepository,
        userSettingsRepository = settingsRepository,
        decideSpin = DecideSpinUseCase(random),
        addItems = AddItemsUseCase(itemsRepository),
        removeItem = RemoveItemUseCase(itemsRepository),
        restoreItem = RestoreItemUseCase(itemsRepository),
        hapticPlayer = hapticPlayer,
        spinSoundPlayer = soundPlayer,
        reduceMotionMonitor = reduceMotionMonitor,
        screenCaptureMonitor = screenCaptureMonitor,
        resultSharer = resultSharer,
    )

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: RouletteViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    /** スピンを始め、盤面の回転が止まったことにする。 */
    private fun TestScope.spinToEnd(viewModel: RouletteViewModel) {
        viewModel.onSpinClick()
        runCurrent()
        viewModel.onSpinFinished()
        runCurrent()
    }

    // ── 表示 ─────────────────────────────────────────────────────────────────

    @Test
    fun `読み込むまではLoadingでそのあと保存した項目を出す`() = runTest {
        // 値をまだ流していない保存先。読み込みが終わるまでの状態を確かめる
        val pendingItems = MutableSharedFlow<List<Item>>(replay = 1)
        val pendingRepository =
            object : ItemsRepository {
                override fun items(kind: ListKind): Flow<List<Item>> = pendingItems

                override suspend fun save(
                    kind: ListKind,
                    items: List<Item>,
                ) = Unit

                override suspend fun reset(kind: ListKind) = Unit
            }
        val viewModel = createViewModel(itemsRepository = pendingRepository)

        viewModel.viewState.test {
            assertEquals(ViewState.Loading, awaitItem())

            pendingItems.emit(defaultItems)

            val uiState = (awaitItem() as ViewState.Success).uiState
            assertEquals(listOf("A", "B", "C", "D"), uiState.items.map { item -> item.label })
            assertEquals(uiState.items.map { item -> item.id }, uiState.slices.map { slice -> slice.id })
            assertTrue(uiState.marks.isEmpty())
            assertNull(uiState.outcome)
        }
    }

    @Test
    fun `項目を読めなければErrorを出す`() = runTest {
        val failingRepository =
            object : ItemsRepository {
                override fun items(kind: ListKind): Flow<List<Item>> = flow { throw IllegalStateException("読めません") }

                override suspend fun save(
                    kind: ListKind,
                    items: List<Item>,
                ) = Unit

                override suspend fun reset(kind: ListKind) = Unit
            }
        val viewModel = createViewModel(itemsRepository = failingRepository)
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()

        assertEquals(ViewState.Error("読めません"), viewModel.viewState.value)
    }

    @Test
    fun `光彩の色は設定に従う`() = runTest {
        val settingsRepository = FakeUserSettingsRepository(UserSettings(glowStyle = GlowStyle.RAINBOW))
        val uiState = observe(createViewModel(settingsRepository = settingsRepository))

        assertEquals(GlowStyleUiModel.RAINBOW, uiState().glowStyle)

        settingsRepository.setGlowStyle(GlowStyle.SLICE)
        runCurrent()

        assertEquals(GlowStyleUiModel.SLICE, uiState().glowStyle)
    }

    // ── 指定 ─────────────────────────────────────────────────────────────────

    @Test
    fun `当たり指定はトグルする`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        val id = defaultItems[1].id

        viewModel.onItemLongPress(id)
        runCurrent()
        assertEquals(mapOf(id to MarkViewData.TARGET), uiState().marks)

        viewModel.onItemLongPress(id)
        runCurrent()
        assertTrue(uiState().marks.isEmpty())
    }

    @Test
    fun `別の項目を長押しすると指定が移る`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onItemLongPress(defaultItems[0].id)
        viewModel.onItemLongPress(defaultItems[2].id)
        runCurrent()

        assertEquals(mapOf(defaultItems[2].id to MarkViewData.TARGET), uiState().marks)
    }

    @Test
    fun `指定した項目を削除すると指定も外れる`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        val id = defaultItems[1].id

        viewModel.onItemLongPress(id)
        viewModel.onRemoveItem(id)
        runCurrent()

        assertTrue(uiState().marks.isEmpty())
        assertEquals(listOf("A", "C", "D"), repository.current(ListKind.ROULETTE).map { item -> item.label })
    }

    @Test
    fun `指定した項目が外から消えたら印を出さない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)

        viewModel.onItemLongPress(defaultItems[1].id)
        repository.save(ListKind.ROULETTE, listOf(Item(label = "X"), Item(label = "Y")))
        runCurrent()

        assertTrue(uiState().marks.isEmpty())
    }

    @Test
    fun `すべて削除すると項目も指定も結果も消える`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        viewModel.onItemLongPress(defaultItems[0].id)
        spinToEnd(viewModel)

        viewModel.onRemoveAllItems()
        runCurrent()

        assertTrue(uiState().items.isEmpty())
        assertTrue(uiState().marks.isEmpty())
        assertNull(uiState().outcome)
        assertTrue(repository.current(ListKind.ROULETTE).isEmpty())
    }

    // ── スピン ───────────────────────────────────────────────────────────────

    @Test
    fun `項目が2つ未満なら回せない`() = runTest {
        val viewModel = createViewModel(itemsRepository = FakeItemsRepository(defaultItems.take(1)))
        val uiState = observe(viewModel)

        assertFalse(uiState().canSpin)

        viewModel.onSpinClick()
        runCurrent()

        assertFalse(uiState().spinning)
        assertEquals(0, uiState().spin.spinCount)
    }

    @Test
    fun `指定があれば結果はその項目になる`() = runTest {
        for (seed in 0 until 20) {
            val viewModel = createViewModel(random = Random(seed))
            val uiState = observe(viewModel)

            viewModel.onItemLongPress(defaultItems[2].id)
            spinToEnd(viewModel)

            assertEquals(SpinOutcomeUiModel(index = 2, label = "C"), uiState().outcome)
            assertEquals(2, RouletteMath.indexUnderPointer(uiState().spin.targetRotation.toDouble(), defaultItems.size))
        }
    }

    @Test
    fun `指定がなければ結果は針の下の項目と一致する`() = runTest {
        for (seed in 0 until 20) {
            val viewModel = createViewModel(random = Random(seed))
            val uiState = observe(viewModel)

            spinToEnd(viewModel)

            val outcome = uiState().outcome!!
            assertEquals(outcome.index, RouletteMath.indexUnderPointer(uiState().spin.targetRotation.toDouble(), defaultItems.size))
            assertEquals(defaultItems[outcome.index].label, outcome.label)
        }
    }

    @Test
    fun `同じラベルが並んでいても結果の添字は指定した項目を指す`() = runTest {
        val sameLabels = List(4) { Item(label = "同じ") }
        val viewModel = createViewModel(itemsRepository = FakeItemsRepository(sameLabels))
        val uiState = observe(viewModel)

        viewModel.onItemLongPress(sameLabels[3].id)
        spinToEnd(viewModel)

        assertEquals(3, uiState().outcome?.index)
    }

    @Test
    fun `演出中は結果を出さず二重に始められない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onSpinClick()
        runCurrent()
        val firstTarget = uiState().spin.targetRotation

        assertTrue(uiState().spinning)
        assertFalse(uiState().canSpin)
        assertNull(uiState().outcome)

        viewModel.onSpinClick()
        runCurrent()

        assertEquals(1, uiState().spin.spinCount)
        assertEquals(firstTarget, uiState().spin.targetRotation)
    }

    @Test
    fun `回転が止まると結果が出て次のスピンを始められる`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        spinToEnd(viewModel)

        assertFalse(uiState().spinning)
        assertTrue(uiState().canSpin)
        assertTrue(uiState().outcome != null)
    }

    @Test
    fun `回転の完了が届かなくても保険のタイマーで終わる`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        val fallbackMillis = (SpinConfig.FALLBACK_SECONDS * 1_000).toLong()

        viewModel.onSpinClick()
        runCurrent()
        advanceTimeBy(fallbackMillis - 1)
        runCurrent()
        assertTrue(uiState().spinning)

        advanceTimeBy(2)
        runCurrent()
        assertFalse(uiState().spinning)
        assertTrue(uiState().outcome != null)
    }

    @Test
    fun `回転の完了と保険のタイマーが重なっても結果は変わらない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        spinToEnd(viewModel)
        val outcome = uiState().outcome
        advanceTimeBy((SpinConfig.FALLBACK_SECONDS * 1_000).toLong() * 2)
        viewModel.onSpinFinished()
        runCurrent()

        assertEquals(outcome, uiState().outcome)
        assertEquals(1, uiState().spin.spinCount)
    }

    @Test
    fun `スピンを始めるたびに回数が増え止まる角度は前の角度から進む`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        spinToEnd(viewModel)
        val firstTarget = uiState().spin.targetRotation
        spinToEnd(viewModel)

        assertEquals(2, uiState().spin.spinCount)
        assertTrue(uiState().spin.targetRotation > firstTarget)
    }

    @Test
    fun `ボタンのスピンは基準の曲線と長さで時計回りに回す`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onSpinClick()
        runCurrent()

        val easing = SpinConfig.EASING
        assertEquals(
            SpinEasingUiModel(easing.x1.toFloat(), easing.y1.toFloat(), easing.x2.toFloat(), easing.y2.toFloat()),
            uiState().spin.easing,
        )
        assertEquals(4_500, uiState().spin.durationMillis)
        assertTrue(uiState().spin.targetRotation > 0f)
    }

    @Test
    fun `演出中は印を伏せるため指定は渡したまま結果も出さない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        val id = defaultItems[0].id

        viewModel.onItemLongPress(id)
        viewModel.onSpinClick()
        runCurrent()

        // 伏せるかどうかは項目リストが spinning で決める。指定そのものは保ったまま
        assertTrue(uiState().spinning)
        assertEquals(mapOf(id to MarkViewData.TARGET), uiState().marks)
    }

    // ── 結果を消す操作 ───────────────────────────────────────────────────────

    @Test
    fun `項目を追加すると結果が消え保存される`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        spinToEnd(viewModel)

        viewModel.onAddItems(listOf("  E  "))
        runCurrent()

        assertNull(uiState().outcome)
        assertEquals(listOf("A", "B", "C", "D", "E"), repository.current(ListKind.ROULETTE).map { item -> item.label })
    }

    @Test
    fun `空白だけの入力では項目も結果も変わらない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        spinToEnd(viewModel)
        val outcome = uiState().outcome

        viewModel.onAddItems(listOf("   "))
        runCurrent()

        assertEquals(outcome, uiState().outcome)
        assertEquals(defaultItems, repository.current(ListKind.ROULETTE))
    }

    @Test
    fun `項目を削除すると結果が消える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        spinToEnd(viewModel)

        viewModel.onRemoveItem(defaultItems[0].id)
        runCurrent()

        assertNull(uiState().outcome)
    }

    @Test
    fun `長押しすると結果が消える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        spinToEnd(viewModel)

        viewModel.onItemLongPress(defaultItems[0].id)
        runCurrent()

        assertNull(uiState().outcome)
    }

    @Test
    fun `項目が画面の外で変わると結果を出さない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        spinToEnd(viewModel)

        // 設定の「項目を初期状態に戻す」にあたる
        repository.save(ListKind.ROULETTE, listOf(Item(label = "X"), Item(label = "Y")))
        runCurrent()

        assertNull(uiState().outcome)
    }

    // ── 保存したリスト ───────────────────────────────────────────────────────

    @Test
    fun `保存したリストを件数つきで出す`() = runTest {
        val savedList = SavedList(name = "ランチ", items = defaultItems.take(3))
        val uiState = observe(createViewModel(savedListsRepository = FakeSavedListsRepository(listOf(savedList))))

        assertEquals(listOf(SavedListSummaryUiModel(id = savedList.id, name = "ランチ", itemCount = 3)), uiState().savedLists)
    }

    @Test
    fun `いまの項目を名前を付けて保存する`() = runTest {
        val savedListsRepository = FakeSavedListsRepository()
        val viewModel = createViewModel(savedListsRepository = savedListsRepository)
        observe(viewModel)

        viewModel.onSaveList("ランチ")
        runCurrent()

        val saved = savedListsRepository.lists.value.single()
        assertEquals("ランチ", saved.name)
        assertEquals(defaultItems, saved.items)
    }

    @Test
    fun `読み込むと項目が置き換わり指定と結果が消える`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        // いまの項目をそのまま保存したリスト。id が同じでも指定は消える
        val savedList = SavedList(name = "いまの項目", items = defaultItems)
        val otherList = SavedList(name = "チーム", items = listOf(Item(label = "X"), Item(label = "Y")))
        val viewModel =
            createViewModel(
                itemsRepository = repository,
                savedListsRepository = FakeSavedListsRepository(listOf(savedList, otherList)),
            )
        val uiState = observe(viewModel)
        viewModel.onItemLongPress(defaultItems[0].id)
        spinToEnd(viewModel)

        viewModel.onLoadList(savedList.id)
        runCurrent()

        assertTrue(uiState().marks.isEmpty())
        assertNull(uiState().outcome)

        viewModel.onLoadList(otherList.id)
        runCurrent()

        assertEquals(listOf("X", "Y"), repository.current(ListKind.ROULETTE).map { item -> item.label })
    }

    @Test
    fun `保存したリストが見つからなければ何も変えない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        viewModel.onItemLongPress(defaultItems[0].id)

        viewModel.onLoadList(UUID.randomUUID())
        runCurrent()

        assertEquals(defaultItems, repository.current(ListKind.ROULETTE))
        assertEquals(mapOf(defaultItems[0].id to MarkViewData.TARGET), uiState().marks)
    }

    @Test
    fun `保存した項目から始まるが指定は保存しない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)
        viewModel.onItemLongPress(defaultItems[1].id)
        runCurrent()

        val recreated = createViewModel(itemsRepository = repository)
        val uiState = observe(recreated)

        assertEquals(defaultItems.map { item -> item.id }, uiState().items.map { item -> item.id })
        assertTrue(uiState().marks.isEmpty())
    }

    // ── まとめて追加・同名 ───────────────────────────────────────────────────

    @Test
    fun `複数のラベルをまとめて追加できる`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)

        viewModel.onAddItems(listOf("E", "F", "G"))
        runCurrent()

        assertEquals(listOf("A", "B", "C", "D", "E", "F", "G"), repository.current(ListKind.ROULETTE).map { item -> item.label })
    }

    @Test
    fun `まとめて追加しても上限を超えた分は切り捨てる`() = runTest {
        val nearlyFull = List(Item.MAX_COUNT - 2) { index -> Item(label = "項目$index") }
        val repository = FakeItemsRepository(nearlyFull)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)

        viewModel.onAddItems(listOf("X", "Y", "Z"))
        runCurrent()

        val labels = repository.current(ListKind.ROULETTE).map { item -> item.label }
        assertEquals(Item.MAX_COUNT, labels.size)
        assertEquals(listOf("X", "Y"), labels.takeLast(2))
    }

    @Test
    fun `まとめて追加すると結果が消える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        spinToEnd(viewModel)

        viewModel.onAddItems(listOf("E", "F"))
        runCurrent()

        assertNull(uiState().outcome)
    }

    @Test
    fun `同名の項目には注意を出す`() = runTest {
        val items = listOf(Item(label = "A"), Item(label = "B"), Item(label = "A"))
        val uiState = observe(createViewModel(itemsRepository = FakeItemsRepository(items)))

        assertEquals(listOf(true, false, true), uiState().items.map { item -> item.isDuplicate })
    }

    // ── 元に戻す ─────────────────────────────────────────────────────────────

    @Test
    fun `元に戻すと同じIDで元の位置に入る`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)

        viewModel.onRemoveItem(defaultItems[1].id)
        runCurrent()
        viewModel.onUndoRemove(defaultItems[1].id)
        runCurrent()

        assertEquals(defaultItems, repository.current(ListKind.ROULETTE))
    }

    @Test
    fun `元に戻しても指定は復元しない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        val id = defaultItems[1].id

        viewModel.onItemLongPress(id)
        viewModel.onRemoveItem(id)
        runCurrent()
        viewModel.onUndoRemove(id)
        runCurrent()

        assertTrue(uiState().items.any { item -> item.id == id })
        assertTrue(uiState().marks.isEmpty())
    }

    @Test
    fun `元に戻すと結果が消える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        viewModel.onRemoveItem(defaultItems[0].id)
        runCurrent()
        spinToEnd(viewModel)

        viewModel.onUndoRemove(defaultItems[0].id)
        runCurrent()

        assertNull(uiState().outcome)
    }

    @Test
    fun `直前に削除した項目でなければ戻さない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)

        viewModel.onRemoveItem(defaultItems[0].id)
        viewModel.onRemoveItem(defaultItems[1].id)
        runCurrent()
        // 多段の取り消しは持たない。前の削除はもう戻せない
        viewModel.onUndoRemove(defaultItems[0].id)
        runCurrent()

        assertEquals(listOf("C", "D"), repository.current(ListKind.ROULETTE).map { item -> item.label })
    }

    @Test
    fun `読み込んだあとは前のリストから消した項目を戻さない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val otherList = SavedList(name = "チーム", items = listOf(Item(label = "X"), Item(label = "Y")))
        val viewModel =
            createViewModel(itemsRepository = repository, savedListsRepository = FakeSavedListsRepository(listOf(otherList)))
        observe(viewModel)

        viewModel.onRemoveItem(defaultItems[0].id)
        runCurrent()
        viewModel.onLoadList(otherList.id)
        runCurrent()
        viewModel.onUndoRemove(defaultItems[0].id)
        runCurrent()

        assertEquals(listOf("X", "Y"), repository.current(ListKind.ROULETTE).map { item -> item.label })
    }

    // ── フリックとドラッグ ───────────────────────────────────────────────────

    @Test
    fun `フリックの周回数を渡しても結果は針の下の項目と一致する`() = runTest {
        for (seed in 0 until 20) {
            val viewModel = createViewModel(random = Random(seed))
            val uiState = observe(viewModel)

            viewModel.onWheelRelease(angularVelocity = SpinConfig.FLICK_MAX_ANGULAR_VELOCITY, dragRotation = 0.0)
            runCurrent()
            viewModel.onSpinFinished()
            runCurrent()

            val outcome = uiState().outcome!!
            assertEquals(outcome.index, RouletteMath.indexUnderPointer(uiState().spin.targetRotation.toDouble(), defaultItems.size))
        }
    }

    @Test
    fun `反時計回りのフリックでは累積角が減り結果は変わらない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        viewModel.onItemLongPress(defaultItems[3].id)

        viewModel.onWheelRelease(angularVelocity = -900.0, dragRotation = -20.0)
        runCurrent()
        viewModel.onSpinFinished()
        runCurrent()

        assertTrue(uiState().spin.targetRotation < -20f)
        assertEquals(3, uiState().outcome?.index)
        assertEquals(3, RouletteMath.indexUnderPointer(uiState().spin.targetRotation.toDouble(), defaultItems.size))
    }

    @Test
    fun `ボタンは直前のフリックの向きを引き継ぐ`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onWheelRelease(angularVelocity = -900.0, dragRotation = 0.0)
        runCurrent()
        viewModel.onSpinFinished()
        runCurrent()
        val afterFlick = uiState().spin.targetRotation

        spinToEnd(viewModel)

        assertTrue(uiState().spin.targetRotation < afterFlick)
    }

    @Test
    fun `始められなかったフリックの向きは引き継がない`() = runTest {
        // 項目が足りず回せない間に反時計回りに払っても、ボタンは時計回りのまま
        val repository = FakeItemsRepository(defaultItems.take(1))
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)

        viewModel.onWheelRelease(angularVelocity = -900.0, dragRotation = 0.0)
        runCurrent()
        repository.save(ListKind.ROULETTE, defaultItems)
        viewModel.onSpinClick()
        runCurrent()

        assertTrue(uiState().spin.targetRotation > 0f)
    }

    @Test
    fun `弱いドラッグでも動かした向きに最小の周回数で回す`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onWheelRelease(angularVelocity = 10.0, dragRotation = -15.0)
        runCurrent()

        val minimum = FlickSpin.releaseSpin(10.0, -15.0, fallback = SpinDirection.CLOCKWISE)
        assertEquals(SpinConfig.FULL_SPIN_RANGE.first, minimum.fullSpins)
        assertTrue(uiState().spinning)
        assertTrue(uiState().spin.targetRotation < -15f)
    }

    @Test
    fun `指で回した角度からスピンを始めても結果は変わらない`() = runTest {
        for (seed in 0 until 20) {
            val viewModel = createViewModel(random = Random(seed))
            val uiState = observe(viewModel)
            viewModel.onItemLongPress(defaultItems[1].id)

            viewModel.onWheelRelease(angularVelocity = null, dragRotation = 137.0)
            runCurrent()
            assertEquals(137f, uiState().spin.targetRotation)
            assertFalse(uiState().spinning)

            spinToEnd(viewModel)

            assertEquals(1, uiState().outcome?.index)
            assertEquals(1, RouletteMath.indexUnderPointer(uiState().spin.targetRotation.toDouble(), defaultItems.size))
        }
    }

    @Test
    fun `フリックの初速は曲線にだけ効き結果は変わらない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        viewModel.onItemLongPress(defaultItems[2].id)

        viewModel.onWheelRelease(angularVelocity = 1_500.0, dragRotation = 0.0)
        runCurrent()

        assertTrue(uiState().spin.easing.y1 != SpinConfig.EASING.y1.toFloat())
        assertEquals(SpinConfig.EASING.x2.toFloat(), uiState().spin.easing.x2)
        assertEquals(4_500, uiState().spin.durationMillis)

        viewModel.onSpinFinished()
        runCurrent()
        assertEquals(2, uiState().outcome?.index)
    }

    @Test
    fun `演出中は指で回せずフリックも始めない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        viewModel.onSpinClick()
        runCurrent()
        val target = uiState().spin.targetRotation

        viewModel.onWheelRelease(angularVelocity = 900.0, dragRotation = 45.0)
        runCurrent()

        assertEquals(target, uiState().spin.targetRotation)
        assertEquals(1, uiState().spin.spinCount)
    }

    // ── 触覚と回転音 ─────────────────────────────────────────────────────────

    @Test
    fun `スピンの開始と境目と停止で触覚を鳴らす`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)

        viewModel.onSpinClick()
        runCurrent()
        assertEquals(HapticEffect.MEDIUM, hapticPlayer.played.first())

        advanceTimeBy((SpinConfig.DURATION_SECONDS * 1_000).toLong())
        runCurrent()
        val ticks = hapticPlayer.played.count { effect -> effect == HapticEffect.SELECTION }
        assertTrue(ticks > 0)

        viewModel.onSpinFinished()
        runCurrent()
        assertEquals(HapticEffect.SUCCESS, hapticPlayer.played.last())
    }

    @Test
    fun `スピンが終わると刻みも止まる`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)

        viewModel.onSpinClick()
        runCurrent()
        viewModel.onSpinFinished()
        runCurrent()
        val playedAtFinish = hapticPlayer.played.size

        advanceTimeBy((SpinConfig.DURATION_SECONDS * 1_000).toLong())
        runCurrent()

        assertEquals(playedAtFinish, hapticPlayer.played.size)
    }

    @Test
    fun `スピンの開始で回転音の時刻を渡す`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)

        viewModel.onSpinClick()
        runCurrent()

        val times = soundPlayer.plays.single()
        assertTrue(times.isNotEmpty())
        assertTrue(times.all { time -> time in 0.0..SpinConfig.DURATION_SECONDS })
    }

    @Test
    fun `設定がOFFなら触覚も回転音も鳴らさない`() = runTest {
        val settings = FakeUserSettingsRepository(UserSettings(hapticsEnabled = false, soundEnabled = false))
        val viewModel = createViewModel(settingsRepository = settings)
        observe(viewModel)

        viewModel.onItemLongPress(defaultItems[0].id)
        viewModel.onSpinClick()
        runCurrent()
        advanceTimeBy((SpinConfig.FALLBACK_SECONDS * 1_000).toLong() + 1)
        runCurrent()
        viewModel.onWheelBoundaryCrossed(100.0)
        runCurrent()

        assertTrue(hapticPlayer.played.isEmpty())
        assertTrue(soundPlayer.plays.isEmpty())
        assertEquals(0, soundPlayer.clickCount)
        assertEquals(0, soundPlayer.prepareCount)
    }

    @Test
    fun `長押しで指定が切り替わると軽い触覚を返す`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)

        viewModel.onItemLongPress(defaultItems[0].id)
        runCurrent()

        assertEquals(listOf(HapticEffect.LIGHT), hapticPlayer.played)
    }

    @Test
    fun `指で動かして境目を越えると強い触覚と回転音を間引いて鳴らす`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)

        viewModel.onWheelBoundaryCrossed(10.0)
        // 回転音の間隔より詰まった境目は鳴らさない
        viewModel.onWheelBoundaryCrossed(10.01)
        viewModel.onWheelBoundaryCrossed(10.5)
        runCurrent()

        assertEquals(listOf(HapticEffect.MEDIUM, HapticEffect.MEDIUM), hapticPlayer.played)
        assertEquals(2, soundPlayer.clickCount)
    }

    @Test
    fun `演出中は境目の音と触覚を鳴らさない`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)
        viewModel.onSpinClick()
        runCurrent()
        hapticPlayer.played.clear()

        viewModel.onWheelBoundaryCrossed(10.0)
        runCurrent()

        assertTrue(hapticPlayer.played.none { effect -> effect == HapticEffect.MEDIUM })
        assertEquals(0, soundPlayer.clickCount)
    }

    @Test
    fun `画面から離れると回転音を止める`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)

        viewModel.onScreenHidden()

        assertEquals(1, soundPlayer.stopCount)
    }

    // ── 動きを減らす・画面キャプチャ ─────────────────────────────────────────

    @Test
    fun `動きを減らす設定では回さずに短い時間で結果を出し境目も鳴らさない`() = runTest {
        val viewModel = createViewModel(reduceMotionMonitor = FakeReduceMotionMonitor(initial = true))
        val uiState = observe(viewModel)
        assertTrue(uiState().reducedMotion)

        viewModel.onSpinClick()
        runCurrent()
        assertTrue(soundPlayer.plays.single().isEmpty())

        advanceTimeBy((SpinConfig.REDUCED_MOTION_DURATION_SECONDS * 1_000).toLong() + 1)
        runCurrent()

        assertFalse(uiState().spinning)
        assertTrue(uiState().outcome != null)
        assertTrue(hapticPlayer.played.none { effect -> effect == HapticEffect.SELECTION })
    }

    @Test
    fun `画面キャプチャの状態を出す`() = runTest {
        val uiState = observe(createViewModel())
        assertFalse(uiState().screenCaptured)

        screenCaptureMonitor.isCaptured.value = true
        runCurrent()

        assertTrue(uiState().screenCaptured)
    }

    // ── コピーと共有 ─────────────────────────────────────────────────────────

    @Test
    fun `コピーと共有は渡した本文をそのまま使う`() = runTest {
        val viewModel = createViewModel()

        viewModel.onCopyResult("結果: A")
        viewModel.onShareResult("結果: A\n\nデキレーレット")

        assertEquals(listOf("結果: A"), resultSharer.copied)
        assertEquals(listOf("結果: A\n\nデキレーレット"), resultSharer.shared)
    }
}
