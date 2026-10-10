package com.basekeita.roulette.domain.spin

import com.basekeita.roulette.data.definition.model.SpinDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** iOS の `FlickSpinTests` のうち、角速度からスピンへの写し方の部分。指の動きから角速度を求める部分は feature/roulette で移す。 */
class FlickSpinTest {
    private val minVelocity = SpinConfig.FLICK_MIN_ANGULAR_VELOCITY
    private val maxVelocity = SpinConfig.FLICK_MAX_ANGULAR_VELOCITY
    private val spinRange = SpinConfig.FULL_SPIN_RANGE

    private fun speeds(
        from: Double,
        through: Double,
        step: Double,
    ): List<Double> = generateSequence(from) { it + step }.takeWhile { it <= through }.toList()

    // 周回数

    @Test
    fun `閾値未満では回さない`() {
        assertNull(FlickSpin.fullSpins(0.0))
        assertNull(FlickSpin.fullSpins(minVelocity - 1))
        assertNull(FlickSpin.fullSpins(-(minVelocity - 1)))
    }

    @Test
    fun `閾値ちょうどで最小の周回数になる`() {
        assertEquals(spinRange.first, FlickSpin.fullSpins(minVelocity))
    }

    @Test
    fun `上限以上は最大の周回数で頭打ちになる`() {
        assertEquals(spinRange.last, FlickSpin.fullSpins(maxVelocity))
        assertEquals(spinRange.last, FlickSpin.fullSpins(maxVelocity * 10))
    }

    @Test
    fun `反時計回りのフリックでも速さだけを見る`() {
        for (speed in speeds(minVelocity, maxVelocity, 100.0)) {
            assertEquals(FlickSpin.fullSpins(speed), FlickSpin.fullSpins(-speed))
        }
    }

    @Test
    fun `速いほど周回数は減らない`() {
        var previous = spinRange.first
        for (speed in speeds(minVelocity, maxVelocity + 500, 10.0)) {
            val spins = FlickSpin.fullSpins(speed)
            assertNotNull(spins)
            assertTrue(spins!! in spinRange)
            assertTrue(spins >= previous)
            previous = spins
        }
    }

    // 初速

    /** 曲線 `easing` で `distance` 度を `duration` 秒かけて回すときの、回り始めの角速度（度/秒）。 */
    private fun initialSpeed(
        easing: CubicBezierCurve,
        distance: Double,
        duration: Double,
    ): Double {
        val deltaTime = 1e-5
        return easing.progressAtTime(deltaTime) * distance / (deltaTime * duration)
    }

    @Test
    fun `盤面は指を離した瞬間の速さで回り出す`() {
        for (velocity in listOf(minVelocity, 600.0, 1200.0, -900.0)) {
            val distance = 4.0 * 360 + 135
            val easing = FlickSpin.easing(velocity, distance, SpinConfig.DURATION_SECONDS)
            val speed = initialSpeed(easing, distance, SpinConfig.DURATION_SECONDS)
            assertTrue(abs(speed - abs(velocity)) / abs(velocity) < 0.01)
        }
    }

    @Test
    fun `初速は向きに依らず速さで決める`() {
        val distance = 6.0 * 360 + 40
        assertEquals(
            FlickSpin.easing(800.0, distance, SpinConfig.DURATION_SECONDS),
            FlickSpin.easing(-800.0, distance, SpinConfig.DURATION_SECONDS),
        )
    }

    @Test
    fun `初速を変えても止まり方と長さの前提は同じ`() {
        val easing = FlickSpin.easing(1000.0, 2000.0, SpinConfig.DURATION_SECONDS)
        // 始点の傾き以外は基準の曲線のまま。終わりの減速の形と、時刻 1 で止まることは変わらない
        assertEquals(SpinConfig.EASING.x1, easing.x1, 0.0)
        assertEquals(SpinConfig.EASING.x2, easing.x2, 0.0)
        assertEquals(SpinConfig.EASING.y2, easing.y2, 0.0)
        assertEquals(1.0, easing.progressAtTime(1.0), 0.0)
    }

    @Test
    fun `速すぎるフリックは曲線が保てる初速で頭打ちになる`() {
        val easing = FlickSpin.easing(100_000.0, 1500.0, SpinConfig.DURATION_SECONDS)
        assertEquals(1.0, easing.y1, 0.0)
    }

    @Test
    fun `初速を変えた曲線も単調に進む`() {
        for (velocity in speeds(minVelocity, 6000.0, 240.0)) {
            val easing = FlickSpin.easing(velocity, 4 * 360.0 + 10, SpinConfig.DURATION_SECONDS)
            var previous = 0.0
            for (step in 1..200) {
                val progress = easing.progressAtTime(step / 200.0)
                assertTrue(progress >= previous)
                previous = progress
            }
            assertEquals(1.0, previous, 1e-9)
        }
    }

    // 指を離したとき

    @Test
    fun `閾値以上ならフリックと同じスピン`() {
        for (velocity in listOf(minVelocity, 900.0, -1500.0, maxVelocity * 2)) {
            val expected = FlickSpin.spin(velocity)
            assertNotNull(expected)
            assertEquals(expected, FlickSpin.releaseSpin(velocity, -velocity, SpinDirection.COUNTERCLOCKWISE))
        }
    }

    @Test
    fun `閾値未満でも最小の周回数で回す`() {
        for (velocity in listOf(0.0, 1.0, minVelocity - 1, -(minVelocity - 1))) {
            val spin = FlickSpin.releaseSpin(velocity, 30.0, SpinDirection.CLOCKWISE)
            assertEquals(spinRange.first, spin.fullSpins)
        }
    }

    @Test
    fun `閾値未満では動かした向きに回す`() {
        // 離す直前に少し戻していても、ドラッグで回した向きを優先する
        assertEquals(
            SpinDirection.CLOCKWISE,
            FlickSpin.releaseSpin(-50.0, 40.0, SpinDirection.COUNTERCLOCKWISE).direction,
        )
        assertEquals(
            SpinDirection.COUNTERCLOCKWISE,
            FlickSpin.releaseSpin(50.0, -40.0, SpinDirection.CLOCKWISE).direction,
        )
    }

    @Test
    fun `閾値未満で盤面を動かしていなければ角速度の向き`() {
        assertEquals(SpinDirection.COUNTERCLOCKWISE, FlickSpin.releaseSpin(-50.0, 0.0, SpinDirection.CLOCKWISE).direction)
        assertEquals(SpinDirection.CLOCKWISE, FlickSpin.releaseSpin(50.0, 0.0, SpinDirection.COUNTERCLOCKWISE).direction)
    }

    @Test
    fun `動きが無ければ直前の向きで回す`() {
        assertEquals(SpinDirection.COUNTERCLOCKWISE, FlickSpin.releaseSpin(0.0, 0.0, SpinDirection.COUNTERCLOCKWISE).direction)
        assertEquals(SpinDirection.CLOCKWISE, FlickSpin.releaseSpin(0.0, 0.0, SpinDirection.CLOCKWISE).direction)
    }

    // 向き

    @Test
    fun `時計回りのフリックは時計回りに回す`() {
        assertEquals(FlickSpin.Spin(spinRange.first, SpinDirection.CLOCKWISE), FlickSpin.spin(minVelocity))
    }

    @Test
    fun `反時計回りのフリックは反時計回りに回す`() {
        assertEquals(FlickSpin.Spin(spinRange.last, SpinDirection.COUNTERCLOCKWISE), FlickSpin.spin(-maxVelocity))
    }

    @Test
    fun `向きを返しても閾値と周回数は変わらない`() {
        assertNull(FlickSpin.spin(0.0))
        assertNull(FlickSpin.spin(minVelocity - 1))
        assertNull(FlickSpin.spin(-(minVelocity - 1)))
        for (speed in speeds(minVelocity, maxVelocity + 500, 10.0)) {
            assertEquals(FlickSpin.fullSpins(speed), FlickSpin.spin(speed)?.fullSpins)
            assertEquals(FlickSpin.fullSpins(-speed), FlickSpin.spin(-speed)?.fullSpins)
            assertEquals(SpinDirection.CLOCKWISE, FlickSpin.spin(speed)?.direction)
            assertEquals(SpinDirection.COUNTERCLOCKWISE, FlickSpin.spin(-speed)?.direction)
        }
    }
}
