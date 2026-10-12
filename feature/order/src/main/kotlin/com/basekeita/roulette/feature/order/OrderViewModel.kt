package com.basekeita.roulette.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.RemovedItem
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.domain.item.AddItemsUseCase
import com.basekeita.roulette.domain.item.RemoveItemUseCase
import com.basekeita.roulette.domain.item.RestoreItemUseCase
import com.basekeita.roulette.domain.order.RevealTiming
import com.basekeita.roulette.domain.order.ShuffleOrderUseCase
import com.basekeita.roulette.feature.order.uimodel.OrderResultUiModel
import com.basekeita.roulette.feature.order.uimodel.OrderedItemUiModel
import com.basekeita.roulette.ui.feedback.HapticEffect
import com.basekeita.roulette.ui.feedback.HapticPlayer
import com.basekeita.roulette.ui.feedback.ReduceMotionMonitor
import com.basekeita.roulette.ui.feedback.ResultSharer
import com.basekeita.roulette.ui.feedback.ScreenCaptureMonitor
import com.basekeita.roulette.ui.feedback.launchTicks
import com.basekeita.roulette.ui.rule.DuplicateLabels
import com.basekeita.roulette.ui.uimodel.ItemListLimits
import com.basekeita.roulette.ui.uimodel.ItemUiModel
import com.basekeita.roulette.ui.uimodel.MarkUiModel
import com.basekeita.roulette.ui.uimodel.SavedListSummaryUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * 順番決め画面。項目は Repository に保存し、先頭・末尾の指定と並べ替えの結果は ViewModel の中だけで持つ（指定は保存しない。SPEC §12）。
 *
 * 並べ替えは開始時に `ShuffleOrderUseCase` が結果と演出の時間を決める。結果の行は Screen が 1 件ずつ遅らせて出し、
 * 全件が出終わる時刻に ViewModel が演出を終える。「アニメーションを削除」のときは 1 件ずつ出さず、まとめて出す。
 *
 * 触覚は、設定で ON のときだけ ViewModel が `ui/feedback` を呼んで鳴らす（行が 1 件現れるごとの刻みと、全件そろった手応え）。
 */
@HiltViewModel
class OrderViewModel
@Inject
constructor(
    private val itemsRepository: ItemsRepository,
    private val savedListsRepository: SavedListsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val shuffleOrder: ShuffleOrderUseCase,
    private val addItems: AddItemsUseCase,
    private val removeItem: RemoveItemUseCase,
    private val restoreItem: RestoreItemUseCase,
    private val hapticPlayer: HapticPlayer,
    private val reduceMotionMonitor: ReduceMotionMonitor,
    screenCaptureMonitor: ScreenCaptureMonitor,
    private val resultSharer: ResultSharer,
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

    /**
     * @property marks 項目の id ごとの指定。伏せるかどうかは項目リストが決める
     * @property revealing 演出中。入力・隠しジェスチャ・ボタンを止め、印を伏せる
     * @property result 並べ替えの結果。まだ並べ替えていないときと、項目を触って結果が消えたあとは null
     * @property reducedMotion 「アニメーションを削除」。結果の行と行の開閉を動かさずに出す
     * @property screenCaptured 画面が録画されている。印を伏せる
     */
    data class UiState(
        val items: List<ItemUiModel>,
        val marks: Map<UUID, MarkUiModel>,
        val savedLists: List<SavedListSummaryUiModel>,
        val limits: ItemListLimits,
        val revealing: Boolean,
        val canShuffle: Boolean,
        val result: OrderResultUiModel?,
        val reducedMotion: Boolean,
        val screenCaptured: Boolean,
    )

    /**
     * ViewModel の中だけで持つ状態。
     *
     * @property ordered 並べ替えの結果。項目を触ったら消す
     * @property shuffledItems 並べ替えたときの項目。項目がこれと違えば（設定から初期状態に戻したなど）結果を出さない
     * @property shuffleCount 並べ替えた回数。結果の行を作り直して演出をやり直すために使う
     * @property revealedAtOnce 並べ替えたときに「アニメーションを削除」だった。結果を 1 件ずつ遅らせずに出す
     */
    private data class Session(
        val firstId: UUID? = null,
        val lastId: UUID? = null,
        val revealing: Boolean = false,
        val ordered: List<Item>? = null,
        val shuffledItems: List<Item> = emptyList(),
        val shuffleCount: Int = 0,
        val revealedAtOnce: Boolean = false,
    )

    /** 画面の外から届く端末の状態。 */
    private data class Environment(
        val reducedMotion: Boolean,
        val screenCaptured: Boolean,
    )

    private val session = MutableStateFlow(Session())
    private var revealJob: Job? = null
    private var tickJob: Job? = null

    /** 直前に削除した項目。「元に戻す」で使う。表示には出さない（Snackbar は項目リストが持つ）。 */
    private var lastRemoved: RemovedItem? = null

    private val environment =
        combine(reduceMotionMonitor.reducedMotion, screenCaptureMonitor.isCaptured) { reducedMotion, screenCaptured ->
            Environment(reducedMotion = reducedMotion, screenCaptured = screenCaptured)
        }

    val viewState: StateFlow<ViewState> =
        combine(
            itemsRepository.items(ListKind.ORDER),
            savedListsRepository.lists,
            session,
            environment,
        ) { items, savedLists, current, environment ->
            ViewState.Success(uiStateOf(items, savedLists, current, environment)) as ViewState
        }.catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    /** 「並べ替え」ボタン。並べ替えられないとき（演出中・項目が足りない）は何もしない。 */
    fun onShuffleClick() {
        if (session.value.revealing) return

        viewModelScope.launch {
            val items = itemsRepository.items(ListKind.ORDER).first()
            val settings = userSettingsRepository.settings.first()
            val current = session.value
            // 項目を読む間に別の操作で演出が始まっていたら重ねない
            if (current.revealing) return@launch

            val reducedMotion = reduceMotionMonitor.reducedMotion.value
            val plan =
                shuffleOrder(
                    items = items,
                    firstId = current.firstId,
                    lastId = current.lastId,
                    reducedMotion = reducedMotion,
                ) ?: return@launch

            session.update { state ->
                state.copy(
                    revealing = true,
                    ordered = plan.ordered,
                    shuffledItems = items,
                    shuffleCount = state.shuffleCount + 1,
                    revealedAtOnce = reducedMotion,
                )
            }
            // 行が現れる時刻は Screen の遅延と同じ式で決まるので、同じ時刻に刻む
            tickJob?.cancel()
            if (settings.hapticsEnabled) {
                tickJob = viewModelScope.launchTicks(plan.hapticTimes) { hapticPlayer.play(HapticEffect.SELECTION) }
            }
            startRevealTimer(plan.revealDurationSeconds)
        }
    }

    /** 入力欄の行（正規化済み）をまとめて項目に足す。上限に収まらない分は切り捨てる。足せたら前の結果を消す。 */
    fun onAddItems(labels: List<String>) {
        viewModelScope.launch {
            val addedCount = addItems(ListKind.ORDER, labels)
            if (addedCount == 0) return@launch

            session.update { state -> state.copy(ordered = null) }
        }
    }

    /** 項目を 1 件削除する。先頭・末尾に指定していた項目なら指定も外す。「元に戻す」のために削除した項目を覚えておく。 */
    fun onRemoveItem(id: UUID) {
        viewModelScope.launch {
            val removed = removeItem(ListKind.ORDER, id) ?: return@launch

            lastRemoved = removed
            session.update { state ->
                val firstId =
                    if (state.firstId == id) {
                        null
                    } else {
                        state.firstId
                    }
                val lastId =
                    if (state.lastId == id) {
                        null
                    } else {
                        state.lastId
                    }
                state.copy(firstId = firstId, lastId = lastId, ordered = null)
            }
        }
    }

    /** 直前に削除した項目を元の位置に戻す。指定は復元しない。同じ項目が残っているか上限に達していれば戻さない。 */
    fun onUndoRemove(id: UUID) {
        val removed = lastRemoved?.takeIf { candidate -> candidate.item.id == id } ?: return

        lastRemoved = null
        viewModelScope.launch {
            val before = itemsRepository.items(ListKind.ORDER).first()
            restoreItem(ListKind.ORDER, removed)
            val after = itemsRepository.items(ListKind.ORDER).first()
            if (before == after) return@launch

            session.update { state -> state.copy(ordered = null) }
        }
    }

    /** 項目をすべて削除する。指定と結果も消える。 */
    fun onRemoveAllItems() {
        lastRemoved = null
        viewModelScope.launch {
            itemsRepository.save(ListKind.ORDER, emptyList())
            session.update { state -> state.copy(firstId = null, lastId = null, ordered = null) }
        }
    }

    /**
     * 長押しのたびに 先頭 → 末尾 → 解除 と回す。
     * 先頭と末尾はそれぞれ 1 項目までなので、付け替えると前の指定は落ちる。
     */
    fun onItemLongPress(id: UUID) {
        session.update { state ->
            if (state.firstId == id) {
                state.copy(firstId = null, lastId = id, ordered = null)
            } else if (state.lastId == id) {
                state.copy(lastId = null, ordered = null)
            } else {
                state.copy(firstId = id, ordered = null)
            }
        }
        // 指定が切り替わった瞬間の、本人の指にだけ伝わる軽い手応え
        viewModelScope.launch {
            if (userSettingsRepository.settings.first().hapticsEnabled) hapticPlayer.play(HapticEffect.LIGHT)
        }
    }

    /** いまの項目を名前を付けて保存する。名前が空、または上限に達しているときは保存しない。 */
    fun onSaveList(name: String) {
        viewModelScope.launch {
            val items = itemsRepository.items(ListKind.ORDER).first()
            savedListsRepository.save(name, items)
        }
    }

    /** 保存したリストで項目を置き換える。指定と結果は前のリストのものなので捨てる。 */
    fun onLoadList(id: UUID) {
        viewModelScope.launch {
            val savedList = savedListsRepository.lists.first().firstOrNull { list -> list.id == id } ?: return@launch

            lastRemoved = null
            itemsRepository.save(ListKind.ORDER, savedList.items)
            session.update { state -> state.copy(firstId = null, lastId = null, ordered = null) }
        }
    }

    /** 結果をクリップボードに書き込む。本文は Screen が組み立てる（指定の有無は載せない）。 */
    fun onCopyResult(text: String) {
        resultSharer.copy(text)
    }

    /** 結果を共有する。 */
    fun onShareResult(text: String) {
        resultSharer.share(text)
    }

    override fun onCleared() {
        revealJob?.cancel()
        tickJob?.cancel()
    }

    /** 全件が出終わる時刻に演出を終え、全件そろった手応えを返す。 */
    private fun startRevealTimer(seconds: Double) {
        revealJob?.cancel()
        revealJob =
            viewModelScope.launch {
                delay((seconds * 1_000).toLong())
                session.update { state -> state.copy(revealing = false) }
                if (userSettingsRepository.settings.first().hapticsEnabled) hapticPlayer.play(HapticEffect.SUCCESS)
            }
    }

    private fun uiStateOf(
        items: List<Item>,
        savedLists: List<SavedList>,
        current: Session,
        environment: Environment,
    ): UiState {
        val marks = buildMap {
            // 指定した項目が消えていれば（削除・読み込み・初期化）印も出さない
            current.firstId?.takeIf { id -> items.any { item -> item.id == id } }?.let { id -> put(id, MarkUiModel.FIRST) }
            current.lastId?.takeIf { id -> items.any { item -> item.id == id } }?.let { id -> put(id, MarkUiModel.LAST) }
        }
        // 並べ替えたあとに項目が変わっていれば（設定から初期状態に戻したなど、この画面の外で変わったときも）結果を出さない
        val ordered = current.ordered?.takeIf { items == current.shuffledItems }
        // 同名の行は項目から毎回求めるので、追加・読み込み・元に戻すのどの経路で入っても反映される
        val duplicateIds = DuplicateLabels.ids(items.map { item -> item.id to item.label }, Item.MAX_LABEL_LENGTH)
        return UiState(
            items = items.map { item -> ItemUiModel(id = item.id, label = item.label, isDuplicate = item.id in duplicateIds) },
            marks = marks,
            savedLists = savedLists.map { list -> SavedListSummaryUiModel(id = list.id, name = list.name, itemCount = list.items.size) },
            limits = itemListLimits,
            revealing = current.revealing,
            canShuffle = !current.revealing && items.size >= Item.MIN_COUNT,
            result = ordered?.let { orderedItems -> resultOf(orderedItems, current.shuffleCount, current.revealedAtOnce) },
            reducedMotion = environment.reducedMotion,
            screenCaptured = environment.screenCaptured,
        )
    }

    private fun resultOf(
        ordered: List<Item>,
        shuffleCount: Int,
        revealedAtOnce: Boolean,
    ): OrderResultUiModel = OrderResultUiModel(
        shuffleCount = shuffleCount,
        rows = ordered.mapIndexed { index, item ->
            OrderedItemUiModel(
                id = item.id,
                label = item.label,
                // 1 件ずつ遅らせて出す。全部そろうまで先の順位が読めないようにする
                revealDelayMillis = (RevealTiming.delay(index, reducedMotion = revealedAtOnce) * 1_000).roundToInt(),
            )
        },
    )

    private companion object {
        val itemListLimits =
            ItemListLimits(
                minItemCount = Item.MIN_COUNT,
                maxItemCount = Item.MAX_COUNT,
                maxLabelLength = Item.MAX_LABEL_LENGTH,
                maxSavedListCount = SavedList.MAX_COUNT,
            )
    }
}
