package com.basekeita.roulette.domain.order

/** 順番決めの結果を 1 件ずつ出す演出の時間計算。Web 版 `useOrder` の `revealDelayMs` / `revealDurationMs` に対応する。 */
object RevealTiming {
    /** 結果を 1 件ずつ出すときの間隔（秒）。 */
    const val STEP_SECONDS = 0.16

    /** 結果 1 件が現れるアニメーションの長さ（秒）。演出の終了時刻の計算に使う。 */
    const val ANIMATION_SECONDS = 0.36

    /** 「アニメーションを削除」のときは 1 件ずつ出さないので、演出はこの時間で終わる。 */
    const val REDUCED_MOTION_DURATION_SECONDS = 0.2

    fun delay(
        index: Int,
        reducedMotion: Boolean,
    ): Double {
        if (reducedMotion) return 0.0

        return index * STEP_SECONDS
    }

    fun duration(
        count: Int,
        reducedMotion: Boolean,
    ): Double {
        if (reducedMotion) return REDUCED_MOTION_DURATION_SECONDS

        return delay(maxOf(count - 1, 0), reducedMotion = false) + ANIMATION_SECONDS
    }
}
