package com.basekeita.roulette.domain.spin

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.SpinDirection
import com.basekeita.roulette.domain.haptic.HapticSchedule
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlin.random.Random

/**
 * スピンの開始時に、結果・止まる累積角・曲線・回転音と触覚の時刻をまとめて決める。
 * 当たりの指定（`targetId`）は ViewModel から引数で受け取るだけで、保存しない。
 */
class DecideSpinUseCase
@Inject
constructor(
    private val random: Random,
) {
    /**
     * @param currentRotation いまの累積回転角。指で回した分を含む
     * @param direction 回す向き。ボタンでは直前のフリックの向きを渡す
     * @param fullSpins フリックの強さから決めた周回数。null なら乱数で決める
     * @param releaseVelocity 指を離した瞬間の角速度（度/秒）。渡すと盤面の初速をそれに合わせる。null なら基準の曲線
     * @return 回せないとき（項目が `Item.MIN_COUNT` 未満、指定した項目が無い）は null
     */
    operator fun invoke(
        items: List<Item>,
        targetId: UUID?,
        currentRotation: Double,
        direction: SpinDirection,
        reducedMotion: Boolean,
        fullSpins: Int? = null,
        releaseVelocity: Double? = null,
    ): SpinPlan? {
        if (items.size < Item.MIN_COUNT) return null

        val targetIndex = resolveTargetIndex(items, targetId) ?: return null
        val targetRotation =
            if (fullSpins != null) {
                RouletteMath.nextRotation(currentRotation, targetIndex, items.size, fullSpins, random, direction)
            } else {
                RouletteMath.nextRotation(currentRotation, targetIndex, items.size, random, direction)
            }
        val easing =
            if (releaseVelocity != null) {
                FlickSpin.easing(
                    angularVelocity = releaseVelocity,
                    distance = abs(targetRotation - currentRotation),
                    durationSeconds = SpinConfig.DURATION_SECONDS,
                )
            } else {
                SpinConfig.EASING
            }
        // 「アニメーションを削除」のときは盤面が回らないので鳴らさない
        val clickTimes =
            if (reducedMotion) {
                emptyList()
            } else {
                SpinTicks.boundaryCrossings(
                    from = currentRotation,
                    to = targetRotation,
                    count = items.size,
                    durationSeconds = SpinConfig.DURATION_SECONDS,
                    easing = easing,
                    minIntervalSeconds = SpinConfig.CLICK_MIN_INTERVAL_SECONDS,
                )
            }
        val hapticTimes =
            if (reducedMotion) {
                emptyList()
            } else {
                HapticSchedule.boundaryCrossings(
                    from = currentRotation,
                    to = targetRotation,
                    count = items.size,
                    durationSeconds = SpinConfig.DURATION_SECONDS,
                    easing = easing,
                    minIntervalSeconds = SpinConfig.HAPTIC_MIN_INTERVAL_SECONDS,
                )
            }
        return SpinPlan(
            outcome = SpinOutcome(targetIndex, items[targetIndex].label),
            targetRotation = targetRotation,
            easing = easing,
            clickTimes = clickTimes,
            hapticTimes = hapticTimes,
        )
    }

    /** 指定があればその項目の添字、無ければ乱数で選んだ添字。指定した項目が無いときは null。 */
    private fun resolveTargetIndex(
        items: List<Item>,
        targetId: UUID?,
    ): Int? {
        if (targetId == null) return random.nextInt(items.size)

        return items.indexOfFirst { it.id == targetId }.takeIf { it >= 0 }
    }
}
