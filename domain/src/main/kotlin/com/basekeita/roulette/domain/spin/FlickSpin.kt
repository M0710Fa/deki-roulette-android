package com.basekeita.roulette.domain.spin

import com.basekeita.roulette.data.definition.model.SpinDirection
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 盤面のフリックの強さをスピンに写す。止まる位置には関与しない（`RouletteMath.nextRotation` に周回数と向きとして渡すだけ）。
 * 指の動きから角速度（度/秒）を求めるのはジェスチャーの読み取りなので、ここでは扱わない（feature/roulette）。
 */
object FlickSpin {
    /** フリックで始めるスピンの周回数と向き。 */
    data class Spin(
        val fullSpins: Int,
        val direction: SpinDirection,
    )

    /**
     * 角速度（度/秒）を周回数と向きに写す。向きは角速度の符号で決め、正（画面上の時計回り）なら時計回り。
     * 閾値未満なら null（スピンを始めない）。周回数は `fullSpins` と同じ。
     */
    fun spin(angularVelocity: Double): Spin? {
        val fullSpins = fullSpins(angularVelocity) ?: return null
        val direction =
            if (angularVelocity > 0) {
                SpinDirection.CLOCKWISE
            } else {
                SpinDirection.COUNTERCLOCKWISE
            }
        return Spin(fullSpins, direction)
    }

    /**
     * 盤面から指を離したときに始めるスピン。閾値以上なら `spin` と同じ。
     * 閾値未満（ゆっくり動かして離した、止めてから離した）でも、最小の周回数で回す。
     * そのときの向きは、ドラッグで盤面を回した向き（`dragRotation` の符号）。動かしていなければ角速度の符号、それも 0 なら `fallback`。
     */
    fun releaseSpin(
        angularVelocity: Double,
        dragRotation: Double,
        fallback: SpinDirection,
    ): Spin {
        val flick = spin(angularVelocity)
        if (flick != null) return flick

        val sign =
            if (dragRotation != 0.0) {
                dragRotation
            } else {
                angularVelocity
            }
        val direction =
            if (sign > 0) {
                SpinDirection.CLOCKWISE
            } else if (sign < 0) {
                SpinDirection.COUNTERCLOCKWISE
            } else {
                fallback
            }
        return Spin(SpinConfig.FULL_SPIN_RANGE.first, direction)
    }

    /**
     * 角速度（度/秒）を周回数に写す。周回数は向きに依らず速さだけで決める。
     * 閾値未満なら null。閾値で最小、`FLICK_MAX_ANGULAR_VELOCITY` 以上で最大の周回数になる。
     */
    fun fullSpins(angularVelocity: Double): Int? {
        val speed = abs(angularVelocity)
        val low = SpinConfig.FLICK_MIN_ANGULAR_VELOCITY
        val high = SpinConfig.FLICK_MAX_ANGULAR_VELOCITY
        if (speed < low) return null

        val range = SpinConfig.FULL_SPIN_RANGE
        val ratio = minOf((speed - low) / (high - low), 1.0)
        return range.first + (ratio * (range.last - range.first)).roundToInt()
    }

    /**
     * フリックで始めるスピンの曲線。指を離した瞬間の角速度（度/秒）と盤面の初速がつながるよう、`base` の始点の傾きだけを変える。
     * 長さと終わりの形（`x2`, `y2`）は `base` のままなので、止まる位置・止まるまでの時間・減速の終わり方は変わらない。
     * `distance` はスピンで回る角度（度、向きに依らず正）。
     *
     * 曲線の始点の傾き `y1 / x1` が、平均の速さ `distance / durationSeconds` に対する初速の倍率になる。`x1` は変えずに `y1` を決め、
     * 0..1 に収める（曲線が単調なまま。速すぎるフリックは `y1 = 1` の初速で頭打ちになる）。
     */
    fun easing(
        angularVelocity: Double,
        distance: Double,
        durationSeconds: Double,
        base: CubicBezierCurve = SpinConfig.EASING,
    ): CubicBezierCurve {
        if (distance <= 0 || durationSeconds <= 0 || base.x1 <= 0) return base

        val slope = abs(angularVelocity) * durationSeconds / distance
        return base.copy(y1 = (slope * base.x1).coerceIn(0.0, 1.0))
    }
}
