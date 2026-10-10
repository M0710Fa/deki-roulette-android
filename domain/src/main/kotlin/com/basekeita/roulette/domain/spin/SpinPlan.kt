package com.basekeita.roulette.domain.spin

/**
 * 開始時に決めたスピンの全体。回転のアニメーション、回転音、触覚はすべてここから組み立てる。
 *
 * @property targetRotation 止まる累積回転角（度）
 * @property easing 回転の曲線。ボタンでは基準の曲線、フリックでは離した瞬間の速さに合わせた曲線
 * @property clickTimes 回転音を鳴らす時刻（開始からの秒）。「アニメーションを削除」のときは空
 * @property hapticTimes 触覚を鳴らす時刻（開始からの秒）。「アニメーションを削除」のときは空
 */
data class SpinPlan(
    val outcome: SpinOutcome,
    val targetRotation: Double,
    val easing: CubicBezierCurve,
    val clickTimes: List<Double>,
    val hapticTimes: List<Double>,
)
