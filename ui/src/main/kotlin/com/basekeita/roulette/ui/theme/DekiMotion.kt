package com.basekeita.roulette.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * アニメーション。iOS 版 `Theme` の値を Compose の指定に置き換えたもの。
 * スピンの回転は `domain` の曲線と時間で回すので、ここには置かない。
 *
 * iOS の `Animation.easeOut` は `cubic-bezier(0, 0, 0.58, 1)` で、Compose の `EaseOut` と同じ。
 * iOS の `spring(duration:bounce:)` は、減衰比 `1 - bounce`、硬さ `(2π / duration)²`（質量 1）のばねと同じ。
 */
object DekiMotion {
    /** 結果が現れる演出（0.36 秒）。両画面で使い回す。 */
    fun <T> reveal(delayMillis: Int = 0): TweenSpec<T> = tween(
        durationMillis = 360,
        delayMillis = delayMillis,
        easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f),
    )

    /** 停止の瞬間の押し出し・沈み込みの行き。 */
    fun <T> stopPulse(): TweenSpec<T> = tween(durationMillis = 140, easing = EaseOut)

    /** 押し出し・沈み込みの戻り。少し弾ませる。強調が解けてスライスが元の大きさに戻るときにも使う。 */
    fun <T> stopSettle(): SpringSpec<T> = springOf(durationSeconds = 0.4f, bounce = 0.35f)

    /** 他のスライスが暗くなる / 戻るときの変化。 */
    fun <T> stopDim(): TweenSpec<T> = tween(durationMillis = 250, easing = EaseOut)

    /** 色見本が印に切り替わるとき。 */
    fun <T> markChange(): TweenSpec<T> = tween(durationMillis = 150, easing = EaseOut)

    /** 開始ボタンを押している間の縮み。 */
    fun <T> pressScale(): TweenSpec<T> = tween(durationMillis = 120, easing = EaseOut)

    /** フッターの折りたたみの開閉。 */
    fun <T> disclosure(): TweenSpec<T> = tween(durationMillis = 200, easing = EaseOut)

    private fun <T> springOf(
        durationSeconds: Float,
        bounce: Float,
    ): SpringSpec<T> {
        val angularFrequency = 2 * PI.toFloat() / durationSeconds
        return spring(dampingRatio = 1f - bounce, stiffness = angularFrequency * angularFrequency)
    }
}

/**
 * 停止の強調の寸法。盤面に比例させる値は基準直径（`DekiLayout.wheelMaxWidthCompact`）での dp。
 *
 * 止まった瞬間に針の下のスライスだけを短く押し出して少し大きいまま残し、他のスライスを強く沈める。
 */
object DekiStopEmphasis {
    /** 縁取りの太さ（基準直径での dp）。 */
    const val OUTLINE_WIDTH = 3f

    /** 光彩の半径（基準直径での dp）。 */
    const val GLOW_RADIUS = 10f

    /** 止まった瞬間にスライスを押し出す倍率。戻りで `HOLD_SCALE` に収まる。 */
    const val PULSE_SCALE = 1.10f

    /** 結果が出ている間、止まったスライスを前に出しておく倍率。 */
    const val HOLD_SCALE = 1.07f

    /** 停止の瞬間に針が沈む量。 */
    val pointerBounceOffset = 4.dp
}
