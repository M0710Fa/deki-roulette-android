package com.basekeita.roulette.domain.order

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.domain.haptic.HapticSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShuffleOrderUseCaseTest {
    private val items = listOf("A", "B", "C", "D").map { Item(label = it) }

    @Test
    fun `項目が2つ未満なら並べ替えない`() {
        val shuffleOrder = ShuffleOrderUseCase(Random(0))
        assertNull(shuffleOrder(items.take(1), null, null, reducedMotion = false))
        assertNull(shuffleOrder(emptyList(), null, null, reducedMotion = false))
    }

    @Test
    fun `並べ替えは先頭と末尾の指定を反映する`() {
        for (seed in 0 until 50) {
            val plan = ShuffleOrderUseCase(Random(seed))(items, items[1].id, items[2].id, reducedMotion = false)!!
            assertEquals(items[1], plan.ordered.first())
            assertEquals(items[2], plan.ordered.last())
            assertEquals(items.toSet(), plan.ordered.toSet())
        }
    }

    @Test
    fun `演出の時間と触覚の時刻は行が現れる時刻に合わせる`() {
        val plan = ShuffleOrderUseCase(Random(0))(items, null, null, reducedMotion = false)!!
        assertEquals(RevealTiming.duration(items.size, reducedMotion = false), plan.revealDurationSeconds, 0.0)
        assertEquals(HapticSchedule.revealTicks(items.size, reducedMotion = false), plan.hapticTimes)
        assertEquals(items.size, plan.hapticTimes.size)
    }

    @Test
    fun `アニメーションを削除しているときは一度に出して1件ごとに刻まない`() {
        val plan = ShuffleOrderUseCase(Random(0))(items, null, null, reducedMotion = true)!!
        assertEquals(RevealTiming.REDUCED_MOTION_DURATION_SECONDS, plan.revealDurationSeconds, 0.0)
        assertTrue(plan.hapticTimes.isEmpty())
    }
}
