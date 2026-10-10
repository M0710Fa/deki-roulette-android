package com.basekeita.roulette.domain.spin

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.SpinDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class RouletteMathTest {
    @Test
    fun `指定したスライスが針の下で止まる`() {
        val random = Random(42)
        var current = 0.0
        repeat(2000) {
            val count = random.nextInt(2, Item.MAX_COUNT + 1)
            val target = random.nextInt(count)
            val next = RouletteMath.nextRotation(current, target, count, random)
            assertEquals(target, RouletteMath.indexUnderPointer(next, count))
            assertTrue(next > current)
            current = next
        }
    }

    @Test
    fun `最低でも4周と10度は回る`() {
        val random = Random(7)
        repeat(500) {
            val next = RouletteMath.nextRotation(123.4, 0, 4, random)
            assertTrue(next - 123.4 >= 4 * 360 + 10)
            assertTrue(next - 123.4 < 9 * 360 + 360)
        }
    }

    @Test
    fun `周回数を与えてもその周回数だけ回って指定したスライスで止まる`() {
        val random = Random(11)
        var current = 0.0
        repeat(2000) {
            val count = random.nextInt(2, Item.MAX_COUNT + 1)
            val target = random.nextInt(count)
            val spins = random.nextInt(0, 13)
            val next = RouletteMath.nextRotation(current, target, count, spins, random)
            assertEquals(target, RouletteMath.indexUnderPointer(next, count))
            // 指定の周回数に、停止位置までの差分（10 度以上 370 度未満）を足した分だけ進む
            assertTrue(next - current >= spins * 360.0 + 10)
            assertTrue(next - current < spins * 360.0 + 370)
            current = next
        }
    }

    @Test
    fun `反時計回りでも指定したスライスが針の下で止まり累積角が減る`() {
        val random = Random(42)
        var current = 0.0
        repeat(2000) {
            val count = random.nextInt(2, Item.MAX_COUNT + 1)
            val target = random.nextInt(count)
            val next = RouletteMath.nextRotation(current, target, count, random, SpinDirection.COUNTERCLOCKWISE)
            assertEquals(target, RouletteMath.indexUnderPointer(next, count))
            assertTrue(next < current)
            current = next
        }
        // 累積角が大きく負に振れても positiveMod で扱える
        assertTrue(current < -2000 * 360.0)
    }

    @Test
    fun `反時計回りでも周回数を与えるとその周回数だけ回る`() {
        val random = Random(11)
        var current = 0.0
        repeat(2000) {
            val count = random.nextInt(2, Item.MAX_COUNT + 1)
            val target = random.nextInt(count)
            val spins = random.nextInt(0, 13)
            val next = RouletteMath.nextRotation(current, target, count, spins, random, SpinDirection.COUNTERCLOCKWISE)
            assertEquals(target, RouletteMath.indexUnderPointer(next, count))
            assertTrue(current - next >= spins * 360.0 + 10)
            assertTrue(current - next < spins * 360.0 + 370)
            current = next
        }
    }

    @Test
    fun `反時計回りで止まる位置が近すぎるときは1周足す`() {
        // 回転 -5 のとき針の下の盤面角は 5。先頭のスライス [0, 90) のうち盤面角 5〜15 度に止まるときは、
        // 反時計回りの差分が 10 度未満になるので 1 周足す
        var addedLap = false
        for (seed in 0 until 200) {
            val next = RouletteMath.nextRotation(-5.0, 0, 4, 0, Random(seed), SpinDirection.COUNTERCLOCKWISE)
            assertEquals(0, RouletteMath.indexUnderPointer(next, 4))
            assertTrue(-5 - next >= 10)
            assertTrue(-5 - next < 370)
            if (-5 - next >= 360) addedLap = true
        }
        assertTrue(addedLap)
    }

    @Test
    fun `向きを省略すると時計回りと同じ結果になる`() {
        val implicitRandom = Random(5)
        val explicitRandom = Random(5)
        repeat(200) {
            val implicit = RouletteMath.nextRotation(37.5, 2, 5, implicitRandom)
            val explicit = RouletteMath.nextRotation(37.5, 2, 5, explicitRandom, SpinDirection.CLOCKWISE)
            assertEquals(explicit, implicit, 0.0)
        }
    }

    @Test
    fun `同じ乱数なら向きを変えても止まる盤面角は同じ`() {
        val random = Random(9)
        repeat(500) {
            val count = random.nextInt(2, Item.MAX_COUNT + 1)
            val target = random.nextInt(count)
            val current = random.nextDouble(-5000.0, 5000.0)
            val seed = random.nextLong()
            val forward = RouletteMath.nextRotation(current, target, count, 3, Random(seed))
            val backward = RouletteMath.nextRotation(current, target, count, 3, Random(seed), SpinDirection.COUNTERCLOCKWISE)
            val difference = abs(RouletteMath.positiveMod(forward) - RouletteMath.positiveMod(backward))
            assertTrue(difference < 1e-6 || abs(difference - 360) < 1e-6)
        }
    }

    @Test
    fun `針の下の添字は回転0で先頭のスライス`() {
        assertEquals(0, RouletteMath.indexUnderPointer(0.0, 4))
        // 盤面を時計回りに 90 度回すと、針の下には最後のスライスが来る
        assertEquals(3, RouletteMath.indexUnderPointer(90.0, 4))
        assertEquals(1, RouletteMath.indexUnderPointer(-90.0, 4))
        assertEquals(0, RouletteMath.indexUnderPointer(360.0 * 5, 4))
    }
}
