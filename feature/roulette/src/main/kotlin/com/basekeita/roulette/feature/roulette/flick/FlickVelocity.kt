package com.basekeita.roulette.feature.roulette.flick

import androidx.compose.ui.geometry.Offset
import com.basekeita.roulette.ui.rule.WheelDrag

/** フリックの角速度を、指を離す直前のこの時間（秒）に回った角度から求める。 */
internal const val FLICK_SAMPLE_WINDOW_SECONDS = 0.15

/**
 * ドラッグ中の指の位置と時刻。
 *
 * @property position 盤面の中の位置（dp）
 */
internal data class FlickSample(
    val timeSeconds: Double,
    val position: Offset,
)

/**
 * 盤面のフリックの読み取り。指を離す直前の動きから角速度を出す（周回数への写し方は domain の `FlickSpin`）。
 */
internal object FlickVelocity {
    /**
     * 指を離す直前 `FLICK_SAMPLE_WINDOW_SECONDS` 秒に、盤面中心 `center` のまわりを回った角度から求めた角速度（度/秒）。
     * 画面上の時計回りが正。`samples` は時刻順で、最後の 1 点が指を離した位置。
     *
     * 離した瞬間の速度は最後の数サンプルで決まり、離すタイミングで大きく揺れるので使わない。
     * 隣り合う点の角度差は追従と同じ `WheelDrag.rotationDelta` で測るので、中心付近に掛かる区間は数えない
     * （中心付近の点を除いてから前後をつなぐと、中心を挟んだ左右の点が半周回ったことになる）。
     */
    fun angularVelocity(
        samples: List<FlickSample>,
        center: Offset,
    ): Double {
        // 窓は指を離した時刻から取り、窓の中の点だけを使う。窓をまたぐ区間や、中心付近で止めていた間より前の動きを
        // 足すと、最後に止めていても手前の動きで回ってしまう
        val releaseTime = samples.lastOrNull()?.timeSeconds ?: return 0.0
        val windowStart = releaseTime - FLICK_SAMPLE_WINDOW_SECONDS
        val recent = samples.filter { sample -> sample.timeSeconds >= windowStart }
        val first = recent.firstOrNull() ?: return 0.0
        val last = recent.last()
        if (last.timeSeconds <= first.timeSeconds) return 0.0

        val swept = recent.zipWithNext { from, to -> WheelDrag.rotationDelta(from.position, to.position, center) }.sum()
        return swept / (last.timeSeconds - first.timeSeconds)
    }
}

/** フリック中の位置の記録。指が動くたびに足す。 */
internal class FlickSampleBuffer {
    private val recorded = ArrayDeque<FlickSample>()

    val samples: List<FlickSample>
        get() = recorded.toList()

    fun append(sample: FlickSample) {
        // 前の点から間が空いたら、それより前の動きは今の勢いに関係しない（止めてから離した、前の操作の残り）
        val previous = recorded.lastOrNull()
        if (previous != null && sample.timeSeconds - previous.timeSeconds > FLICK_SAMPLE_WINDOW_SECONDS * 2) {
            recorded.clear()
        }
        recorded.addLast(sample)
        // 計算に使うのは直近の窓の中だけ。余裕を持たせて古いものを捨てる
        val keepFrom = sample.timeSeconds - FLICK_SAMPLE_WINDOW_SECONDS * 2
        while (recorded.first().timeSeconds < keepFrom) {
            recorded.removeFirst()
        }
    }

    fun reset() {
        recorded.clear()
    }
}
