package com.basekeita.roulette.domain.haptic

import com.basekeita.roulette.domain.order.RevealTiming
import com.basekeita.roulette.domain.spin.CubicBezierCurve
import com.basekeita.roulette.domain.spin.SpinTicks

/** 触覚を鳴らす時刻の計算。演出の途中経過は使わないので、開始時に時刻の列を決めておき、順に鳴らす。 */
object HapticSchedule {
    /** スピン中に針がスライスの境目を越える時刻。計算は回転音と同じ `SpinTicks.boundaryCrossings`。 */
    fun boundaryCrossings(
        from: Double,
        to: Double,
        count: Int,
        durationSeconds: Double,
        easing: CubicBezierCurve,
        minIntervalSeconds: Double,
    ): List<Double> = SpinTicks.boundaryCrossings(from, to, count, durationSeconds, easing, minIntervalSeconds)

    /** 順番の結果が 1 件ずつ現れる時刻（開始からの秒）。「アニメーションを削除」のときは一度に出るので空。 */
    fun revealTicks(
        count: Int,
        reducedMotion: Boolean,
    ): List<Double> {
        if (reducedMotion || count <= 0) return emptyList()

        return List(count) { index -> RevealTiming.delay(index, reducedMotion = false) }
    }
}
