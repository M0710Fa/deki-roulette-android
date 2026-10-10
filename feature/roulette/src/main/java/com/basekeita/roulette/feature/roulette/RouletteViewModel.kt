package com.basekeita.roulette.feature.roulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basekeita.roulette.data.definition.model.GlowStyle
import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.RemovedItem
import com.basekeita.roulette.data.definition.model.SavedList
import com.basekeita.roulette.data.definition.model.SpinDirection
import com.basekeita.roulette.data.definition.repository.ItemsRepository
import com.basekeita.roulette.data.definition.repository.SavedListsRepository
import com.basekeita.roulette.data.definition.repository.UserSettingsRepository
import com.basekeita.roulette.domain.item.AddItemsUseCase
import com.basekeita.roulette.domain.item.RemoveItemUseCase
import com.basekeita.roulette.domain.item.RestoreItemUseCase
import com.basekeita.roulette.domain.spin.CubicBezierCurve
import com.basekeita.roulette.domain.spin.DecideSpinUseCase
import com.basekeita.roulette.domain.spin.FlickSpin
import com.basekeita.roulette.domain.spin.SpinConfig
import com.basekeita.roulette.domain.spin.SpinOutcome
import com.basekeita.roulette.feature.roulette.viewdata.GlowStyleUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinAnimationUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinEasingUiModel
import com.basekeita.roulette.feature.roulette.viewdata.SpinOutcomeUiModel
import com.basekeita.roulette.feature.roulette.viewdata.WheelSliceUiModel
import com.basekeita.roulette.ui.feedback.HapticEffect
import com.basekeita.roulette.ui.feedback.HapticPlayer
import com.basekeita.roulette.ui.feedback.ReduceMotionMonitor
import com.basekeita.roulette.ui.feedback.ResultSharer
import com.basekeita.roulette.ui.feedback.ScreenCaptureMonitor
import com.basekeita.roulette.ui.feedback.SpinSoundPlayer
import com.basekeita.roulette.ui.feedback.launchTicks
import com.basekeita.roulette.ui.rule.DuplicateLabels
import com.basekeita.roulette.ui.rule.WheelDrag
import com.basekeita.roulette.ui.viewdata.ItemListLimits
import com.basekeita.roulette.ui.viewdata.ItemUiModel
import com.basekeita.roulette.ui.viewdata.MarkViewData
import com.basekeita.roulette.ui.viewdata.SavedListSummaryUiModel
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

/**
 * ルーレット画面。項目は Repository に保存し、当たりの指定・回転・結果は ViewModel の中だけで持つ（指定は保存しない。SPEC §12）。
 *
 * スピンは開始時に `DecideSpinUseCase` が結果と止まる累積角を決める。回転は Screen が `SpinAnimationUiModel` に従って回し、
 * 終わったら `onSpinFinished` を呼ぶ。完了が届かないとき（画面を離れた等）に備えて、保険のタイマーでも終える。
 * 「アニメーションを削除」のときは回さず、保険のタイマー（0.32 秒）で終える。
 *
 * 触覚と回転音は、設定で ON のときだけ ViewModel が `ui/feedback` を呼んで鳴らす。
 */
@HiltViewModel
class RouletteViewModel
@Inject
constructor(
    private val itemsRepository: ItemsRepository,
    private val savedListsRepository: SavedListsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val decideSpin: DecideSpinUseCase,
    private val addItems: AddItemsUseCase,
    private val removeItem: RemoveItemUseCase,
    private val restoreItem: RestoreItemUseCase,
    private val hapticPlayer: HapticPlayer,
    private val spinSoundPlayer: SpinSoundPlayer,
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
     * @property spinning 演出中。入力・隠しジェスチャ・ボタン・盤面のドラッグを止め、印を伏せる
     * @property outcome 止まったスライス。演出中と、項目を触って結果が消えたあとは null
     * @property savedLists 項目リストの見出しのメニューで読み込める、保存したリスト
     * @property reducedMotion 「アニメーションを削除」。盤面を回さず、強調・結果の帯・行の開閉の動きを省く
     * @property screenCaptured 画面が録画されている。印を伏せる
     */
    data class UiState(
        val items: List<ItemUiModel>,
        val slices: List<WheelSliceUiModel>,
        val marks: Map<UUID, MarkViewData>,
        val savedLists: List<SavedListSummaryUiModel>,
        val limits: ItemListLimits,
        val spinning: Boolean,
        val canSpin: Boolean,
        val spin: SpinAnimationUiModel,
        val outcome: SpinOutcomeUiModel?,
        val glowStyle: GlowStyleUiModel,
        val reducedMotion: Boolean,
        val screenCaptured: Boolean,
    )

    /**
     * ViewModel の中だけで持つ状態。
     *
     * @property rotation 累積回転角（度）。スピンを始めた時点で止まる角度に進める。指で回した分も取り込む
     * @property pendingOutcome 開始時に決めた結果。止まるまでは `outcome` に出さない
     * @property spunItems スピンを始めたときの項目。項目がこれと違えば（設定から初期状態に戻したなど）結果を出さない
     * @property lastDirection 「スピン」ボタンで回す向き。直前のフリックの向きを引き継ぐ。保存しないので起動ごとに時計回りから始まる
     */
    private data class Session(
        val targetId: UUID? = null,
        val spinning: Boolean = false,
        val rotation: Double = 0.0,
        val spinCount: Int = 0,
        val easing: CubicBezierCurve = SpinConfig.EASING,
        val lastDirection: SpinDirection = SpinDirection.CLOCKWISE,
        val pendingOutcome: SpinOutcome? = null,
        val outcome: SpinOutcome? = null,
        val spunItems: List<Item> = emptyList(),
    )

    /** 画面の外から届く端末の状態。 */
    private data class Environment(
        val reducedMotion: Boolean,
        val screenCaptured: Boolean,
    )

    private val session = MutableStateFlow(Session())
    private var fallbackJob: Job? = null
    private var tickJob: Job? = null

    /** 直前に削除した項目。「元に戻す」で使う。表示には出さない（トーストは項目リストが持つ）。 */
    private var lastRemoved: RemovedItem? = null

    /** 指で動かしている間に境目を越えたときの回転音と触覚の間引き。 */
    private val dragFeedbackThrottle =
        WheelDrag.FeedbackThrottle(
            clickMinIntervalSeconds = SpinConfig.CLICK_MIN_INTERVAL_SECONDS,
            hapticMinIntervalSeconds = SpinConfig.HAPTIC_MIN_INTERVAL_SECONDS,
        )

    private val environment =
        combine(reduceMotionMonitor.reducedMotion, screenCaptureMonitor.isCaptured) { reducedMotion, screenCaptured ->
            Environment(reducedMotion = reducedMotion, screenCaptured = screenCaptured)
        }

    val viewState: StateFlow<ViewState> =
        combine(
            itemsRepository.items(ListKind.ROULETTE),
            savedListsRepository.lists,
            userSettingsRepository.settings,
            session,
            environment,
        ) { items, savedLists, settings, current, device ->
            ViewState.Success(uiStateOf(items, savedLists, settings.glowStyle, current, device)) as ViewState
        }.catch { error -> emit(ViewState.Error(error.message.orEmpty())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewState.Loading)

    init {
        viewModelScope.launch {
            if (userSettingsRepository.settings.first().soundEnabled) spinSoundPlayer.prepare()
        }
    }

    /** 「スピン」ボタン。直前のフリックの向きで回す。回せないとき（演出中・項目が足りない）は何もしない。 */
    fun onSpinClick() {
        startSpin(fullSpins = null, direction = null, releaseVelocity = null)
    }

    /**
     * 盤面から指を離した。追従で回した角度を取り込み、その角度からフリックのスピンを始める。
     * 強さは周回数にだけ反映し、フリックした向きに回す（閾値未満でも最小の周回数で動かした向きに回す）。
     * 盤面は離した瞬間の速さで回り出し、そこから減速する。止まる位置の決まり方は変わらない。
     *
     * @param angularVelocity 指を離す直前の角速度（度/秒、時計回りが正）。指を離さずに取り消されたときは null で、回さない
     * @param dragRotation 追従で回した角度（度）
     */
    fun onWheelRelease(
        angularVelocity: Double?,
        dragRotation: Double,
    ) {
        if (session.value.spinning) return

        session.update { state -> state.copy(rotation = state.rotation + dragRotation) }
        if (angularVelocity == null) return

        val flick = FlickSpin.releaseSpin(angularVelocity, dragRotation, fallback = session.value.lastDirection)
        startSpin(fullSpins = flick.fullSpins, direction = flick.direction, releaseVelocity = angularVelocity)
    }

    /**
     * 指で動かしている盤面の針が、時刻 `timeSeconds` に境目を越えた。間引いたうえで、スピン中の刻みより強い触覚と回転音を鳴らす。
     * 演出中は指で動かせないので何もしない。
     */
    fun onWheelBoundaryCrossed(timeSeconds: Double) {
        if (session.value.spinning) return

        val feedback = dragFeedbackThrottle.cross(timeSeconds)
        if (!feedback.click && !feedback.haptic) return

        viewModelScope.launch {
            val settings = userSettingsRepository.settings.first()
            if (feedback.haptic && settings.hapticsEnabled) hapticPlayer.play(HapticEffect.MEDIUM)
            if (feedback.click && settings.soundEnabled) spinSoundPlayer.playClick()
        }
    }

    /** 盤面の回転が止まった。保険のタイマーと重なっても、結果を出すのは 1 回だけ。 */
    fun onSpinFinished() {
        fallbackJob?.cancel()
        fallbackJob = null
        tickJob?.cancel()
        tickJob = null
        val finishing = session.value
        if (!finishing.spinning) return

        session.update { state -> state.copy(spinning = false, outcome = state.pendingOutcome, pendingOutcome = null) }
        if (finishing.pendingOutcome == null) return

        viewModelScope.launch {
            if (userSettingsRepository.settings.first().hapticsEnabled) hapticPlayer.play(HapticEffect.SUCCESS)
        }
    }

    /** 入力欄の行（正規化済み）をまとめて項目に足す。上限に収まらない分は切り捨てる。足せたら前の結果を消す。 */
    fun onAddItems(labels: List<String>) {
        viewModelScope.launch {
            val addedCount = addItems(ListKind.ROULETTE, labels)
            if (addedCount == 0) return@launch

            session.update { state -> state.copy(outcome = null) }
        }
    }

    /** 項目を 1 件削除する。指定していた項目なら指定も外す。「元に戻す」のために削除した項目を覚えておく。 */
    fun onRemoveItem(id: UUID) {
        viewModelScope.launch {
            val removed = removeItem(ListKind.ROULETTE, id) ?: return@launch

            lastRemoved = removed
            session.update { state ->
                val targetId =
                    if (state.targetId == id) {
                        null
                    } else {
                        state.targetId
                    }
                state.copy(targetId = targetId, outcome = null)
            }
        }
    }

    /** 直前に削除した項目を元の位置に戻す。指定は復元しない。同じ項目が残っているか上限に達していれば戻さない。 */
    fun onUndoRemove(id: UUID) {
        val removed = lastRemoved?.takeIf { candidate -> candidate.item.id == id } ?: return

        lastRemoved = null
        viewModelScope.launch {
            val before = itemsRepository.items(ListKind.ROULETTE).first()
            restoreItem(ListKind.ROULETTE, removed)
            val after = itemsRepository.items(ListKind.ROULETTE).first()
            if (before == after) return@launch

            session.update { state -> state.copy(outcome = null) }
        }
    }

    /** 項目をすべて削除する。指定と結果も消える。 */
    fun onRemoveAllItems() {
        lastRemoved = null
        viewModelScope.launch {
            itemsRepository.save(ListKind.ROULETTE, emptyList())
            session.update { state -> state.copy(targetId = null, outcome = null) }
        }
    }

    /** いまの項目を名前を付けて保存する。名前が空、または上限に達しているときは保存しない。 */
    fun onSaveList(name: String) {
        viewModelScope.launch {
            val items = itemsRepository.items(ListKind.ROULETTE).first()
            savedListsRepository.save(name, items)
        }
    }

    /** 保存したリストで項目を置き換える。指定と結果は前のリストのものなので捨てる。 */
    fun onLoadList(id: UUID) {
        viewModelScope.launch {
            val savedList = savedListsRepository.lists.first().firstOrNull { list -> list.id == id } ?: return@launch

            lastRemoved = null
            itemsRepository.save(ListKind.ROULETTE, savedList.items)
            session.update { state -> state.copy(targetId = null, outcome = null) }
        }
    }

    /** 長押しで当たりの指定と解除を切り替える。本人の指にだけ伝わる軽い手応えを返す。 */
    fun onItemLongPress(id: UUID) {
        session.update { state ->
            val targetId =
                if (state.targetId == id) {
                    null
                } else {
                    id
                }
            state.copy(targetId = targetId, outcome = null)
        }
        viewModelScope.launch {
            if (userSettingsRepository.settings.first().hapticsEnabled) hapticPlayer.play(HapticEffect.LIGHT)
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

    /** 画面から離れた。鳴らしている途中の回転音を止める。 */
    fun onScreenHidden() {
        spinSoundPlayer.stop()
    }

    override fun onCleared() {
        fallbackJob?.cancel()
        tickJob?.cancel()
        spinSoundPlayer.stop()
    }

    /**
     * スピンを始める。回せないとき（演出中・項目が足りない）は何もしない。
     * `direction` を渡すとその向きに回し、以後のボタンのスピンもその向きを引き継ぐ（始められなかったフリックの向きは引き継がない）。
     */
    private fun startSpin(
        fullSpins: Int?,
        direction: SpinDirection?,
        releaseVelocity: Double?,
    ) {
        if (session.value.spinning) return

        viewModelScope.launch {
            val items = itemsRepository.items(ListKind.ROULETTE).first()
            val settings = userSettingsRepository.settings.first()
            val current = session.value
            // 項目を読む間に別の操作でスピンが始まっていたら重ねない
            if (current.spinning) return@launch

            val reducedMotion = reduceMotionMonitor.reducedMotion.value
            val spinDirection = direction ?: current.lastDirection
            val plan =
                decideSpin(
                    items = items,
                    targetId = current.targetId,
                    currentRotation = current.rotation,
                    direction = spinDirection,
                    reducedMotion = reducedMotion,
                    fullSpins = fullSpins,
                    releaseVelocity = releaseVelocity,
                ) ?: return@launch

            session.update { state ->
                state.copy(
                    spinning = true,
                    rotation = plan.targetRotation,
                    spinCount = state.spinCount + 1,
                    easing = plan.easing,
                    lastDirection = spinDirection,
                    pendingOutcome = plan.outcome,
                    outcome = null,
                    spunItems = items,
                )
            }
            if (settings.soundEnabled) spinSoundPlayer.play(plan.clickTimes)
            if (settings.hapticsEnabled) {
                hapticPlayer.play(HapticEffect.MEDIUM)
                tickJob?.cancel()
                tickJob = viewModelScope.launchTicks(plan.hapticTimes) { hapticPlayer.play(HapticEffect.SELECTION) }
            }
            // 「アニメーションを削除」のときは回さずに止まる。終了は保険のタイマーが担う
            val fallbackSeconds =
                if (reducedMotion) {
                    SpinConfig.REDUCED_MOTION_DURATION_SECONDS
                } else {
                    SpinConfig.FALLBACK_SECONDS
                }
            startFallbackTimer(fallbackSeconds)
        }
    }

    private fun startFallbackTimer(seconds: Double) {
        fallbackJob?.cancel()
        fallbackJob =
            viewModelScope.launch {
                delay((seconds * 1_000).toLong())
                onSpinFinished()
            }
    }

    private fun uiStateOf(
        items: List<Item>,
        savedLists: List<SavedList>,
        glowStyle: GlowStyle,
        current: Session,
        device: Environment,
    ): UiState {
        // 指定した項目が消えていれば（削除・読み込み・初期化）印も出さない
        val targetId = current.targetId?.takeIf { id -> items.any { item -> item.id == id } }
        val marks = targetId?.let { id -> mapOf(id to MarkViewData.TARGET) } ?: emptyMap()
        // 回したあとに項目が変わっていれば（設定から初期状態に戻したなど、この画面の外で変わったときも）結果を出さない
        val outcome = current.outcome?.takeIf { items == current.spunItems }
        // 同名の行は項目から毎回求めるので、追加・読み込み・元に戻すのどの経路で入っても反映される
        val duplicateIds = DuplicateLabels.ids(items.map { item -> item.id to item.label }, Item.MAX_LABEL_LENGTH)
        return UiState(
            items = items.map { item -> ItemUiModel(id = item.id, label = item.label, isDuplicate = item.id in duplicateIds) },
            slices = items.map { item -> WheelSliceUiModel(id = item.id, label = item.label) },
            marks = marks,
            savedLists = savedLists.map { list -> SavedListSummaryUiModel(id = list.id, name = list.name, itemCount = list.items.size) },
            limits = itemListLimits,
            spinning = current.spinning,
            canSpin = !current.spinning && items.size >= Item.MIN_COUNT,
            spin =
            SpinAnimationUiModel(
                spinCount = current.spinCount,
                targetRotation = current.rotation.toFloat(),
                easing = current.easing.toUiModel(),
                durationMillis = (SpinConfig.DURATION_SECONDS * 1_000).toInt(),
            ),
            outcome = outcome?.let { stopped -> SpinOutcomeUiModel(index = stopped.index, label = stopped.label) },
            glowStyle = glowStyle.toUiModel(),
            reducedMotion = device.reducedMotion,
            screenCaptured = device.screenCaptured,
        )
    }

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

private fun CubicBezierCurve.toUiModel(): SpinEasingUiModel = SpinEasingUiModel(x1 = x1.toFloat(), y1 = y1.toFloat(), x2 = x2.toFloat(), y2 = y2.toFloat())

private fun GlowStyle.toUiModel(): GlowStyleUiModel = when (this) {
    GlowStyle.WHITE -> GlowStyleUiModel.WHITE
    GlowStyle.SLICE -> GlowStyleUiModel.SLICE
    GlowStyle.RAINBOW -> GlowStyleUiModel.RAINBOW
}
