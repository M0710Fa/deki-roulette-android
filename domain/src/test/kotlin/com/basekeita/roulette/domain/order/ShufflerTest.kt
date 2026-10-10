package com.basekeita.roulette.domain.order

import com.basekeita.roulette.data.definition.model.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

class ShufflerTest {
    private val items = listOf("A", "B", "C", "D", "E", "F").map { Item(label = it) }

    @Test
    fun `先頭と末尾の指定が反映される`() {
        for (seed in 0 until 200) {
            val result = Shuffler.arrange(items, items[2].id, items[4].id, Random(seed))
            assertEquals(items[2], result.first())
            assertEquals(items[4], result.last())
            assertEquals(items.toSet(), result.toSet())
            assertEquals(items.size, result.size)
        }
    }

    @Test
    fun `先頭だけの指定でも末尾は自由に動く`() {
        val seenLast = mutableSetOf<UUID>()
        for (seed in 0 until 200) {
            val result = Shuffler.arrange(items, items[0].id, null, Random(seed))
            assertEquals(items[0], result.first())
            seenLast.add(result.last().id)
        }
        assertTrue(seenLast.size > 1)
    }

    @Test
    fun `指定なしならすべての位置に散る`() {
        val seenFirst = mutableSetOf<UUID>()
        for (seed in 0 until 200) {
            seenFirst.add(Shuffler.arrange(items, null, null, Random(seed)).first().id)
        }
        assertEquals(items.size, seenFirst.size)
    }

    @Test
    fun `要素が1つでも落ちない`() {
        val single = listOf(items[0])
        assertEquals(single, Shuffler.arrange(single, single[0].id, null, Random(0)))
        assertTrue(Shuffler.arrange(emptyList(), null, null, Random(0)).isEmpty())
    }
}
