package com.basekeita.roulette.ui.rule

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot

/**
 * ドラッグ中に盤面を指に追従させる角度の計算。フリックの角速度と同じ角度の測り方を使う。
 * 位置の単位は dp（盤面中心のまわりの不感帯を dp で決めているため）。
 */
object WheelDrag {
    /** 盤面中心からこの半径（dp）の内側では角度が定まらないので、追従とフリックの計算に使わない。 */
    const val DEAD_ZONE_RADIUS = 24.0

    /**
     * 指が `from` から `to` へ動いたとき、盤面中心 `center` のまわりに盤面を回す角度（度、画面上の時計回りが正）。
     * 中心から `DEAD_ZONE_RADIUS` 未満のところは角度が定まらないので、区間がそこに掛かれば 0（盤面は動かさない）。
     * 両端が外にあっても、間で中心付近を横切る区間は 0 にする（端だけで見ると半回転して見える）。
     */
    fun rotationDelta(
        from: Offset,
        to: Offset,
        center: Offset,
    ): Double {
        if (distanceToSegment(center, from, to) < DEAD_ZONE_RADIUS) return 0.0

        return normalized(angle(to, center) - angle(from, center))
    }

    /**
     * 盤面の累積角が `from` から `to` へ動いたときに、針がスライスの境目を越えた数（向きに依らず 0 以上）。
     * 境目は累積角が `360 / count` の倍数に達するところ（`SpinTicks` と同じ）。1 件以下の盤面には境目が無いので 0。
     */
    fun boundaryCrossings(
        from: Double,
        to: Double,
        count: Int,
    ): Int {
        if (count <= 1) return 0

        val sliceAngle = 360.0 / count
        return abs(floor(to / sliceAngle).toInt() - floor(from / sliceAngle).toInt())
    }

    /**
     * ドラッグ中に境目を越えたときの回転音と触覚の間引き。指を速く動かすと境目が詰まるので、
     * スピン中と同じ最短間隔より詰まったものは鳴らさない。鳴らさなかった境目は間隔の起点にしない。
     */
    class FeedbackThrottle(
        private val clickMinIntervalSeconds: Double,
        private val hapticMinIntervalSeconds: Double,
    ) {
        private var lastClickSeconds = Double.NEGATIVE_INFINITY
        private var lastHapticSeconds = Double.NEGATIVE_INFINITY

        /** 時刻 `timeSeconds` に境目を越えた。回転音と触覚をそれぞれ鳴らすかを返す。 */
        fun cross(timeSeconds: Double): Feedback {
            val click = timeSeconds - lastClickSeconds >= clickMinIntervalSeconds
            val haptic = timeSeconds - lastHapticSeconds >= hapticMinIntervalSeconds
            if (click) lastClickSeconds = timeSeconds
            if (haptic) lastHapticSeconds = timeSeconds
            return Feedback(click = click, haptic = haptic)
        }
    }

    data class Feedback(
        val click: Boolean,
        val haptic: Boolean,
    )

    /** 中心から見た向き（度）。y 軸が下向きなので、値が増える向きが画面上の時計回りになる。 */
    private fun angle(
        point: Offset,
        center: Offset,
    ): Double = atan2((point.y - center.y).toDouble(), (point.x - center.x).toDouble()) * 180 / PI

    /** 隣り合う 2 点の角度差を -180〜180 度に収める。±180 度をまたいでも 1 周分跳ねないようにする。 */
    private fun normalized(delta: Double): Double {
        var value = delta % 360
        if (value > 180) value -= 360
        if (value <= -180) value += 360
        return value
    }

    private fun distance(
        from: Offset,
        to: Offset,
    ): Double = hypot((to.x - from.x).toDouble(), (to.y - from.y).toDouble())

    /** 点 `point` から `start`〜`end` の線分までの最短距離。 */
    private fun distanceToSegment(
        point: Offset,
        start: Offset,
        end: Offset,
    ): Double {
        val dx = (end.x - start.x).toDouble()
        val dy = (end.y - start.y).toDouble()
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared <= 0) return distance(point, start)

        val projection = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        val nearest = Offset((start.x + projection * dx).toFloat(), (start.y + projection * dy).toFloat())
        return distance(point, nearest)
    }
}
