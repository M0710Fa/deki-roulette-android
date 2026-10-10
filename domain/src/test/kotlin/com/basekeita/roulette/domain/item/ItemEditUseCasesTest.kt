package com.basekeita.roulette.domain.item

import com.basekeita.roulette.data.definition.model.Item
import com.basekeita.roulette.data.definition.model.ListKind
import com.basekeita.roulette.data.definition.model.RemovedItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemEditUseCasesTest {
    private val kind = ListKind.ROULETTE
    private val items = listOf("A", "B", "C", "D").map { Item(label = it) }

    private fun repositoryWith(items: List<Item>) = FakeItemsRepository(initial = mapOf(kind to items))

    // 追加

    @Test
    fun `項目は上限まで追加できる`() = runTest {
        val repository = repositoryWith(emptyList())
        val addItems = AddItemsUseCase(repository)
        repeat(30) { index -> addItems(kind, listOf("項目$index")) }
        assertEquals(Item.MAX_COUNT, repository.current(kind).size)
    }

    @Test
    fun `空のラベルは追加せず保存もしない`() = runTest {
        val repository = repositoryWith(emptyList())
        assertEquals(0, AddItemsUseCase(repository)(kind, listOf("   ")))
        assertTrue(repository.current(kind).isEmpty())
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `複数のラベルをまとめて正規化して追加できる`() = runTest {
        val repository = repositoryWith(emptyList())
        val added = AddItemsUseCase(repository)(kind, listOf("A", "  B  C ", "   ", "D"))
        assertEquals(3, added)
        assertEquals(listOf("A", "B C", "D"), repository.current(kind).map { it.label })
    }

    @Test
    fun `まとめて追加しても上限を超えた分は切り捨てる`() = runTest {
        val repository = repositoryWith(items.take(2))
        val addItems = AddItemsUseCase(repository)
        val added = addItems(kind, (0 until 30).map { "項目$it" })
        assertEquals(Item.MAX_COUNT - 2, added)
        assertEquals(Item.MAX_COUNT, repository.current(kind).size)
        assertEquals("項目${Item.MAX_COUNT - 3}", repository.current(kind).last().label)
        assertEquals(0, addItems(kind, listOf("E")))
    }

    @Test
    fun `画面ごとのリストは互いに影響しない`() = runTest {
        val repository = repositoryWith(items)
        AddItemsUseCase(repository)(ListKind.ORDER, listOf("X"))
        assertEquals(items, repository.current(ListKind.ROULETTE))
        assertEquals(listOf("X"), repository.current(ListKind.ORDER).map { it.label })
    }

    // 削除

    @Test
    fun `削除すると削除した項目と元の位置が返る`() = runTest {
        val repository = repositoryWith(items)
        val removeItem = RemoveItemUseCase(repository)
        assertEquals(RemovedItem(items[1], 1), removeItem(kind, items[1].id))
        assertEquals(listOf("A", "C", "D"), repository.current(kind).map { it.label })
        assertNull(removeItem(kind, items[1].id))
    }

    // 元に戻す

    @Test
    fun `元に戻すと同じIDで元の位置に入る`() = runTest {
        val repository = repositoryWith(items)
        val removed = RemoveItemUseCase(repository)(kind, items[1].id)!!
        RestoreItemUseCase(repository)(kind, removed)
        assertEquals(items, repository.current(kind))
    }

    @Test
    fun `元の位置が範囲外なら末尾に戻す`() = runTest {
        val repository = repositoryWith(items)
        val removeItem = RemoveItemUseCase(repository)
        val removed = removeItem(kind, items[3].id)!!
        removeItem(kind, items[2].id)
        RestoreItemUseCase(repository)(kind, removed)
        assertEquals(listOf("A", "B", "D"), repository.current(kind).map { it.label })
    }

    @Test
    fun `同じ項目が残っているか上限に達していれば戻さない`() = runTest {
        val repository = repositoryWith(items)
        RestoreItemUseCase(repository)(kind, RemovedItem(items[0], 0))
        assertEquals(items, repository.current(kind))

        val full = repositoryWith(List(Item.MAX_COUNT) { Item(label = "項目$it") })
        RestoreItemUseCase(full)(kind, RemovedItem(Item(label = "E"), 0))
        assertEquals(Item.MAX_COUNT, full.current(kind).size)
        assertEquals(0, full.saveCount)
    }
}
