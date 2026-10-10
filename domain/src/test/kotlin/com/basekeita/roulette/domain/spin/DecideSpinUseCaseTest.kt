package com.basekeita.roulette.domain.spin

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.SpinDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

class DecideSpinUseCaseTest {
    private val items = listOf("A", "B", "C", "D").map { Item(label = it) }

    private fun decide(
        random: Random = Random(1),
        items: List<Item> = this.items,
        targetId: UUID? = null,
        currentRotation: Double = 0.0,
        direction: SpinDirection = SpinDirection.CLOCKWISE,
        reducedMotion: Boolean = false,
        fullSpins: Int? = null,
        releaseVelocity: Double? = null,
    ): SpinPlan? = DecideSpinUseCase(random)(
        items = items,
        targetId = targetId,
        currentRotation = currentRotation,
        direction = direction,
        reducedMotion = reducedMotion,
        fullSpins = fullSpins,
        releaseVelocity = releaseVelocity,
    )

    @Test
    fun `項目が2つ未満なら回せない`() {
        assertNull(decide(items = items.take(1)))
        assertNull(decide(items = emptyList()))
    }

    @Test
    fun `指定した項目が無ければ回せない`() {
        assertNull(decide(targetId = UUID.randomUUID()))
    }

    @Test
    fun `指定があれば結果はその項目で針の下に止まる`() {
        for (seed in 0 until 50) {
            val plan = decide(random = Random(seed), targetId = items[3].id)
            assertNotNull(plan)
            assertEquals(SpinOutcome(3, "D"), plan!!.outcome)
            assertEquals(3, RouletteMath.indexUnderPointer(plan.targetRotation, items.size))
        }
    }

    @Test
    fun `指定がなければ結果は針の下の項目と一致する`() {
        for (seed in 0 until 50) {
            val plan = decide(random = Random(seed))!!
            val index = RouletteMath.indexUnderPointer(plan.targetRotation, items.size)
            assertEquals(SpinOutcome(index, items[index].label), plan.outcome)
        }
    }

    @Test
    fun `指定がなければ結果はどの項目にも散る`() {
        val seen = (0 until 200).map { seed -> decide(random = Random(seed))!!.outcome.index }.toSet()
        assertEquals(items.indices.toSet(), seen)
    }

    @Test
    fun `同じラベルが並んでいても結果の添字は指定した項目を指す`() {
        val sameLabels = List(4) { Item(label = "A") }
        val plan = decide(items = sameLabels, targetId = sameLabels[2].id)!!
        assertEquals(SpinOutcome(2, "A"), plan.outcome)
    }

    @Test
    fun `フリックの周回数を渡すとその周回数だけ回り結果は変わらない`() {
        for (spins in SpinConfig.FULL_SPIN_RANGE) {
            val plan = decide(targetId = items[1].id, fullSpins = spins)!!
            assertTrue(plan.targetRotation >= spins * 360.0 + 10)
            assertTrue(plan.targetRotation < spins * 360.0 + 370)
            assertEquals(SpinOutcome(1, "B"), plan.outcome)
        }
    }

    @Test
    fun `反時計回りでは累積角が減り結果は変わらない`() {
        for (spins in SpinConfig.FULL_SPIN_RANGE) {
            val plan = decide(targetId = items[1].id, fullSpins = spins, direction = SpinDirection.COUNTERCLOCKWISE)!!
            assertTrue(-plan.targetRotation >= spins * 360.0 + 10)
            assertTrue(-plan.targetRotation < spins * 360.0 + 370)
            assertEquals(SpinOutcome(1, "B"), plan.outcome)
            assertEquals(1, RouletteMath.indexUnderPointer(plan.targetRotation, items.size))
        }
    }

    @Test
    fun `指で回した角度から始めても結果は変わらず音の時刻もその角度から求める`() {
        val random = Random(3)
        repeat(100) {
            val from = random.nextDouble(-5000.0, 5000.0)
            val direction =
                if (from < 0) {
                    SpinDirection.COUNTERCLOCKWISE
                } else {
                    SpinDirection.CLOCKWISE
                }
            val plan = decide(random = random, targetId = items[2].id, currentRotation = from, direction = direction, fullSpins = 4)!!
            assertTrue(abs(plan.targetRotation - from) >= 4 * 360 + 10)
            assertEquals(SpinOutcome(2, "C"), plan.outcome)
            assertEquals(2, RouletteMath.indexUnderPointer(plan.targetRotation, items.size))
            val expectedClicks =
                SpinTicks.boundaryCrossings(
                    from,
                    plan.targetRotation,
                    items.size,
                    SpinConfig.DURATION_SECONDS,
                    SpinConfig.EASING,
                    SpinConfig.CLICK_MIN_INTERVAL_SECONDS,
                )
            assertEquals(expectedClicks, plan.clickTimes)
        }
    }

    @Test
    fun `フリックの初速は曲線にだけ効き結果は変わらない`() {
        val random = Random(5)
        repeat(50) {
            val velocity = random.nextDouble(-4000.0, 4000.0)
            val direction =
                if (velocity < 0) {
                    SpinDirection.COUNTERCLOCKWISE
                } else {
                    SpinDirection.CLOCKWISE
                }
            val plan =
                decide(
                    random = random,
                    targetId = items[1].id,
                    direction = direction,
                    fullSpins = 5,
                    releaseVelocity = velocity,
                )!!
            val expectedEasing = FlickSpin.easing(velocity, abs(plan.targetRotation), SpinConfig.DURATION_SECONDS)
            assertEquals(expectedEasing, plan.easing)
            // 回転音の時刻も同じ曲線から逆算する（見た目とずれない）
            val expectedClicks =
                SpinTicks.boundaryCrossings(
                    0.0,
                    plan.targetRotation,
                    items.size,
                    SpinConfig.DURATION_SECONDS,
                    expectedEasing,
                    SpinConfig.CLICK_MIN_INTERVAL_SECONDS,
                )
            assertEquals(expectedClicks, plan.clickTimes)
            assertEquals(SpinOutcome(1, "B"), plan.outcome)
        }
    }

    @Test
    fun `ボタンのスピンは基準の曲線で回す`() {
        assertEquals(SpinConfig.EASING, decide()!!.easing)
        assertNotEquals(SpinConfig.EASING, decide(fullSpins = 4, releaseVelocity = 1500.0)!!.easing)
    }

    @Test
    fun `回転音と触覚の時刻はスピンの間に収まり触覚は触覚の間隔で間引く`() {
        val plan = decide()!!
        assertTrue(plan.clickTimes.isNotEmpty())
        assertEquals(plan.clickTimes.sorted(), plan.clickTimes)
        assertTrue(plan.clickTimes.first() > 0)
        assertTrue(plan.clickTimes.last() <= SpinConfig.DURATION_SECONDS)
        // 4 項目なので境目は 90 度ごと。間引きがある分だけ必ず少なくなる
        assertTrue(plan.clickTimes.size <= (plan.targetRotation / 90).toInt())
        val expectedHaptics =
            SpinTicks.boundaryCrossings(
                0.0,
                plan.targetRotation,
                items.size,
                SpinConfig.DURATION_SECONDS,
                SpinConfig.EASING,
                SpinConfig.HAPTIC_MIN_INTERVAL_SECONDS,
            )
        assertEquals(expectedHaptics, plan.hapticTimes)
    }

    @Test
    fun `反時計回りでも回転音と触覚の時刻が出る`() {
        val plan = decide(fullSpins = 4, direction = SpinDirection.COUNTERCLOCKWISE)!!
        assertTrue(plan.clickTimes.isNotEmpty())
        assertTrue(plan.hapticTimes.isNotEmpty())
        assertEquals(plan.clickTimes.sorted(), plan.clickTimes)
        assertTrue(plan.clickTimes.last() <= SpinConfig.DURATION_SECONDS)
    }

    @Test
    fun `アニメーションを削除しているときは回転音も触覚も鳴らさない`() {
        val plan = decide(reducedMotion = true)!!
        assertTrue(plan.clickTimes.isEmpty())
        assertTrue(plan.hapticTimes.isEmpty())
    }
}
