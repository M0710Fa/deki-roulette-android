package com.basekeita.roulette.domain.spin

import kotlin.math.floor

/**
 * スピン中に針がスライスの境目を越える時刻の計算。回転はアニメーションに任せていて途中の角度は使わないので、
 * 開始時にこの列を求めて回転音と触覚を鳴らす位置を決める。
 */
object SpinTicks {
    /**
     * 回転角が `from` から `to` へ `durationSeconds` 秒で `easing` に沿って進むとき、針の下のスライス
     * （`RouletteMath.indexUnderPointer`）が切り替わる時刻（開始からの秒）を昇順で返す。
     * 直前に残した時刻から `minIntervalSeconds` 未満しか離れていないものは間引く。序盤は速くて境目が密なので
     * 間引かれ、終盤は減速に合わせて間隔が開く。
     * `to < from`（反時計回り）でも同じ規則で求める。`to == from` なら空。
     */
    fun boundaryCrossings(
        from: Double,
        to: Double,
        count: Int,
        durationSeconds: Double,
        easing: CubicBezierCurve,
        minIntervalSeconds: Double,
    ): List<Double> {
        if (count <= 0 || to == from || durationSeconds <= 0) return emptyList()

        if (to < from) {
            // 反時計回りは向きを反転して同じ式で数える。境目 k * sliceAngle は符号を反転しても境目のまま
            return boundaryCrossings(-from, -to, count, durationSeconds, easing, minIntervalSeconds)
        }
        val sliceAngle = 360.0 / count
        // 針の下の添字は盤面角 (360 - R) mod 360 が sliceAngle の倍数をまたぐ瞬間に変わる。
        // 360 は sliceAngle の倍数なので、回転角 R 自身が k * sliceAngle に達する瞬間と同じ
        val firstBoundary = floor(from / sliceAngle).toInt() + 1
        val lastBoundary = floor(to / sliceAngle).toInt()
        if (lastBoundary < firstBoundary) return emptyList()

        val times = mutableListOf<Double>()
        var lastKept = Double.NEGATIVE_INFINITY
        for (boundary in firstBoundary..lastBoundary) {
            val progress = (boundary * sliceAngle - from) / (to - from)
            val time = easing.timeAtProgress(progress) * durationSeconds
            if (time - lastKept < minIntervalSeconds) continue

            times.add(time)
            lastKept = time
        }
        return times
    }
}
