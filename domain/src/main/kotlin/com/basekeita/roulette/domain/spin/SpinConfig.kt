package com.basekeita.roulette.domain.spin

/** スピンの数値。iOS の `Core/Config.swift` の値。時間の単位は秒。 */
object SpinConfig {
    const val DURATION_SECONDS = 4.5

    /** アニメーションの完了が届かない環境（バックグラウンド等）向けの保険。 */
    const val FALLBACK_SECONDS = DURATION_SECONDS + 0.6

    /** 「アニメーションを削除」のときは回さず、この時間で完了扱いにする。 */
    const val REDUCED_MOTION_DURATION_SECONDS = 0.32

    /** スピンのイージング（Web 版 `SPIN_EASING`）。回転のアニメーションと、触覚と回転音の時刻の逆算で使う。 */
    val EASING = CubicBezierCurve(0.15, 0.85, 0.3, 1.0)

    /** スピンの周回数の範囲。フリックの強さもこの範囲の中に写す。 */
    val FULL_SPIN_RANGE = 4..8

    /** 盤面のフリックをスピンとみなす角速度（度/秒）の下限。 */
    const val FLICK_MIN_ANGULAR_VELOCITY = 240.0

    /** 周回数が上限に達する角速度（度/秒）。これ以上はすべて最大の周回数になる。 */
    const val FLICK_MAX_ANGULAR_VELOCITY = 1800.0

    /** スピン中に境目を越える触覚を鳴らす最短間隔。序盤は境目を越える間隔がこれより短いので間引く。 */
    const val HAPTIC_MIN_INTERVAL_SECONDS = 0.06

    /** スピン中に回転音を鳴らす最短間隔。 */
    const val CLICK_MIN_INTERVAL_SECONDS = 0.032
}
