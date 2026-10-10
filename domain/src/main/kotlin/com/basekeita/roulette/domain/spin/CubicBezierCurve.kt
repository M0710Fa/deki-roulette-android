package com.basekeita.roulette.domain.spin

/**
 * CSS の `cubic-bezier(x1, y1, x2, y2)` と同じ意味のイージング曲線。x が時刻の割合、y が進み具合で、端点は (0, 0) と (1, 1) に固定。
 * 回転のアニメーション（Compose の `CubicBezierEasing`）と同じ形なので、補間中の角度をこの曲線から逆算できる。
 * 制御点は x, y とも 0..1 に収まり、両成分が単調増加である前提（CSS と同じ制約）。
 */
data class CubicBezierCurve(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
) {
    /** 時刻の割合 `time`（0..1）における進み具合（0..1）。 */
    fun progressAtTime(time: Double): Double = yAt(solve(time.coerceIn(0.0, 1.0), ::xAt))

    /** 進み具合 `progress`（0..1）に初めて達する時刻の割合（0..1）。`progressAtTime` の逆関数。 */
    fun timeAtProgress(progress: Double): Double = xAt(solve(progress.coerceIn(0.0, 1.0), ::yAt))

    private fun xAt(parameter: Double): Double = component(parameter, x1, x2)

    private fun yAt(parameter: Double): Double = component(parameter, y1, y2)

    /** 端点 0 と 1 を持つ 3 次ベジェの 1 成分をパラメータ `parameter` で評価する。 */
    private fun component(
        parameter: Double,
        firstControl: Double,
        secondControl: Double,
    ): Double {
        val remaining = 1 - parameter
        return 3 * remaining * remaining * parameter * firstControl +
            3 * remaining * parameter * parameter * secondControl +
            parameter * parameter * parameter
    }

    /**
     * 単調増加な成分が `target` になるパラメータを二分法で求める。
     * 端点は丸めで 1 に届かないことがあるので、二分法に掛けずそのまま返す。
     */
    private fun solve(
        target: Double,
        component: (Double) -> Double,
    ): Double {
        if (target <= 0) return 0.0

        if (target >= 1) return 1.0

        var low = 0.0
        var high = 1.0
        repeat(48) {
            val middle = (low + high) / 2
            if (component(middle) < target) {
                low = middle
            } else {
                high = middle
            }
        }
        return (low + high) / 2
    }
}
