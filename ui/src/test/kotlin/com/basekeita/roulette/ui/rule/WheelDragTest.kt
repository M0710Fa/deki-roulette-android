package com.basekeita.roulette.ui.rule

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class WheelDragTest {
    private val center = Offset(160f, 160f)

    /** 位置が Float なので、角度の誤差はこの範囲まで許す。 */
    private val tolerance = 1e-3

    /** 中心から `radius` の円周上で、3 時を 0 度・画面上の時計回りを正とする角度 `degrees` の点。 */
    private fun point(
        degrees: Double,
        radius: Double = 100.0,
    ): Offset {
        val radians = degrees * PI / 180
        return Offset((center.x + radius * cos(radians)).toFloat(), (center.y + radius * sin(radians)).toFloat())
    }

    private fun totalRotation(path: List<Offset>): Double = path.zipWithNext { from, to -> WheelDrag.rotationDelta(from, to, center) }.sum()

    @Test
    fun `時計回りに動かすと正の角度だけ回す`() {
        assertEquals(30.0, WheelDrag.rotationDelta(point(-90.0), point(-60.0), center), tolerance)
    }

    @Test
    fun `反時計回りに動かすと負の角度だけ回す`() {
        assertEquals(-45.0, WheelDrag.rotationDelta(point(10.0), point(-35.0), center), tolerance)
    }

    @Test
    fun `中心からの距離に依らず回った角度だけ回す`() {
        val near = WheelDrag.rotationDelta(point(0.0, radius = 40.0), point(20.0, radius = 40.0), center)
        val far = WheelDrag.rotationDelta(point(0.0, radius = 150.0), point(20.0, radius = 40.0), center)
        assertEquals(20.0, near, tolerance)
        assertEquals(20.0, far, tolerance)
    }

    @Test
    fun `九時をまたいでも一周分跳ねない`() {
        // atan2 が ±180 度で折り返す位置（9 時）を時計回り・反時計回りにまたぐ
        assertEquals(20.0, WheelDrag.rotationDelta(point(170.0), point(190.0), center), tolerance)
        assertEquals(-20.0, WheelDrag.rotationDelta(point(190.0), point(170.0), center), tolerance)
    }

    @Test
    fun `半径方向の動きでは回さない`() {
        assertEquals(0.0, WheelDrag.rotationDelta(point(45.0, radius = 40.0), point(45.0, radius = 150.0), center), tolerance)
    }

    @Test
    fun `中心付近の点を含む区間では回さない`() {
        val inside = WheelDrag.DEAD_ZONE_RADIUS - 1
        assertEquals(0.0, WheelDrag.rotationDelta(point(0.0, radius = inside), point(90.0), center), 0.0)
        assertEquals(0.0, WheelDrag.rotationDelta(point(0.0), point(90.0, radius = inside), center), 0.0)
        assertEquals(0.0, WheelDrag.rotationDelta(center, center, center), 0.0)
    }

    @Test
    fun `両端が外でも中心付近を横切る区間では回さない`() {
        // 中心の左右 30dp を 1 区間で結ぶ。端だけで見ると 180 度回ったことになる
        val outside = WheelDrag.DEAD_ZONE_RADIUS + 6
        assertEquals(0.0, WheelDrag.rotationDelta(point(180.0, radius = outside), point(0.0, radius = outside), center), 0.0)
    }

    @Test
    fun `中心付近を横切らない区間は端が近くても回す`() {
        // 弦が中心から不感帯の半径より離れていれば、端の点が内側に寄っていても角度どおりに回す
        assertEquals(20.0, WheelDrag.rotationDelta(point(-10.0, radius = 40.0), point(10.0, radius = 40.0), center), tolerance)
    }

    @Test
    fun `中心付近を通り抜けても抜けた先から追従する`() {
        // 9 時から中心を通って 3 時へ抜ける。中心付近の区間は数えないので、盤面は 180 度跳ねない
        val path = listOf(point(180.0), point(180.0, radius = 30.0), center, point(0.0, radius = 30.0), point(0.0), point(20.0))
        assertEquals(20.0, totalRotation(path), tolerance)
    }

    @Test
    fun `細かく動かした合計は回った角度に等しい`() {
        // 時計回りに 2 周と 30 度、1 度ずつ動かす。累積で 360 度を超えても失わない
        val path = (0..750).map { step -> point(-90.0 + step) }
        assertEquals(750.0, totalRotation(path), 0.1)
    }

    @Test
    fun `境目を越えた数を向きに依らず数える`() {
        // 4 項目なので境目は 90 度ごと
        assertEquals(1, WheelDrag.boundaryCrossings(80.0, 100.0, 4))
        assertEquals(1, WheelDrag.boundaryCrossings(100.0, 80.0, 4))
        assertEquals(0, WheelDrag.boundaryCrossings(10.0, 80.0, 4))
        assertEquals(3, WheelDrag.boundaryCrossings(10.0, 280.0, 4))
    }

    @Test
    fun `負の累積角でも境目を数える`() {
        assertEquals(1, WheelDrag.boundaryCrossings(5.0, -5.0, 4))
        assertEquals(1, WheelDrag.boundaryCrossings(-95.0, -85.0, 4))
        assertEquals(6, WheelDrag.boundaryCrossings(-1000.0, -1000.0 - 360, 6))
    }

    @Test
    fun `項目が1件以下なら境目は無い`() {
        assertEquals(0, WheelDrag.boundaryCrossings(0.0, 720.0, 1))
        assertEquals(0, WheelDrag.boundaryCrossings(0.0, 720.0, 0))
    }

    @Test
    fun `詰まった境目の音と触覚を間引く`() {
        val click = 0.032
        val haptic = 0.06
        val throttle = WheelDrag.FeedbackThrottle(clickMinIntervalSeconds = click, hapticMinIntervalSeconds = haptic)
        assertEquals(WheelDrag.Feedback(click = true, haptic = true), throttle.cross(10.0))
        // 回転音の間隔は空いたが、触覚の間隔はまだ空いていない
        val afterClick = 10 + click * 1.1
        assertEquals(WheelDrag.Feedback(click = true, haptic = false), throttle.cross(afterClick))
        assertEquals(WheelDrag.Feedback(click = false, haptic = false), throttle.cross(afterClick + click * 0.5))
        // 鳴らさなかった境目は間隔の起点にしない
        assertEquals(WheelDrag.Feedback(click = true, haptic = true), throttle.cross(10 + max(haptic, click * 1.1 + click) * 1.1))
    }
}
