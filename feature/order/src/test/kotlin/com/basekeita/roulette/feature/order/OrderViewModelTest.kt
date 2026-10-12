package com.basekeita.roulette.feature.order

import app.cash.turbine.test
import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.data.definition.model.UserSettings
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.domain.item.AddItemsUseCase
import com.basekeita.roulette.domain.item.RemoveItemUseCase
import com.basekeita.roulette.domain.item.RestoreItemUseCase
import com.basekeita.roulette.domain.order.RevealTiming
import com.basekeita.roulette.domain.order.ShuffleOrderUseCase
import com.basekeita.roulette.feature.order.OrderViewModel.UiState
import com.basekeita.roulette.feature.order.OrderViewModel.ViewState
import com.basekeita.roulette.ui.feedback.HapticEffect
import com.basekeita.roulette.ui.feedback.HapticPlayer
import com.basekeita.roulette.ui.feedback.ReduceMotionMonitor
import com.basekeita.roulette.ui.feedback.ResultSharer
import com.basekeita.roulette.ui.feedback.ScreenCaptureMonitor
import com.basekeita.roulette.ui.uimodel.MarkUiModel
import com.basekeita.roulette.ui.uimodel.SavedListSummaryUiModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {
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
    private val screenCaptureMonitor = FakeScreenCaptureMonitor()
    private val resultSharer = FakeResultSharer()

    private fun createViewModel(
        itemsRepository: ItemsRepository = FakeItemsRepository(defaultItems),
        savedListsRepository: SavedListsRepository = FakeSavedListsRepository(),
        settingsRepository: UserSettingsRepository = FakeUserSettingsRepository(),
        random: Random = Random(1),
        reduceMotionMonitor: ReduceMotionMonitor = FakeReduceMotionMonitor(),
    ): OrderViewModel = OrderViewModel(
        itemsRepository = itemsRepository,
        savedListsRepository = savedListsRepository,
        userSettingsRepository = settingsRepository,
        shuffleOrder = ShuffleOrderUseCase(random),
        addItems = AddItemsUseCase(itemsRepository),
        removeItem = RemoveItemUseCase(itemsRepository),
        restoreItem = RestoreItemUseCase(itemsRepository),
        hapticPlayer = hapticPlayer,
        reduceMotionMonitor = reduceMotionMonitor,
        screenCaptureMonitor = screenCaptureMonitor,
        resultSharer = resultSharer,
    )

    /** `WhileSubscribed` の状態を購読しておき、いまの UiState を読めるようにする。 */
    private fun TestScope.observe(viewModel: OrderViewModel): () -> UiState {
        backgroundScope.launch { viewModel.viewState.collect {} }
        runCurrent()
        return { (viewModel.viewState.value as ViewState.Success).uiState }
    }

    private val revealMillis = (RevealTiming.duration(4, reducedMotion = false) * 1_000).toLong()

    /** 並べ替えて、演出が終わるまで進める。 */
    private fun TestScope.shuffleToEnd(viewModel: OrderViewModel) {
        viewModel.onShuffleClick()
        runCurrent()
        advanceTimeBy(revealMillis + 1)
        runCurrent()
    }

    // ── 表示 ─────────────────────────────────────────────────────────────────

    @Test
    fun `読み込むまではLoadingでそのあと保存した項目を出す`() = runTest {
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
            assertTrue(uiState.marks.isEmpty())
            assertNull(uiState.result)
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

    // ── 指定 ─────────────────────────────────────────────────────────────────

    @Test
    fun `長押しのたびに先頭から末尾から解除へ回る`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        val id = defaultItems[1].id

        viewModel.onItemLongPress(id)
        runCurrent()
        assertEquals(mapOf(id to MarkUiModel.FIRST), uiState().marks)

        viewModel.onItemLongPress(id)
        runCurrent()
        assertEquals(mapOf(id to MarkUiModel.LAST), uiState().marks)

        viewModel.onItemLongPress(id)
        runCurrent()
        assertTrue(uiState().marks.isEmpty())
    }

    @Test
    fun `先頭を別の項目に付け替えると前の先頭は外れる`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onItemLongPress(defaultItems[0].id)
        viewModel.onItemLongPress(defaultItems[2].id)
        runCurrent()

        assertEquals(mapOf(defaultItems[2].id to MarkUiModel.FIRST), uiState().marks)
    }

    @Test
    fun `先頭と末尾は別の項目に同時に付けられる`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        // 1 件目を末尾にしてから、2 件目を先頭にする
        viewModel.onItemLongPress(defaultItems[0].id)
        viewModel.onItemLongPress(defaultItems[0].id)
        viewModel.onItemLongPress(defaultItems[1].id)
        runCurrent()

        assertEquals(
            mapOf(defaultItems[0].id to MarkUiModel.LAST, defaultItems[1].id to MarkUiModel.FIRST),
            uiState().marks,
        )
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
        assertEquals(listOf("A", "C", "D"), repository.current(ListKind.ORDER).map { item -> item.label })
    }

    @Test
    fun `すべて削除すると項目も指定も結果も消える`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        viewModel.onItemLongPress(defaultItems[0].id)
        shuffleToEnd(viewModel)

        viewModel.onRemoveAllItems()
        runCurrent()

        assertTrue(uiState().items.isEmpty())
        assertTrue(uiState().marks.isEmpty())
        assertNull(uiState().result)
        assertTrue(repository.current(ListKind.ORDER).isEmpty())
    }

    // ── 並べ替え ─────────────────────────────────────────────────────────────

    @Test
    fun `項目が2つ未満なら並べ替えられない`() = runTest {
        val viewModel = createViewModel(itemsRepository = FakeItemsRepository(defaultItems.take(1)))
        val uiState = observe(viewModel)

        assertFalse(uiState().canShuffle)

        viewModel.onShuffleClick()
        runCurrent()

        assertFalse(uiState().revealing)
        assertNull(uiState().result)
    }

    @Test
    fun `指定した項目は先頭と末尾に来て残りはすべて並ぶ`() = runTest {
        for (seed in 0 until 20) {
            val viewModel = createViewModel(random = Random(seed))
            val uiState = observe(viewModel)
            // B を末尾にしてから、C を先頭にする
            viewModel.onItemLongPress(defaultItems[1].id)
            viewModel.onItemLongPress(defaultItems[1].id)
            viewModel.onItemLongPress(defaultItems[2].id)

            shuffleToEnd(viewModel)

            val rows = uiState().result!!.rows
            assertEquals(defaultItems[2].id, rows.first().id)
            assertEquals(defaultItems[1].id, rows.last().id)
            assertEquals(defaultItems.map { item -> item.id }.toSet(), rows.map { row -> row.id }.toSet())
        }
    }

    @Test
    fun `結果の行は1件ずつ遅らせて出す`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        shuffleToEnd(viewModel)

        assertEquals(listOf(0, 160, 320, 480), uiState().result!!.rows.map { row -> row.revealDelayMillis })
    }

    @Test
    fun `演出中は二重に始められず全件が出終わると次を始められる`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        viewModel.onShuffleClick()
        runCurrent()
        val firstResult = uiState().result

        assertTrue(uiState().revealing)
        assertFalse(uiState().canShuffle)
        assertNotNull(firstResult)

        viewModel.onShuffleClick()
        runCurrent()
        assertEquals(firstResult, uiState().result)

        advanceTimeBy(revealMillis - 1)
        runCurrent()
        assertTrue(uiState().revealing)

        advanceTimeBy(2)
        runCurrent()
        assertFalse(uiState().revealing)
        assertTrue(uiState().canShuffle)
    }

    @Test
    fun `並べ替えるたびに結果の回数が増える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)

        shuffleToEnd(viewModel)
        shuffleToEnd(viewModel)

        assertEquals(2, uiState().result!!.shuffleCount)
    }

    @Test
    fun `演出中も指定は渡したまま項目リストが伏せる`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        val id = defaultItems[0].id

        viewModel.onItemLongPress(id)
        viewModel.onShuffleClick()
        runCurrent()

        assertTrue(uiState().revealing)
        assertEquals(mapOf(id to MarkUiModel.FIRST), uiState().marks)
    }

    // ── 結果を消す操作 ───────────────────────────────────────────────────────

    @Test
    fun `項目を追加すると結果が消え保存される`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        shuffleToEnd(viewModel)

        viewModel.onAddItems(listOf("  E  "))
        runCurrent()

        assertNull(uiState().result)
        assertEquals(listOf("A", "B", "C", "D", "E"), repository.current(ListKind.ORDER).map { item -> item.label })
    }

    @Test
    fun `空白だけの入力では項目も結果も変わらない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        shuffleToEnd(viewModel)
        val result = uiState().result

        viewModel.onAddItems(listOf("   "))
        runCurrent()

        assertEquals(result, uiState().result)
        assertEquals(defaultItems, repository.current(ListKind.ORDER))
    }

    @Test
    fun `項目を削除すると結果が消える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        shuffleToEnd(viewModel)

        viewModel.onRemoveItem(defaultItems[0].id)
        runCurrent()

        assertNull(uiState().result)
    }

    @Test
    fun `長押しすると結果が消える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        shuffleToEnd(viewModel)

        viewModel.onItemLongPress(defaultItems[0].id)
        runCurrent()

        assertNull(uiState().result)
    }

    @Test
    fun `項目が画面の外で変わると結果を出さない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        val uiState = observe(viewModel)
        shuffleToEnd(viewModel)

        // 設定の「項目を初期状態に戻す」にあたる
        repository.save(ListKind.ORDER, listOf(Item(label = "X"), Item(label = "Y")))
        runCurrent()

        assertNull(uiState().result)
    }

    // ── 保存したリスト ───────────────────────────────────────────────────────

    @Test
    fun `保存したリストを件数つきで出す`() = runTest {
        val savedList = SavedList(name = "チーム", items = defaultItems.take(3))
        val uiState = observe(createViewModel(savedListsRepository = FakeSavedListsRepository(listOf(savedList))))

        assertEquals(listOf(SavedListSummaryUiModel(id = savedList.id, name = "チーム", itemCount = 3)), uiState().savedLists)
    }

    @Test
    fun `いまの項目を名前を付けて保存する`() = runTest {
        val savedListsRepository = FakeSavedListsRepository()
        val viewModel = createViewModel(savedListsRepository = savedListsRepository)
        observe(viewModel)

        viewModel.onSaveList("チーム")
        runCurrent()

        val saved = savedListsRepository.lists.value.single()
        assertEquals("チーム", saved.name)
        assertEquals(defaultItems, saved.items)
    }

    @Test
    fun `読み込むと項目が置き換わり指定と結果が消える`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        // いまの項目をそのまま保存したリスト。id が同じでも指定は消える
        val savedList = SavedList(name = "いまの項目", items = defaultItems)
        val otherList = SavedList(name = "ランチ", items = listOf(Item(label = "X"), Item(label = "Y")))
        val viewModel =
            createViewModel(
                itemsRepository = repository,
                savedListsRepository = FakeSavedListsRepository(listOf(savedList, otherList)),
            )
        val uiState = observe(viewModel)
        viewModel.onItemLongPress(defaultItems[0].id)
        shuffleToEnd(viewModel)

        viewModel.onLoadList(savedList.id)
        runCurrent()

        assertTrue(uiState().marks.isEmpty())
        assertNull(uiState().result)

        viewModel.onLoadList(otherList.id)
        runCurrent()

        assertEquals(listOf("X", "Y"), repository.current(ListKind.ORDER).map { item -> item.label })
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

        viewModel.onAddItems(listOf("E", "F"))
        runCurrent()

        assertEquals(listOf("A", "B", "C", "D", "E", "F"), repository.current(ListKind.ORDER).map { item -> item.label })
    }

    @Test
    fun `まとめて追加しても上限を超えた分は切り捨てる`() = runTest {
        val nearlyFull = List(Item.MAX_COUNT - 1) { index -> Item(label = "項目$index") }
        val repository = FakeItemsRepository(nearlyFull)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)

        viewModel.onAddItems(listOf("X", "Y"))
        runCurrent()

        val labels = repository.current(ListKind.ORDER).map { item -> item.label }
        assertEquals(Item.MAX_COUNT, labels.size)
        assertEquals("X", labels.last())
    }

    @Test
    fun `まとめて追加すると結果が消える`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        shuffleToEnd(viewModel)

        viewModel.onAddItems(listOf("E", "F"))
        runCurrent()

        assertNull(uiState().result)
    }

    @Test
    fun `同名の項目には注意を出す`() = runTest {
        val items = listOf(Item(label = "A"), Item(label = " A "), Item(label = "B"))
        val uiState = observe(createViewModel(itemsRepository = FakeItemsRepository(items)))

        assertEquals(listOf(true, true, false), uiState().items.map { item -> item.isDuplicate })
    }

    // ── 元に戻す ─────────────────────────────────────────────────────────────

    @Test
    fun `元に戻すと同じIDで元の位置に入る`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)

        viewModel.onRemoveItem(defaultItems[2].id)
        runCurrent()
        viewModel.onUndoRemove(defaultItems[2].id)
        runCurrent()

        assertEquals(defaultItems, repository.current(ListKind.ORDER))
    }

    @Test
    fun `元に戻しても先頭末尾の指定は復元しない`() = runTest {
        val viewModel = createViewModel()
        val uiState = observe(viewModel)
        val id = defaultItems[0].id

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
        shuffleToEnd(viewModel)

        viewModel.onUndoRemove(defaultItems[0].id)
        runCurrent()

        assertNull(uiState().result)
    }

    @Test
    fun `すべて削除したあとは戻さない`() = runTest {
        val repository = FakeItemsRepository(defaultItems)
        val viewModel = createViewModel(itemsRepository = repository)
        observe(viewModel)

        viewModel.onRemoveItem(defaultItems[0].id)
        runCurrent()
        viewModel.onRemoveAllItems()
        runCurrent()
        viewModel.onUndoRemove(defaultItems[0].id)
        runCurrent()

        assertTrue(repository.current(ListKind.ORDER).isEmpty())
    }

    // ── 触覚 ─────────────────────────────────────────────────────────────────

    @Test
    fun `結果の行が現れるごとに刻み全件そろうと手応えを返す`() = runTest {
        val viewModel = createViewModel()
        observe(viewModel)

        shuffleToEnd(viewModel)

        assertEquals(List(defaultItems.size) { HapticEffect.SELECTION } + HapticEffect.SUCCESS, hapticPlayer.played)
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
    fun `設定がOFFなら触覚を鳴らさない`() = runTest {
        val viewModel = createViewModel(settingsRepository = FakeUserSettingsRepository(UserSettings(hapticsEnabled = false)))
        observe(viewModel)

        viewModel.onItemLongPress(defaultItems[0].id)
        shuffleToEnd(viewModel)

        assertTrue(hapticPlayer.played.isEmpty())
    }

    // ── 動きを減らす・画面キャプチャ ─────────────────────────────────────────

    @Test
    fun `動きを減らす設定では1件ずつ出さず刻まない`() = runTest {
        val viewModel = createViewModel(reduceMotionMonitor = FakeReduceMotionMonitor(initial = true))
        val uiState = observe(viewModel)
        assertTrue(uiState().reducedMotion)

        viewModel.onShuffleClick()
        runCurrent()
        assertTrue(uiState().result!!.rows.all { row -> row.revealDelayMillis == 0 })

        advanceTimeBy((RevealTiming.REDUCED_MOTION_DURATION_SECONDS * 1_000).toLong() + 1)
        runCurrent()

        assertFalse(uiState().revealing)
        assertEquals(listOf(HapticEffect.SUCCESS), hapticPlayer.played)
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

        viewModel.onCopyResult("1. A\n2. B")
        viewModel.onShareResult("1. A\n2. B\n\nデキレーレット")

        assertEquals(listOf("1. A\n2. B"), resultSharer.copied)
        assertEquals(listOf("1. A\n2. B\n\nデキレーレット"), resultSharer.shared)
    }
}
